#!/usr/bin/env python3
"""Tests for migration_cli.py.

Run with:
    python3 -m pytest scripts/test_migration_cli.py -v

Or via Docker:
    docker compose -f docker/docker-compose.yml run --rm python-build \
        sh -c "pip install --quiet --no-cache-dir pyyaml pytest && \
               python3 -m pytest scripts/test_migration_cli.py -v"
"""

from __future__ import annotations

import json
import os
import subprocess
import textwrap
from pathlib import Path
from unittest.mock import patch, MagicMock

import pytest

# Import the module under test
sys_path_entry = str(Path(__file__).resolve().parent)
import sys
if sys_path_entry not in sys.path:
    sys.path.insert(0, sys_path_entry)

import migration_cli as cli


# ─── Fixtures ───────────────────────────────────────────────────────────────

@pytest.fixture
def repo_root():
    """The real repo root for self-test checks."""
    return cli.REPO_ROOT


@pytest.fixture
def reference_detector(repo_root):
    return cli.RepoDetector(repo_root)


@pytest.fixture
def reference_checker(reference_detector):
    return cli.ScorecardChecker(reference_detector)


@pytest.fixture
def tmp_repo(tmp_path):
    """Create a minimal fake repo structure for isolated tests."""
    java_root = tmp_path / "src" / "main" / "java" / "com" / "example" / "svc"
    test_root = tmp_path / "src" / "test" / "java" / "com" / "example" / "svc"
    resources = tmp_path / "src" / "main" / "resources"

    # Package structure
    for pkg in ["core", "core/port", "core/service", "persistence", "rest", "events", "autoconfigure"]:
        (java_root / pkg).mkdir(parents=True, exist_ok=True)

    test_root.mkdir(parents=True, exist_ok=True)
    resources.mkdir(parents=True, exist_ok=True)

    return tmp_path, java_root, test_root, resources


# ─── URL Detection Tests ───────────────────────────────────────────────────

class TestIsGitUrl:
    def test_https_url(self):
        assert cli._is_git_url("https://github.com/org/repo.git")

    def test_http_url(self):
        assert cli._is_git_url("http://github.com/org/repo")

    def test_ssh_url(self):
        assert cli._is_git_url("ssh://git@github.com/org/repo.git")

    def test_git_at_url(self):
        assert cli._is_git_url("git@github.com:org/repo.git")

    def test_azure_devops_url(self):
        assert cli._is_git_url("https://dev.azure.com/org/project/_git/repo")

    def test_local_path_not_url(self):
        assert not cli._is_git_url("/home/user/projects/repo")

    def test_relative_path_not_url(self):
        assert not cli._is_git_url("../other-repo")

    def test_dot_git_suffix_is_url(self):
        # .git suffix alone is treated as a URL indicator
        assert cli._is_git_url("some-host/repo.git")

    def test_plain_name_not_url(self):
        assert not cli._is_git_url("my-repo")


# ─── Repo Name Extraction Tests ────────────────────────────────────────────

class TestRepoNameFromUrl:
    def test_https_with_git_suffix(self):
        assert cli._repo_name_from_url("https://github.com/org/billing-service.git") == "billing-service"

    def test_https_without_git_suffix(self):
        assert cli._repo_name_from_url("https://github.com/org/payment-gateway") == "payment-gateway"

    def test_ssh_url(self):
        assert cli._repo_name_from_url("git@github.com:org/my-service.git") == "my-service"

    def test_azure_devops_url(self):
        assert cli._repo_name_from_url("https://dev.azure.com/org/proj/_git/svc") == "svc"

    def test_trailing_slash_stripped(self):
        assert cli._repo_name_from_url("https://github.com/org/repo/") == "repo"

    def test_fallback_to_target(self):
        assert cli._repo_name_from_url("") == "target"


# ─── resolve_target Tests ──────────────────────────────────────────────────

