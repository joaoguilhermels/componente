#!/usr/bin/env python3
"""Migration CLI — Guided workflow + scorecard verification for OneFinancial.

Usage:
    python3 scripts/migration_cli.py verify [--self-test] [--phase N] [--json] [--target PATH_OR_URL]
    python3 scripts/migration_cli.py status [--target PATH_OR_URL] [--json]
    python3 scripts/migration_cli.py init --service-name NAME --tier TIER [--target PATH_OR_URL] ...
    python3 scripts/migration_cli.py guide [--target PATH_OR_URL] [--phase N] [--skip-gate]
    python3 scripts/migration_cli.py record --service-name NAME [--poc-data PATH] [--dry-run]

--target accepts a local path or a git URL (https://, git@, ssh://).
When a URL is given, the repo is cloned to .migration/repos/<name>/ automatically.

Dependencies: PyYAML (stdlib-only otherwise).
"""

from __future__ import annotations

import argparse
import json
import os
import re
import subprocess
import sys
from dataclasses import dataclass, field
from datetime import datetime, timezone
from pathlib import Path
from typing import Optional

import yaml

# ─── Constants ──────────────────────────────────────────────────────────────

SCRIPT_DIR = Path(__file__).resolve().parent
REPO_ROOT = SCRIPT_DIR.parent

REFERENCE_MARKER = "customer-registry-starter"

# Forbidden imports in core — hexagonal boundary enforcement
FORBIDDEN_CORE_IMPORTS = [
    "jakarta.persistence",
    "org.springframework.data",
    "org.springframework.web",
    "org.springframework.stereotype",
    "org.springframework.boot",
    "org.springframework.context",
    "org.springframework.beans",
]

# Allowed exceptions — cross-cutting concerns that are permitted in core
ALLOWED_CORE_IMPORTS = [
    "org.springframework.transaction.annotation.Transactional",
    "org.springframework.modulith",
]

# Phase definitions
PHASES = {
    1: {"name": "Foundation", "weight": 0.25},
    2: {"name": "Core Domain", "weight": 0.35},
    3: {"name": "Adapters", "weight": 0.15},
    4: {"name": "Auto-configuration", "weight": 0.20},
    5: {"name": "Frontend", "weight": 0.05},
}

# Dimension-to-phase mapping with weights
DIMENSIONS = {
    "package_structure":         {"phase": 1, "weight": 0.10, "label": "Package structure (hexagonal layout)"},
    "modulith_marker":           {"phase": 1, "weight": 0.10, "label": "Modulith marker + test exists"},
    "ci_pipeline":               {"phase": 1, "weight": 0.05, "label": "CI pipeline with architecture gates"},
    "core_isolation":            {"phase": 2, "weight": 0.15, "label": "Core isolation (zero infra deps)"},
    "port_interfaces":           {"phase": 2, "weight": 0.10, "label": "Port interfaces for external access"},
    "domain_test_coverage":      {"phase": 2, "weight": 0.10, "label": "Domain unit test coverage"},
    "bridge_configs":            {"phase": 3, "weight": 0.05, "label": "Bridge configs on all adapters"},
    "adapter_integration_tests": {"phase": 3, "weight": 0.10, "label": "Adapter integration tests"},
    "conditional_on_missing_bean": {"phase": 4, "weight": 0.05, "label": "@ConditionalOnMissingBean coverage"},
    "feature_flags_default":     {"phase": 4, "weight": 0.05, "label": "Feature flags default to off"},
    "auto_config_meta_inf":      {"phase": 4, "weight": 0.05, "label": "Auto-config registered in META-INF"},
    "context_runner_tests":      {"phase": 4, "weight": 0.05, "label": "Context runner tests"},
    "angular_standalone_onpush": {"phase": 5, "weight": 0.05, "label": "Angular standalone + OnPush"},
}

# ANSI color codes
BOLD = "\033[1m"
DIM = "\033[2m"
GREEN = "\033[32m"
RED = "\033[31m"
YELLOW = "\033[33m"
CYAN = "\033[36m"
WHITE = "\033[97m"
RESET = "\033[0m"

# Prompt references for the guide subcommand
PHASE_PROMPTS = {
    0: ["Prompt 0.1: Workspace Setup", "Prompt 0.2: Understand Legacy Service"],
    1: ["Prompt 1.1: Scaffold Package Structure", "Prompt 1.2: Modulith Marker", "Prompt 1.3: CI Pipeline"],
    2: ["Prompt 2.1: Core Model + Ports", "Prompt 2.2: Domain Service", "Prompt 2.3: Core Tests"],
    3: ["Prompt 3.1: Persistence Adapter", "Prompt 3.2: REST Adapter", "Prompt 3.3: Events Adapter"],
    4: ["Prompt 4.1: Auto-Configuration", "Prompt 4.2: Feature Flags", "Prompt 4.3: Context Runner Tests"],
    5: ["Prompt 5.1: Angular Components", "Prompt 5.2: Frontend Tests"],
}


# ─── Dataclasses ────────────────────────────────────────────────────────────

@dataclass
class CheckResult:
    """Result of a single scorecard dimension check."""
    dimension: str
    passed: bool
    detail: str
    weight: float
    phase: int
    label: str


@dataclass
class PhaseResult:
    """Aggregated result for one phase."""
    phase: int
    name: str
    weight: float
    checks: list[CheckResult]

    @property
    def score(self) -> float:
        """Phase score as percentage (0-100)."""
        total_weight = sum(c.weight for c in self.checks)
        if total_weight == 0:
            return 0.0
        passed_weight = sum(c.weight for c in self.checks if c.passed)
        return (passed_weight / total_weight) * 100

    @property
    def all_passed(self) -> bool:
        return all(c.passed for c in self.checks)