class TestResolveTarget:
    def test_none_returns_repo_root(self):
        assert cli.resolve_target(None) == cli.REPO_ROOT

    def test_empty_string_returns_repo_root(self):
        assert cli.resolve_target("") == cli.REPO_ROOT

    def test_local_path_resolved(self, tmp_path):
        result = cli.resolve_target(str(tmp_path))
        assert result == tmp_path.resolve()

    @patch("migration_cli._clone_or_pull")
    def test_url_triggers_clone(self, mock_clone):
        url = "https://github.com/org/billing-service.git"
        expected_dest = cli.CLONE_BASE_DIR / "billing-service"
        mock_clone.return_value = expected_dest

        result = cli.resolve_target(url)

        mock_clone.assert_called_once_with(url, expected_dest)
        assert result == expected_dest

    @patch("migration_cli._clone_or_pull")
    def test_ssh_url_triggers_clone(self, mock_clone):
        url = "git@github.com:org/payment-gateway.git"
        expected_dest = cli.CLONE_BASE_DIR / "payment-gateway"
        mock_clone.return_value = expected_dest

        result = cli.resolve_target(url)

        mock_clone.assert_called_once_with(url, expected_dest)
        assert result == expected_dest


# ─── clone_or_pull Tests ───────────────────────────────────────────────────

class TestCloneOrPull:
    @patch("subprocess.run")
    def test_clone_when_no_existing_dir(self, mock_run, tmp_path):
        dest = tmp_path / "new-repo"
        result = cli._clone_or_pull("https://github.com/org/repo.git", dest)

        mock_run.assert_called_once_with(
            ["git", "clone", "https://github.com/org/repo.git", str(dest)],
            check=True,
        )
        assert result == dest

    @patch("subprocess.run")
    def test_pull_when_existing_git_dir(self, mock_run, tmp_path):
        dest = tmp_path / "existing-repo"
        dest.mkdir()
        (dest / ".git").mkdir()

        result = cli._clone_or_pull("https://github.com/org/repo.git", dest)

        mock_run.assert_called_once_with(
            ["git", "-C", str(dest), "pull", "--ff-only"],
            check=False,
        )
        assert result == dest


# ─── RepoDetector Tests ────────────────────────────────────────────────────

class TestRepoDetector:
    def test_reference_repo_detected(self, repo_root):
        detector = cli.RepoDetector(repo_root)
        assert detector.is_reference is True
        assert "Reference" in detector.repo_name

    def test_reference_java_main_path(self, repo_root):
        detector = cli.RepoDetector(repo_root)
        assert "customer-registry-starter" in str(detector.java_main)
        assert detector.java_main.is_dir()

    def test_reference_java_test_path(self, repo_root):
        detector = cli.RepoDetector(repo_root)
        assert "customer-registry-starter" in str(detector.java_test)
        assert detector.java_test.is_dir()

    def test_reference_frontend_path(self, repo_root):
        detector = cli.RepoDetector(repo_root)
        assert detector.frontend is not None
        assert detector.frontend.is_dir()

    def test_external_repo_not_reference(self, tmp_path):
        detector = cli.RepoDetector(tmp_path)
        assert detector.is_reference is False

    def test_external_repo_name_from_state(self, tmp_path):
        state = cli.MigrationState(
            service_name="BillingService", base_package="", db_prefix="",
            property_prefix="", tier="Standard", has_frontend=False,
            current_phase=0, phase_times={}, errors_encountered=[],
        )
        detector = cli.RepoDetector(tmp_path, state)
        assert detector.repo_name == "BillingService"

    def test_external_repo_name_fallback_to_dirname(self, tmp_path):
        detector = cli.RepoDetector(tmp_path)
        assert detector.repo_name == tmp_path.name


# ─── ScorecardChecker: Reference Repo Self-Tests ───────────────────────────