@dataclass
class VerifyResult:
    """Full verification result across all phases."""
    phases: list[PhaseResult]
    repo_name: str
    is_self_test: bool
    timestamp: str = field(default_factory=lambda: datetime.now(timezone.utc).isoformat())

    @property
    def overall_score(self) -> float:
        total_weight = sum(c.weight for p in self.phases for c in p.checks)
        if total_weight == 0:
            return 0.0
        passed_weight = sum(c.weight for p in self.phases for c in p.checks if c.passed)
        return (passed_weight / total_weight) * 100

    @property
    def total_dimensions(self) -> int:
        return sum(len(p.checks) for p in self.phases)

    @property
    def passing_dimensions(self) -> int:
        return sum(1 for p in self.phases for c in p.checks if c.passed)

    @property
    def all_passed(self) -> bool:
        return all(p.all_passed for p in self.phases)


@dataclass
class MigrationState:
    """Persistent state for a service being migrated."""
    service_name: str
    base_package: str
    db_prefix: str
    property_prefix: str
    tier: str
    has_frontend: bool
    current_phase: int
    phase_times: dict
    errors_encountered: list


# ─── RepoDetector ───────────────────────────────────────────────────────────

class RepoDetector:
    """Detect repo type and compute source roots."""

    def __init__(self, target_path: Optional[Path] = None, state: Optional[MigrationState] = None):
        self.target = (target_path or REPO_ROOT).resolve()
        self.state = state
        self._is_reference = (self.target / REFERENCE_MARKER).is_dir()

    @property
    def is_reference(self) -> bool:
        return self._is_reference

    @property
    def repo_name(self) -> str:
        if self._is_reference:
            return "OneFinancial Reference"
        if self.state:
            return self.state.service_name
        return self.target.name

    @property
    def java_main(self) -> Path:
        if self._is_reference:
            return self.target / REFERENCE_MARKER / "src" / "main" / "java" / "com" / "onefinancial" / "customer"
        return self.target / "src" / "main" / "java"

    @property
    def java_test(self) -> Path:
        if self._is_reference:
            return self.target / REFERENCE_MARKER / "src" / "test" / "java" / "com" / "onefinancial" / "customer"
        return self.target / "src" / "test" / "java"

    @property
    def resources(self) -> Path:
        if self._is_reference:
            return self.target / REFERENCE_MARKER / "src" / "main" / "resources"
        return self.target / "src" / "main" / "resources"

    @property
    def frontend(self) -> Optional[Path]:
        if self._is_reference:
            fe = self.target / "frontend" / "projects" / "customer-registry-ui" / "src"
            return fe if fe.is_dir() else None
        fe = self.target / "frontend"
        return fe if fe.is_dir() else None

    @property
    def ci_paths(self) -> list[Path]:
        """Candidate CI pipeline file paths."""
        return [
            self.target / ".azure" / "pipelines",
            self.target / ".github" / "workflows",
        ]


# ─── ScorecardChecker ───────────────────────────────────────────────────────