class TestScorecardCheckerSelfTest:
    """These tests run against the real reference repo and must all pass."""

    def test_run_all_passes(self, reference_checker):
        result = reference_checker.run_all()
        assert result.all_passed, (
            f"Self-test failed: {result.passing_dimensions}/{result.total_dimensions}. "
            f"Failing: {[c.dimension for p in result.phases for c in p.checks if not c.passed]}"
        )
        assert result.overall_score == 100.0

    def test_total_dimensions_is_13(self, reference_checker):
        result = reference_checker.run_all()
        assert result.total_dimensions == 13

    def test_each_phase_passes(self, reference_checker):
        result = reference_checker.run_all()
        for phase in result.phases:
            assert phase.all_passed, f"Phase {phase.phase} ({phase.name}) failed"

    def test_run_phase_filters_correctly(self, reference_checker):
        for phase_num in range(1, 6):
            result = reference_checker.run_phase(phase_num)
            assert len(result.phases) == 1
            assert result.phases[0].phase == phase_num


# ─── ScorecardChecker: Individual Check Validation ─────────────────────────

class TestIndividualChecks:
    """Validate details of individual check results on the reference repo."""

    def test_package_structure_finds_all_packages(self, reference_checker):
        result = reference_checker.check_package_structure()
        assert result.passed
        assert "core/" in result.detail
        assert "persistence/" in result.detail
        assert "rest/" in result.detail

    def test_modulith_marker_found(self, reference_checker):
        result = reference_checker.check_modulith_marker()
        assert result.passed
        assert "CustomerRegistryModule.java" in result.detail

    def test_ci_pipeline_found(self, reference_checker):
        result = reference_checker.check_ci_pipeline()
        assert result.passed
        assert "pr-pipeline.yml" in result.detail

    def test_core_isolation_no_violations(self, reference_checker):
        result = reference_checker.check_core_isolation()
        assert result.passed
        assert "No forbidden" in result.detail

    def test_port_interfaces_count(self, reference_checker):
        result = reference_checker.check_port_interfaces()
        assert result.passed
        assert "2 port(s)" in result.detail

    def test_domain_test_coverage(self, reference_checker):
        result = reference_checker.check_domain_test_coverage()
        assert result.passed
        assert int(result.detail.split()[0]) >= 1

    def test_bridge_configs_all_present(self, reference_checker):
        result = reference_checker.check_bridge_configs()
        assert result.passed
        assert "persistence/" in result.detail
        assert "rest/" in result.detail
        assert "events/" in result.detail

    def test_integration_tests_found(self, reference_checker):
        result = reference_checker.check_adapter_integration_tests()
        assert result.passed
        assert int(result.detail.split()[0]) >= 1

    def test_conditional_on_missing_bean_parity(self, reference_checker):
        result = reference_checker.check_conditional_on_missing_bean()
        assert result.passed
        # N/N means all @Bean have matching @ConditionalOnMissingBean
        parts = result.detail.split("/")
        assert parts[0].strip() == parts[1].split()[0].strip()

    def test_feature_flags_secure_by_default(self, reference_checker):
        result = reference_checker.check_feature_flags_default()
        assert result.passed
        assert "matchIfMissing" not in result.detail or "No matchIfMissing" in result.detail

    def test_auto_config_meta_inf(self, reference_checker):
        result = reference_checker.check_auto_config_meta_inf()
        assert result.passed
        assert "6 auto-config" in result.detail

    def test_context_runner_tests(self, reference_checker):
        result = reference_checker.check_context_runner_tests()
        assert result.passed
        assert "5 test(s)" in result.detail

    def test_angular_standalone_onpush(self, reference_checker):
        result = reference_checker.check_angular_standalone_onpush()
        assert result.passed
        assert "5/5 standalone" in result.detail
        assert "5/5 OnPush" in result.detail


# ─── ScorecardChecker: Failure Scenarios (synthetic repos) ──────────────────