class ScorecardChecker:
    """Runs the 13 fast scorecard checks against a detected repo."""

    def __init__(self, detector: RepoDetector):
        self.d = detector

    def run_all(self) -> VerifyResult:
        """Run all 13 checks, grouped by phase."""
        phases = []
        for phase_num in sorted(PHASES.keys()):
            phase_info = PHASES[phase_num]
            dims = {k: v for k, v in DIMENSIONS.items() if v["phase"] == phase_num}
            checks = []
            for dim_key, dim_info in dims.items():
                method = getattr(self, f"check_{dim_key}")
                result = method()
                checks.append(result)
            phases.append(PhaseResult(
                phase=phase_num,
                name=phase_info["name"],
                weight=phase_info["weight"],
                checks=checks,
            ))
        return VerifyResult(
            phases=phases,
            repo_name=self.d.repo_name,
            is_self_test=self.d.is_reference,
        )

    def run_phase(self, phase_num: int) -> VerifyResult:
        """Run checks for a single phase."""
        phase_info = PHASES[phase_num]
        dims = {k: v for k, v in DIMENSIONS.items() if v["phase"] == phase_num}
        checks = []
        for dim_key, dim_info in dims.items():
            method = getattr(self, f"check_{dim_key}")
            result = method()
            checks.append(result)
        return VerifyResult(
            phases=[PhaseResult(
                phase=phase_num,
                name=phase_info["name"],
                weight=phase_info["weight"],
                checks=checks,
            )],
            repo_name=self.d.repo_name,
            is_self_test=self.d.is_reference,
        )

    # ── Phase 1: Foundation ──

    def check_package_structure(self) -> CheckResult:
        """Verify hexagonal package layout: core/, persistence/, rest/ exist."""
        dim = DIMENSIONS["package_structure"]
        required = ["core", "persistence", "rest"]
        found = []
        missing = []
        for pkg in required:
            if self._find_package_dir(pkg):
                found.append(pkg)
            else:
                missing.append(pkg)

        # Also check for optional but expected packages
        optional = ["events", "autoconfigure"]
        for pkg in optional:
            if self._find_package_dir(pkg):
                found.append(pkg)

        passed = len(missing) == 0
        if passed:
            detail = ", ".join(f"{p}/" for p in found)
        else:
            detail = f"Missing: {', '.join(missing)}"
        return CheckResult(
            dimension="package_structure", passed=passed, detail=detail,
            weight=dim["weight"], phase=dim["phase"], label=dim["label"],
        )

    def check_modulith_marker(self) -> CheckResult:
        """Verify @Modulithic marker class and ModulithStructureTest exist."""
        dim = DIMENSIONS["modulith_marker"]
        marker_found = False
        marker_file = ""
        test_found = False

        for java_file in self.d.java_main.rglob("*.java"):
            try:
                content = java_file.read_text(encoding="utf-8")
            except (OSError, UnicodeDecodeError):
                continue
            if "@Modulithic" in content:
                marker_found = True
                marker_file = java_file.name
                break

        for test_file in self.d.java_test.rglob("*ModulithStructureTest*.java"):
            test_found = True
            break

        passed = marker_found and test_found
        if passed:
            detail = f"{marker_file} with @Modulithic"
        elif marker_found:
            detail = f"{marker_file} found, but ModulithStructureTest missing"
        elif test_found:
            detail = "ModulithStructureTest found, but no @Modulithic marker"
        else:
            detail = "Neither @Modulithic marker nor ModulithStructureTest found"
        return CheckResult(
            dimension="modulith_marker", passed=passed, detail=detail,
            weight=dim["weight"], phase=dim["phase"], label=dim["label"],
        )

    def check_ci_pipeline(self) -> CheckResult:
        """Verify CI pipeline files exist."""
        dim = DIMENSIONS["ci_pipeline"]
        found_files = []
        for ci_dir in self.d.ci_paths:
            if ci_dir.is_dir():
                for yml in ci_dir.glob("*.yml"):
                    found_files.append(str(yml.relative_to(self.d.target)))

        passed = len(found_files) > 0
        detail = ", ".join(found_files) if passed else "No CI pipeline files found"
        return CheckResult(
            dimension="ci_pipeline", passed=passed, detail=detail,
            weight=dim["weight"], phase=dim["phase"], label=dim["label"],
        )

    # ── Phase 2: Core Domain ──

    def check_core_isolation(self) -> CheckResult:
        """Verify core package has zero forbidden infrastructure imports."""
        dim = DIMENSIONS["core_isolation"]
        core_dir = self._find_package_dir("core")
        if not core_dir:
            return CheckResult(
                dimension="core_isolation", passed=False, detail="core/ directory not found",
                weight=dim["weight"], phase=dim["phase"], label=dim["label"],
            )

        violations = []
        for java_file in core_dir.rglob("*.java"):
            try:
                content = java_file.read_text(encoding="utf-8")
            except (OSError, UnicodeDecodeError):
                continue
            for line_num, line in enumerate(content.splitlines(), 1):
                stripped = line.strip()
                if not stripped.startswith("import "):
                    continue
                import_statement = stripped[7:].rstrip(";").strip()
                if self._is_forbidden_import(import_statement):
                    rel_path = java_file.relative_to(self.d.java_main)
                    violations.append(f"{rel_path}:{line_num} -> {import_statement}")

        passed = len(violations) == 0
        if passed:
            detail = "No forbidden imports in core/"
        else:
            detail = f"{len(violations)} violation(s): " + "; ".join(violations[:3])
            if len(violations) > 3:
                detail += f" ... (+{len(violations) - 3} more)"
        return CheckResult(
            dimension="core_isolation", passed=passed, detail=detail,
            weight=dim["weight"], phase=dim["phase"], label=dim["label"],
        )

    def _is_forbidden_import(self, import_path: str) -> bool:
        """Check if an import is forbidden (not in the allowed exceptions list)."""
        for allowed in ALLOWED_CORE_IMPORTS:
            if import_path.startswith(allowed):
                return False
        for forbidden in FORBIDDEN_CORE_IMPORTS:
            if import_path.startswith(forbidden):
                return True
        return False

    def check_port_interfaces(self) -> CheckResult:
        """Verify at least 1 port interface exists in core/port/."""
        dim = DIMENSIONS["port_interfaces"]
        core_dir = self._find_package_dir("core")
        if not core_dir:
            return CheckResult(
                dimension="port_interfaces", passed=False, detail="core/ directory not found",
                weight=dim["weight"], phase=dim["phase"], label=dim["label"],
            )

        port_files = []
        for port_dir in core_dir.rglob("port"):
            if port_dir.is_dir():
                port_files.extend(f.name for f in port_dir.glob("*.java"))

        passed = len(port_files) >= 1
        detail = f"{len(port_files)} port(s): {', '.join(port_files)}" if passed else "No port interfaces found in core/port/"
        return CheckResult(
            dimension="port_interfaces", passed=passed, detail=detail,
            weight=dim["weight"], phase=dim["phase"], label=dim["label"],
        )

    def check_domain_test_coverage(self) -> CheckResult:
        """Verify at least 1 test file exists for core domain packages."""
        dim = DIMENSIONS["domain_test_coverage"]
        core_test_dir = self._find_test_package_dir("core")
        test_count = 0
        test_files = []
        if core_test_dir:
            for test_file in core_test_dir.rglob("*Test.java"):
                test_count += 1
                test_files.append(test_file.name)

        passed = test_count >= 1
        detail = f"{test_count} test(s): {', '.join(test_files[:5])}" if passed else "No test files for core domain"
        return CheckResult(
            dimension="domain_test_coverage", passed=passed, detail=detail,
            weight=dim["weight"], phase=dim["phase"], label=dim["label"],
        )

    # ── Phase 3: Adapters ──

    def check_bridge_configs(self) -> CheckResult:
        """Verify bridge Configuration classes exist in adapter packages."""
        dim = DIMENSIONS["bridge_configs"]
        adapter_packages = ["persistence", "rest", "events"]
        found = []
        missing = []

        for pkg in adapter_packages:
            pkg_dir = self._find_package_dir(pkg)
            if not pkg_dir:
                missing.append(pkg)
                continue
            config_files = list(pkg_dir.glob("*Configuration.java"))
            if config_files:
                found.append(f"{pkg}/{config_files[0].name}")
            else:
                missing.append(pkg)

        passed = len(missing) == 0
        if passed:
            detail = ", ".join(found)
        else:
            detail = f"Missing bridge config in: {', '.join(missing)}"
        return CheckResult(
            dimension="bridge_configs", passed=passed, detail=detail,
            weight=dim["weight"], phase=dim["phase"], label=dim["label"],
        )

    def check_adapter_integration_tests(self) -> CheckResult:
        """Verify at least 1 integration test exists."""
        dim = DIMENSIONS["adapter_integration_tests"]
        test_files = list(self.d.java_test.rglob("*IntegrationTest.java"))

        passed = len(test_files) >= 1
        names = [f.name for f in test_files]
        detail = f"{len(test_files)} test(s): {', '.join(names)}" if passed else "No integration tests found"
        return CheckResult(
            dimension="adapter_integration_tests", passed=passed, detail=detail,
            weight=dim["weight"], phase=dim["phase"], label=dim["label"],
        )

    # ── Phase 4: Auto-configuration ──

    def check_conditional_on_missing_bean(self) -> CheckResult:
        """Verify @Bean methods in core auto-config + bridge configs have @ConditionalOnMissingBean.

        Per architecture rules, @ConditionalOnMissingBean goes on bridge config
        @Bean methods and core fallback beans — NOT blanket parity with all
        @Bean methods in every auto-config (migration, observability, etc. use
        different conditional patterns deliberately).
        """
        dim = DIMENSIONS["conditional_on_missing_bean"]
        bean_pattern = re.compile(r"^\s*@Bean")
        comb_pattern = re.compile(r"^\s*@ConditionalOnMissingBean")

        # Only check: core auto-config + bridge configs (persistence, rest, events)
        files_to_scan = []
        autoconf_dir = self._find_package_dir("autoconfigure")
        if autoconf_dir:
            # Only the core auto-config — not migration/observability/persistence-liquibase
            for f in autoconf_dir.glob("*CoreAutoConfiguration.java"):
                files_to_scan.append(f)
        for pkg in ["persistence", "rest", "events"]:
            pkg_dir = self._find_package_dir(pkg)
            if pkg_dir:
                files_to_scan.extend(pkg_dir.glob("*Configuration.java"))

        total_beans = 0
        total_conditional = 0
        for java_file in files_to_scan:
            try:
                content = java_file.read_text(encoding="utf-8")
            except (OSError, UnicodeDecodeError):
                continue
            lines = content.splitlines()
            for i, line in enumerate(lines):
                if bean_pattern.match(line):
                    if self._is_in_comment(lines, i):
                        continue
                    total_beans += 1
                if comb_pattern.match(line):
                    if not self._is_in_comment(lines, i):
                        total_conditional += 1

        passed = total_beans > 0 and total_beans == total_conditional
        detail = f"{total_conditional}/{total_beans} @Bean methods have @ConditionalOnMissingBean"
        return CheckResult(
            dimension="conditional_on_missing_bean", passed=passed, detail=detail,
            weight=dim["weight"], phase=dim["phase"], label=dim["label"],
        )

    @staticmethod
    def _is_in_comment(lines: list[str], line_idx: int) -> bool:
        """Simple heuristic: check if line is inside a block comment."""
        in_block = False
        for i in range(line_idx + 1):
            line = lines[i].strip()
            if "/*" in line:
                in_block = True
            if "*/" in line:
                in_block = False
        return in_block

    def check_feature_flags_default(self) -> CheckResult:
        """Verify zero occurrences of matchIfMissing = true."""
        dim = DIMENSIONS["feature_flags_default"]
        violations = 0
        files_with_violations = []

        for java_file in self.d.java_main.rglob("*.java"):
            try:
                content = java_file.read_text(encoding="utf-8")
            except (OSError, UnicodeDecodeError):
                continue
            if "matchIfMissing" in content and "true" in content:
                # More precise check
                if re.search(r"matchIfMissing\s*=\s*true", content):
                    violations += 1
                    files_with_violations.append(java_file.name)

        passed = violations == 0
        if passed:
            detail = "No matchIfMissing = true (secure-by-default)"
        else:
            detail = f"{violations} file(s) with matchIfMissing = true: {', '.join(files_with_violations)}"
        return CheckResult(
            dimension="feature_flags_default", passed=passed, detail=detail,
            weight=dim["weight"], phase=dim["phase"], label=dim["label"],
        )

    def check_auto_config_meta_inf(self) -> CheckResult:
        """Verify AutoConfiguration.imports exists with >= 1 entry."""
        dim = DIMENSIONS["auto_config_meta_inf"]
        imports_file = self.d.resources / "META-INF" / "spring" / "org.springframework.boot.autoconfigure.AutoConfiguration.imports"

        if not imports_file.is_file():
            return CheckResult(
                dimension="auto_config_meta_inf", passed=False,
                detail="AutoConfiguration.imports not found",
                weight=dim["weight"], phase=dim["phase"], label=dim["label"],
            )

        try:
            entries = [line.strip() for line in imports_file.read_text(encoding="utf-8").splitlines() if line.strip()]
        except (OSError, UnicodeDecodeError):
            entries = []

        passed = len(entries) >= 1
        detail = f"{len(entries)} auto-config(s) registered" if passed else "AutoConfiguration.imports is empty"
        return CheckResult(
            dimension="auto_config_meta_inf", passed=passed, detail=detail,
            weight=dim["weight"], phase=dim["phase"], label=dim["label"],
        )

    def check_context_runner_tests(self) -> CheckResult:
        """Verify at least 1 test uses ApplicationContextRunner."""
        dim = DIMENSIONS["context_runner_tests"]
        test_files = []

        for test_file in self.d.java_test.rglob("*.java"):
            try:
                content = test_file.read_text(encoding="utf-8")
            except (OSError, UnicodeDecodeError):
                continue
            if "ApplicationContextRunner" in content:
                test_files.append(test_file.name)

        passed = len(test_files) >= 1
        detail = f"{len(test_files)} test(s): {', '.join(test_files)}" if passed else "No context runner tests found"
        return CheckResult(
            dimension="context_runner_tests", passed=passed, detail=detail,
            weight=dim["weight"], phase=dim["phase"], label=dim["label"],
        )

    # ── Phase 5: Frontend ──

    def check_angular_standalone_onpush(self) -> CheckResult:
        """Verify Angular components use standalone: true + OnPush."""
        dim = DIMENSIONS["angular_standalone_onpush"]
        fe_dir = self.d.frontend
        if not fe_dir:
            return CheckResult(
                dimension="angular_standalone_onpush", passed=True,
                detail="No frontend (N/A — pass by default)",
                weight=dim["weight"], phase=dim["phase"], label=dim["label"],
            )

        component_files = list(fe_dir.rglob("*.component.ts"))
        if not component_files:
            return CheckResult(
                dimension="angular_standalone_onpush", passed=True,
                detail="No Angular components found (N/A)",
                weight=dim["weight"], phase=dim["phase"], label=dim["label"],
            )

        standalone_count = 0
        onpush_count = 0
        total = len(component_files)

        for ts_file in component_files:
            try:
                content = ts_file.read_text(encoding="utf-8")
            except (OSError, UnicodeDecodeError):
                continue
            if "standalone: true" in content or "standalone:true" in content:
                standalone_count += 1
            if "OnPush" in content:
                onpush_count += 1

        passed = standalone_count == total and onpush_count == total
        detail = f"{standalone_count}/{total} standalone, {onpush_count}/{total} OnPush"
        return CheckResult(
            dimension="angular_standalone_onpush", passed=passed, detail=detail,
            weight=dim["weight"], phase=dim["phase"], label=dim["label"],
        )

    # ── Helpers ──

    def _find_package_dir(self, package_name: str) -> Optional[Path]:
        """Find a package directory under java_main."""
        candidates = list(self.d.java_main.rglob(package_name))
        for c in candidates:
            if c.is_dir() and c.name == package_name:
                return c
        return None

    def _find_test_package_dir(self, package_name: str) -> Optional[Path]:
        """Find a package directory under java_test."""
        candidates = list(self.d.java_test.rglob(package_name))
        for c in candidates:
            if c.is_dir() and c.name == package_name:
                return c
        return None


# ─── StateManager ───────────────────────────────────────────────────────────

class StateManager:
    """Persist migration state to .migration/state.yml."""

    def __init__(self, target_path: Path):
        self.state_dir = target_path / ".migration"
        self.state_file = self.state_dir / "state.yml"

    def exists(self) -> bool:
        return self.state_file.is_file()

    def load(self) -> MigrationState:
        with open(self.state_file, encoding="utf-8") as f:
            data = yaml.safe_load(f)
        return MigrationState(
            service_name=data["service_name"],
            base_package=data.get("base_package", ""),
            db_prefix=data.get("db_prefix", ""),
            property_prefix=data.get("property_prefix", ""),
            tier=data.get("tier", "Standard"),
            has_frontend=data.get("has_frontend", False),
            current_phase=data.get("current_phase", 0),
            phase_times=data.get("phase_times", {}),
            errors_encountered=data.get("errors_encountered", []),
        )

    def save(self, state: MigrationState) -> None:
        self.state_dir.mkdir(parents=True, exist_ok=True)
        data = {
            "service_name": state.service_name,
            "base_package": state.base_package,
            "db_prefix": state.db_prefix,
            "property_prefix": state.property_prefix,
            "tier": state.tier,
            "has_frontend": state.has_frontend,
            "current_phase": state.current_phase,
            "phase_times": state.phase_times,
            "errors_encountered": state.errors_encountered,
        }
        with open(self.state_file, "w", encoding="utf-8") as f:
            yaml.dump(data, f, default_flow_style=False, sort_keys=False)

    def create(self, service_name: str, tier: str, base_package: str = "",
               db_prefix: str = "", property_prefix: str = "", has_frontend: bool = False) -> MigrationState:
        now = datetime.now(timezone.utc).isoformat()
        state = MigrationState(
            service_name=service_name,
            base_package=base_package,
            db_prefix=db_prefix,
            property_prefix=property_prefix,
            tier=tier,
            has_frontend=has_frontend,
            current_phase=0,
            phase_times={"0": {"started": now, "completed": None}},
            errors_encountered=[],
        )
        self.save(state)
        return state