class TestCheckerFailures:
    """Verify checks correctly detect failures on synthetic repos."""

    def test_missing_core_fails_package_structure(self, tmp_repo):
        tmp_path, java_root, _, _ = tmp_repo
        # Remove core directory
        import shutil
        shutil.rmtree(java_root / "core")

        detector = cli.RepoDetector(tmp_path)
        checker = cli.ScorecardChecker(detector)
        result = checker.check_package_structure()
        assert not result.passed
        assert "Missing" in result.detail

    def test_no_modulith_marker_fails(self, tmp_repo):
        tmp_path, java_root, _, _ = tmp_repo
        detector = cli.RepoDetector(tmp_path)
        checker = cli.ScorecardChecker(detector)
        result = checker.check_modulith_marker()
        assert not result.passed

    def test_forbidden_import_detected(self, tmp_repo):
        tmp_path, java_root, _, _ = tmp_repo
        # Write a core class with forbidden import
        core_class = java_root / "core" / "BadService.java"
        core_class.write_text(textwrap.dedent("""\
            package com.example.svc.core;

            import jakarta.persistence.Entity;

            public class BadService {}
        """))

        detector = cli.RepoDetector(tmp_path)
        checker = cli.ScorecardChecker(detector)
        result = checker.check_core_isolation()
        assert not result.passed
        assert "jakarta.persistence" in result.detail

    def test_allowed_import_not_flagged(self, tmp_repo):
        tmp_path, java_root, _, _ = tmp_repo
        # Write a core class with allowed import
        core_class = java_root / "core" / "GoodService.java"
        core_class.write_text(textwrap.dedent("""\
            package com.example.svc.core;

            import org.springframework.transaction.annotation.Transactional;

            public class GoodService {}
        """))

        detector = cli.RepoDetector(tmp_path)
        checker = cli.ScorecardChecker(detector)
        result = checker.check_core_isolation()
        assert result.passed

    def test_no_port_interfaces_fails(self, tmp_repo):
        tmp_path, java_root, _, _ = tmp_repo
        # port/ exists but is empty
        detector = cli.RepoDetector(tmp_path)
        checker = cli.ScorecardChecker(detector)
        result = checker.check_port_interfaces()
        assert not result.passed

    def test_no_ci_pipeline_fails(self, tmp_repo):
        tmp_path, _, _, _ = tmp_repo
        detector = cli.RepoDetector(tmp_path)
        checker = cli.ScorecardChecker(detector)
        result = checker.check_ci_pipeline()
        assert not result.passed

    def test_match_if_missing_true_fails(self, tmp_repo):
        tmp_path, java_root, _, _ = tmp_repo
        bad_config = java_root / "autoconfigure" / "BadAutoConfig.java"
        bad_config.write_text(textwrap.dedent("""\
            @ConditionalOnProperty(name = "foo", matchIfMissing = true)
            public class BadAutoConfig {}
        """))

        detector = cli.RepoDetector(tmp_path)
        checker = cli.ScorecardChecker(detector)
        result = checker.check_feature_flags_default()
        assert not result.passed

    def test_no_meta_inf_fails(self, tmp_repo):
        tmp_path, _, _, _ = tmp_repo
        detector = cli.RepoDetector(tmp_path)
        checker = cli.ScorecardChecker(detector)
        result = checker.check_auto_config_meta_inf()
        assert not result.passed

    def test_no_frontend_passes_by_default(self, tmp_repo):
        tmp_path, _, _, _ = tmp_repo
        detector = cli.RepoDetector(tmp_path)
        checker = cli.ScorecardChecker(detector)
        result = checker.check_angular_standalone_onpush()
        assert result.passed
        assert "N/A" in result.detail


# ─── Core Isolation Boundary Rules ──────────────────────────────────────────