# ─── WorkflowGuide ──────────────────────────────────────────────────────────

class WorkflowGuide:
    """Interactive phase-by-phase migration guide."""

    def __init__(self, detector: RepoDetector, checker: ScorecardChecker,
                 state_mgr: StateManager, formatter: "TerminalFormatter"):
        self.detector = detector
        self.checker = checker
        self.state_mgr = state_mgr
        self.fmt = formatter

    def run(self, target_phase: Optional[int] = None, skip_gate: bool = False) -> None:
        if self.detector.is_reference:
            print(f"\n{YELLOW}Self-test mode: running all checks against reference repo.{RESET}\n")
            result = self.checker.run_all()
            self.fmt.print_result(result)
            return

        if not self.state_mgr.exists():
            print(f"\n{RED}No migration state found. Run 'init' first.{RESET}")
            print(f"  python3 scripts/migration_cli.py init --service-name NAME --tier TIER\n")
            sys.exit(1)

        state = self.state_mgr.load()
        phase = target_phase if target_phase is not None else state.current_phase

        if phase < 1 or phase > 5:
            print(f"\n{CYAN}{BOLD}Phase 0: Workspace Setup{RESET}")
            print(f"  Service: {state.service_name} ({state.tier})")
            print(f"  Base package: {state.base_package or '(not set)'}")
            print(f"\n{BOLD}Relevant Prompts:{RESET}")
            for p in PHASE_PROMPTS.get(0, []):
                print(f"  - {p}")
            print(f"\n{DIM}When ready, advance to Phase 1 with: guide --phase 1{RESET}\n")
            return

        phase_info = PHASES[phase]
        print(f"\n{CYAN}{BOLD}Phase {phase}: {phase_info['name']}{RESET}")
        print(f"  Service: {state.service_name} ({state.tier})\n")

        print(f"{BOLD}Relevant Prompts:{RESET}")
        for p in PHASE_PROMPTS.get(phase, []):
            print(f"  - {p}")
        print()

        if not skip_gate:
            print(f"{BOLD}Running gate checks...{RESET}\n")
            result = self.checker.run_phase(phase)
            self.fmt.print_result(result)

            if result.all_passed:
                print(f"\n{GREEN}{BOLD}Gate PASSED!{RESET} Phase {phase} is complete.")
                if phase < 5:
                    print(f"  Advance to Phase {phase + 1} with: guide --phase {phase + 1}")
                else:
                    print(f"  {GREEN}Migration complete!{RESET}")
                # Update state
                now = datetime.now(timezone.utc).isoformat()
                phase_key = str(phase)
                if phase_key in state.phase_times:
                    state.phase_times[phase_key]["completed"] = now
                state.current_phase = phase + 1 if phase < 5 else 5
                next_key = str(state.current_phase)
                if next_key not in state.phase_times and state.current_phase <= 5:
                    state.phase_times[next_key] = {"started": now, "completed": None}
                self.state_mgr.save(state)
            else:
                print(f"\n{RED}{BOLD}Gate FAILED.{RESET} Fix failing checks before advancing.")
        else:
            print(f"{DIM}(Gate check skipped with --skip-gate){RESET}\n")