class TestCoreIsolationBoundary:
    """Exhaustive tests for the forbidden/allowed import logic."""

    def test_jakarta_persistence_forbidden(self):
        checker = cli.ScorecardChecker.__new__(cli.ScorecardChecker)
        assert checker._is_forbidden_import("jakarta.persistence.Entity")

    def test_spring_data_forbidden(self):
        checker = cli.ScorecardChecker.__new__(cli.ScorecardChecker)
        assert checker._is_forbidden_import("org.springframework.data.jpa.repository.JpaRepository")

    def test_spring_web_forbidden(self):
        checker = cli.ScorecardChecker.__new__(cli.ScorecardChecker)
        assert checker._is_forbidden_import("org.springframework.web.bind.annotation.RestController")

    def test_spring_stereotype_forbidden(self):
        checker = cli.ScorecardChecker.__new__(cli.ScorecardChecker)
        assert checker._is_forbidden_import("org.springframework.stereotype.Service")

    def test_spring_boot_forbidden(self):
        checker = cli.ScorecardChecker.__new__(cli.ScorecardChecker)
        assert checker._is_forbidden_import("org.springframework.boot.autoconfigure.AutoConfiguration")

    def test_transactional_allowed(self):
        checker = cli.ScorecardChecker.__new__(cli.ScorecardChecker)
        assert not checker._is_forbidden_import("org.springframework.transaction.annotation.Transactional")

    def test_modulith_allowed(self):
        checker = cli.ScorecardChecker.__new__(cli.ScorecardChecker)
        assert not checker._is_forbidden_import("org.springframework.modulith.core.ApplicationModule")

    def test_java_util_not_forbidden(self):
        checker = cli.ScorecardChecker.__new__(cli.ScorecardChecker)
        assert not checker._is_forbidden_import("java.util.List")

    def test_domain_import_not_forbidden(self):
        checker = cli.ScorecardChecker.__new__(cli.ScorecardChecker)
        assert not checker._is_forbidden_import("com.onefinancial.customer.core.model.Customer")


# ─── StateManager Tests ────────────────────────────────────────────────────

class TestStateManager:
    def test_create_and_load(self, tmp_path):
        mgr = cli.StateManager(tmp_path)
        state = mgr.create(
            service_name="TestService",
            tier="Standard",
            base_package="com.example.test",
        )
        assert mgr.exists()
        loaded = mgr.load()
        assert loaded.service_name == "TestService"
        assert loaded.tier == "Standard"
        assert loaded.base_package == "com.example.test"
        assert loaded.current_phase == 0

    def test_save_updates_state(self, tmp_path):
        mgr = cli.StateManager(tmp_path)
        state = mgr.create(service_name="Svc", tier="Simple")
        state.current_phase = 3
        mgr.save(state)
        reloaded = mgr.load()
        assert reloaded.current_phase == 3

    def test_exists_false_when_no_file(self, tmp_path):
        mgr = cli.StateManager(tmp_path)
        assert not mgr.exists()


# ─── VerifyResult / PhaseResult Dataclass Tests ─────────────────────────────

class TestDataclasses:
    def _make_check(self, passed: bool, weight: float = 0.1) -> cli.CheckResult:
        return cli.CheckResult(
            dimension="test", passed=passed, detail="", weight=weight,
            phase=1, label="Test",
        )

    def test_phase_score_all_pass(self):
        phase = cli.PhaseResult(
            phase=1, name="Test", weight=0.25,
            checks=[self._make_check(True, 0.1), self._make_check(True, 0.15)],
        )
        assert phase.score == 100.0
        assert phase.all_passed

    def test_phase_score_partial(self):
        phase = cli.PhaseResult(
            phase=1, name="Test", weight=0.25,
            checks=[self._make_check(True, 0.1), self._make_check(False, 0.15)],
        )
        assert phase.score == pytest.approx(40.0)
        assert not phase.all_passed

    def test_verify_result_overall(self):
        result = cli.VerifyResult(
            phases=[
                cli.PhaseResult(1, "A", 0.5, [self._make_check(True, 0.3), self._make_check(True, 0.2)]),
                cli.PhaseResult(2, "B", 0.5, [self._make_check(False, 0.5)]),
            ],
            repo_name="test",
            is_self_test=False,
        )
        # 0.3 + 0.2 pass out of 0.3 + 0.2 + 0.5 = 1.0
        assert result.overall_score == 50.0
        assert result.passing_dimensions == 2
        assert result.total_dimensions == 3
        assert not result.all_passed


# ─── JsonFormatter Tests ───────────────────────────────────────────────────

class TestJsonFormatter:
    def test_json_output_is_valid(self, reference_checker):
        result = reference_checker.run_all()
        json_str = cli.JsonFormatter.format_result(result)
        data = json.loads(json_str)
        assert data["all_passed"] is True
        assert data["overall_score"] == 100.0
        assert len(data["phases"]) == 5

    def test_json_contains_all_dimensions(self, reference_checker):
        result = reference_checker.run_all()
        data = json.loads(cli.JsonFormatter.format_result(result))
        all_checks = [c for p in data["phases"] for c in p["checks"]]
        assert len(all_checks) == 13