# ─── ResultsRecorder ────────────────────────────────────────────────────────

class ResultsRecorder:
    """Merge verify results into poc-data.yml."""

    def __init__(self, poc_data_path: Path):
        self.poc_data_path = poc_data_path

    def load(self) -> dict:
        if self.poc_data_path.is_file():
            with open(self.poc_data_path, encoding="utf-8") as f:
                return yaml.safe_load(f) or {"services": [], "learnings": {"worked_well": [], "needs_improvement": []}}
        return {"services": [], "learnings": {"worked_well": [], "needs_improvement": []}}

    def save(self, data: dict) -> None:
        self.poc_data_path.parent.mkdir(parents=True, exist_ok=True)
        with open(self.poc_data_path, "w", encoding="utf-8") as f:
            yaml.dump(data, f, default_flow_style=False, sort_keys=False, allow_unicode=True)

    def record(self, service_name: str, result: VerifyResult, dry_run: bool = False) -> dict:
        """Create a new attempt entry from verify results."""
        data = self.load()

        # Find or create service entry
        service = None
        for svc in data.get("services", []):
            if svc["name"] == service_name:
                service = svc
                break

        if not service:
            service = {"name": service_name, "tier": "Unknown", "attempts": []}
            data.setdefault("services", []).append(service)

        # Build scores by phase
        scores = {}
        for phase_result in result.phases:
            scores[f"phase_{phase_result.phase}"] = round(phase_result.score)

        # Determine phase reached
        phase_reached = 0
        for phase_result in sorted(result.phases, key=lambda p: p.phase):
            if phase_result.all_passed:
                phase_reached = phase_result.phase
            else:
                break

        attempt_num = len(service["attempts"]) + 1
        attempt = {
            "attempt": attempt_num,
            "date": datetime.now(timezone.utc).strftime("%Y-%m-%d"),
            "copilot_model": "automated-verify",
            "phase_reached": phase_reached,
            "scores": scores,
            "timing": {"total_hours": None, **{f"phase_{i}_hours": None for i in range(6)}},
            "quality": {
                "first_try_correct_pct": 0,
                "recovery_prompts_used": 0,
                "archunit_violations": 0,
            },
            "errors": [],
            "improvements": [],
            "notes": f"Automated scorecard: {result.passing_dimensions}/{result.total_dimensions} dimensions, {result.overall_score:.0f}%",
        }
        service["attempts"].append(attempt)

        if dry_run:
            print(yaml.dump({"new_attempt": attempt}, default_flow_style=False, sort_keys=False))
        else:
            self.save(data)
            print(f"Recorded attempt #{attempt_num} for {service_name} -> {self.poc_data_path}")

        return attempt


# ─── TerminalFormatter ──────────────────────────────────────────────────────

class TerminalFormatter:
    """ANSI-colored terminal output."""

    def print_result(self, result: VerifyResult) -> None:
        mode = "self-test" if result.is_self_test else "external"
        header = f" MIGRATION SCORECARD — {result.repo_name} ({mode})"
        separator = "═" * max(60, len(header) + 2)

        print(f"\n{BOLD}{WHITE} {header}{RESET}")
        print(f" {separator}\n")

        for phase in result.phases:
            phase_status = f"{GREEN}[{phase.score:.0f}%]{RESET}" if phase.all_passed else f"{RED}[{phase.score:.0f}%]{RESET}"
            print(f" {BOLD}Phase {phase.phase}: {phase.name} ({PHASES[phase.phase]['weight'] * 100:.0f}%){RESET}  {phase_status}")

            for check in phase.checks:
                icon = f"{GREEN}✓{RESET}" if check.passed else f"{RED}✗{RESET}"
                weight_str = f"{check.weight * 100:.0f}%"
                print(f"   {icon} {check.label:<50s} {weight_str}")
                print(f"     {DIM}{check.detail}{RESET}")

            print()

        print(f" {separator}")
        total_icon = f"{GREEN}" if result.all_passed else f"{RED}"
        print(f" {BOLD}{total_icon}OVERALL SCORE: {result.overall_score:.0f}%  "
              f"({result.passing_dimensions}/{result.total_dimensions} dimensions passing){RESET}")
        print(f" {separator}\n")