# ─── ResultsRecorder Tests ─────────────────────────────────────────────────

class TestResultsRecorder:
    def test_record_creates_new_entry(self, tmp_path, reference_checker):
        poc_file = tmp_path / "poc-data.yml"
        recorder = cli.ResultsRecorder(poc_file)
        result = reference_checker.run_all()

        recorder.record("TestService", result)

        import yaml
        with open(poc_file) as f:
            data = yaml.safe_load(f)
        assert len(data["services"]) == 1
        assert data["services"][0]["name"] == "TestService"
        assert data["services"][0]["attempts"][0]["attempt"] == 1

    def test_record_dry_run_does_not_write(self, tmp_path, reference_checker):
        poc_file = tmp_path / "poc-data.yml"
        recorder = cli.ResultsRecorder(poc_file)
        result = reference_checker.run_all()

        recorder.record("TestService", result, dry_run=True)

        assert not poc_file.exists()

    def test_record_appends_attempt(self, tmp_path, reference_checker):
        poc_file = tmp_path / "poc-data.yml"
        recorder = cli.ResultsRecorder(poc_file)
        result = reference_checker.run_all()

        recorder.record("TestService", result)
        recorder.record("TestService", result)

        import yaml
        with open(poc_file) as f:
            data = yaml.safe_load(f)
        attempts = data["services"][0]["attempts"]
        assert len(attempts) == 2
        assert attempts[1]["attempt"] == 2


# ─── CLI Argument Parsing Tests ─────────────────────────────────────────────

class TestArgParsing:
    def test_verify_defaults(self):
        parser = cli.build_parser()
        args = parser.parse_args(["verify"])
        assert args.command == "verify"
        assert args.self_test is False
        assert args.phase is None
        assert args.json is False

    def test_verify_with_all_flags(self):
        parser = cli.build_parser()
        args = parser.parse_args(["verify", "--self-test", "--phase", "2", "--json"])
        assert args.self_test is True
        assert args.phase == 2
        assert args.json is True

    def test_verify_with_url_target(self):
        parser = cli.build_parser()
        args = parser.parse_args(["verify", "--target", "https://github.com/org/repo.git"])
        assert args.target == "https://github.com/org/repo.git"

    def test_init_required_args(self):
        parser = cli.build_parser()
        args = parser.parse_args(["init", "--service-name", "Billing", "--tier", "Standard"])
        assert args.service_name == "Billing"
        assert args.tier == "Standard"

    def test_guide_with_phase(self):
        parser = cli.build_parser()
        args = parser.parse_args(["guide", "--phase", "3", "--skip-gate"])
        assert args.phase == 3
        assert args.skip_gate is True

    def test_record_required_args(self):
        parser = cli.build_parser()
        args = parser.parse_args(["record", "--service-name", "Svc"])
        assert args.service_name == "Svc"


# ─── End-to-End: cmd_verify self-test ───────────────────────────────────────

class TestCmdVerify:
    def test_self_test_returns_zero(self):
        parser = cli.build_parser()
        args = parser.parse_args(["verify", "--self-test"])
        exit_code = cli.cmd_verify(args)
        assert exit_code == 0

    def test_self_test_json_output(self, capsys):
        parser = cli.build_parser()
        args = parser.parse_args(["verify", "--self-test", "--json"])
        exit_code = cli.cmd_verify(args)
        assert exit_code == 0
        captured = capsys.readouterr()
        data = json.loads(captured.out)
        assert data["all_passed"] is True

    def test_phase_filter(self, capsys):
        parser = cli.build_parser()
        args = parser.parse_args(["verify", "--self-test", "--phase", "3", "--json"])
        exit_code = cli.cmd_verify(args)
        assert exit_code == 0
        data = json.loads(capsys.readouterr().out)
        assert len(data["phases"]) == 1
        assert data["phases"][0]["phase"] == 3