# ─── JsonFormatter ──────────────────────────────────────────────────────────

class JsonFormatter:
    """JSON output for machine consumption."""

    @staticmethod
    def format_result(result: VerifyResult) -> str:
        data = {
            "repo_name": result.repo_name,
            "is_self_test": result.is_self_test,
            "timestamp": result.timestamp,
            "overall_score": round(result.overall_score, 1),
            "passing": result.passing_dimensions,
            "total": result.total_dimensions,
            "all_passed": result.all_passed,
            "phases": [],
        }
        for phase in result.phases:
            phase_data = {
                "phase": phase.phase,
                "name": phase.name,
                "weight": phase.weight,
                "score": round(phase.score, 1),
                "all_passed": phase.all_passed,
                "checks": [],
            }
            for check in phase.checks:
                phase_data["checks"].append({
                    "dimension": check.dimension,
                    "label": check.label,
                    "passed": check.passed,
                    "weight": check.weight,
                    "detail": check.detail,
                })
            data["phases"].append(phase_data)
        return json.dumps(data, indent=2)


# ─── Target Resolution (URL or Path) ────────────────────────────────────────

GIT_URL_PATTERN = re.compile(
    r"^(?:https?://|git@|ssh://)"  # protocol prefixes
    r"|\.git$"                      # or ends with .git
)

CLONE_BASE_DIR = REPO_ROOT / ".migration" / "repos"


def _is_git_url(value: str) -> bool:
    """Detect whether the target value looks like a git URL."""
    return bool(GIT_URL_PATTERN.search(value))


def _repo_name_from_url(url: str) -> str:
    """Extract a directory name from a git URL.

    Examples:
        https://github.com/org/billing-service.git  -> billing-service
        git@github.com:org/payment-gateway.git      -> payment-gateway
        https://dev.azure.com/org/proj/_git/svc     -> svc
    """
    # Strip trailing slashes and .git suffix
    name = url.rstrip("/")
    if name.endswith(".git"):
        name = name[:-4]
    # Take the last path segment
    name = name.rsplit("/", 1)[-1]
    # Handle git@host:org/repo format
    if ":" in name:
        name = name.rsplit(":", 1)[-1].rsplit("/", 1)[-1]
    return name or "target"


def _clone_or_pull(url: str, dest: Path) -> Path:
    """Clone a repo or pull if it already exists. Returns the repo path."""
    if dest.is_dir() and (dest / ".git").is_dir():
        print(f"{CYAN}Updating existing clone: {dest.name}{RESET}")
        subprocess.run(
            ["git", "-C", str(dest), "pull", "--ff-only"],
            check=False,  # non-fatal — stale clone still usable
        )
    else:
        dest.parent.mkdir(parents=True, exist_ok=True)
        print(f"{CYAN}Cloning {url}{RESET}")
        print(f"  -> {dest}\n")
        subprocess.run(["git", "clone", url, str(dest)], check=True)
    return dest


def resolve_target(raw_target: Optional[str]) -> Path:
    """Resolve --target to a local directory, cloning if a URL is given.

    Rules:
        - None / empty       -> REPO_ROOT (current repo, self-test)
        - local path          -> resolve to absolute
        - git URL             -> clone to .migration/repos/<name>/ and return that path
    """
    if not raw_target:
        return REPO_ROOT

    if _is_git_url(raw_target):
        repo_name = _repo_name_from_url(raw_target)
        dest = CLONE_BASE_DIR / repo_name
        return _clone_or_pull(raw_target, dest)

    return Path(raw_target).resolve()


# ─── Subcommands ────────────────────────────────────────────────────────────

def cmd_verify(args: argparse.Namespace) -> int:
    """Run scorecard checks."""
    target_path = resolve_target(args.target)

    # Auto-detect self-test
    if args.self_test or (target_path / REFERENCE_MARKER).is_dir():
        detector = RepoDetector(target_path)
    else:
        state_mgr = StateManager(target_path)
        state = state_mgr.load() if state_mgr.exists() else None
        detector = RepoDetector(target_path, state)

    checker = ScorecardChecker(detector)

    if args.phase is not None:
        if args.phase not in PHASES:
            print(f"Invalid phase: {args.phase}. Must be 1-5.", file=sys.stderr)
            return 1
        result = checker.run_phase(args.phase)
    else:
        result = checker.run_all()

    if args.json:
        print(JsonFormatter.format_result(result))
    else:
        TerminalFormatter().print_result(result)

    return 0 if result.all_passed else 1


def cmd_status(args: argparse.Namespace) -> int:
    """Show migration status dashboard."""
    target_path = resolve_target(args.target)
    state_mgr = StateManager(target_path)

    if (target_path / REFERENCE_MARKER).is_dir():
        # Self-test mode — just run verify
        detector = RepoDetector(target_path)
        checker = ScorecardChecker(detector)
        result = checker.run_all()
        if args.json:
            print(JsonFormatter.format_result(result))
        else:
            TerminalFormatter().print_result(result)
        return 0 if result.all_passed else 1

    if not state_mgr.exists():
        print(f"\n{RED}No migration state found.{RESET}")
        print(f"  Run 'init' first, or use '--target' to point to a migration.\n")
        return 1

    state = state_mgr.load()
    detector = RepoDetector(target_path, state)
    checker = ScorecardChecker(detector)
    result = checker.run_all()

    if args.json:
        status_data = {
            "service_name": state.service_name,
            "tier": state.tier,
            "current_phase": state.current_phase,
            "scorecard": json.loads(JsonFormatter.format_result(result)),
        }
        print(json.dumps(status_data, indent=2))
    else:
        print(f"\n{BOLD}{CYAN}Migration Status: {state.service_name}{RESET}")
        print(f"  Tier: {state.tier}")
        print(f"  Current Phase: {state.current_phase}")
        print(f"  Base Package: {state.base_package or '(not set)'}")
        print()
        TerminalFormatter().print_result(result)

    return 0 if result.all_passed else 1


def cmd_init(args: argparse.Namespace) -> int:
    """Initialize migration state."""
    target_path = resolve_target(args.target)
    state_mgr = StateManager(target_path)

    if state_mgr.exists():
        print(f"{YELLOW}Migration state already exists at {state_mgr.state_file}{RESET}")
        print(f"  Delete .migration/state.yml to reinitialize.\n")
        return 1

    state = state_mgr.create(
        service_name=args.service_name,
        tier=args.tier,
        base_package=getattr(args, "base_package", ""),
        db_prefix=getattr(args, "db_prefix", ""),
        property_prefix=getattr(args, "property_prefix", ""),
        has_frontend=getattr(args, "has_frontend", False),
    )

    print(f"\n{GREEN}Migration initialized!{RESET}")
    print(f"  Service: {state.service_name}")
    print(f"  Tier: {state.tier}")
    print(f"  State file: {state_mgr.state_file}")
    print(f"\n  Next: run 'guide' to start Phase 0.\n")
    return 0


def cmd_guide(args: argparse.Namespace) -> int:
    """Interactive migration guide."""
    target_path = resolve_target(args.target)
    state_mgr = StateManager(target_path)

    state = state_mgr.load() if state_mgr.exists() else None
    detector = RepoDetector(target_path, state)
    checker = ScorecardChecker(detector)
    formatter = TerminalFormatter()
    guide = WorkflowGuide(detector, checker, state_mgr, formatter)

    guide.run(
        target_phase=args.phase,
        skip_gate=getattr(args, "skip_gate", False),
    )
    return 0


def cmd_record(args: argparse.Namespace) -> int:
    """Record verify results to poc-data.yml."""
    target_path = resolve_target(args.target)
    poc_data_path = Path(args.poc_data) if args.poc_data else REPO_ROOT / "docs" / "presentations" / "poc-data.yml"

    state_mgr = StateManager(target_path)
    state = state_mgr.load() if state_mgr.exists() else None
    detector = RepoDetector(target_path, state)
    checker = ScorecardChecker(detector)
    result = checker.run_all()

    TerminalFormatter().print_result(result)

    recorder = ResultsRecorder(poc_data_path)
    recorder.record(
        service_name=args.service_name,
        result=result,
        dry_run=getattr(args, "dry_run", False),
    )
    return 0 if result.all_passed else 1


# ─── Argument Parsing ───────────────────────────────────────────────────────

def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="migration_cli",
        description="Migration CLI — Guided workflow + scorecard verification for OneFinancial.",
    )
    subparsers = parser.add_subparsers(dest="command", help="Available subcommands")

    # verify
    p_verify = subparsers.add_parser("verify", help="Run scorecard checks")
    p_verify.add_argument("--self-test", action="store_true", help="Run against reference repo")
    p_verify.add_argument("--phase", type=int, help="Run checks for a single phase (1-5)")
    p_verify.add_argument("--json", action="store_true", help="Output JSON instead of terminal")
    p_verify.add_argument("--target", help="Path or git URL of target repo (default: current repo)")

    # status
    p_status = subparsers.add_parser("status", help="Show migration status dashboard")
    p_status.add_argument("--target", help="Path or git URL of target repo")
    p_status.add_argument("--json", action="store_true", help="Output JSON")

    # init
    p_init = subparsers.add_parser("init", help="Initialize migration state")
    p_init.add_argument("--service-name", required=True, help="Name of the service")
    p_init.add_argument("--tier", required=True, choices=["Simple", "Standard", "Advanced"], help="Migration tier")
    p_init.add_argument("--base-package", default="", help="Java base package (e.g., com.onefinancial.billing)")
    p_init.add_argument("--db-prefix", default="", help="DB table prefix (e.g., bs_)")
    p_init.add_argument("--property-prefix", default="", help="Spring property prefix")
    p_init.add_argument("--has-frontend", action="store_true", help="Service has an Angular frontend")
    p_init.add_argument("--target", help="Path or git URL of target repo")

    # guide
    p_guide = subparsers.add_parser("guide", help="Interactive migration guide")
    p_guide.add_argument("--target", help="Path or git URL of target repo")
    p_guide.add_argument("--phase", type=int, help="Jump to specific phase (0-5)")
    p_guide.add_argument("--skip-gate", action="store_true", help="Skip gate checks")

    # record
    p_record = subparsers.add_parser("record", help="Record results to poc-data.yml")
    p_record.add_argument("--service-name", required=True, help="Service name for the record")
    p_record.add_argument("--poc-data", help="Path to poc-data.yml")
    p_record.add_argument("--dry-run", action="store_true", help="Print without saving")
    p_record.add_argument("--target", help="Path or git URL of target repo")

    return parser


# ─── Main ───────────────────────────────────────────────────────────────────

def main() -> int:
    parser = build_parser()
    args = parser.parse_args()

    if not args.command:
        parser.print_help()
        return 1

    commands = {
        "verify": cmd_verify,
        "status": cmd_status,
        "init": cmd_init,
        "guide": cmd_guide,
        "record": cmd_record,
    }

    handler = commands.get(args.command)
    if not handler:
        parser.print_help()
        return 1

    return handler(args)


if __name__ == "__main__":
    sys.exit(main())
