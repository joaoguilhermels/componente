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
import sys
import textwrap
from pathlib import Path
from unittest.mock import patch, MagicMock

import pytest

# Import the module under test
sys_path_entry = str(Path(__file__).resolve().parent)
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
    @patch("migration_cli._check_git_available")
    @patch("subprocess.run")
    def test_clone_when_no_existing_dir(self, mock_run, mock_git_check, tmp_path):
        dest = tmp_path / "new-repo"
        result = cli._clone_or_pull("https://github.com/org/repo.git", dest)

        mock_git_check.assert_called_once()
        mock_run.assert_called_once_with(
            ["git", "clone", "https://github.com/org/repo.git", str(dest)],
            check=True, timeout=300,
        )
        assert result == dest

    @patch("migration_cli._check_git_available")
    @patch("subprocess.run")
    def test_pull_when_existing_git_dir(self, mock_run, mock_git_check, tmp_path):
        dest = tmp_path / "existing-repo"
        dest.mkdir()
        (dest / ".git").mkdir()

        result = cli._clone_or_pull("https://github.com/org/repo.git", dest)

        mock_git_check.assert_called_once()
        mock_run.assert_called_once_with(
            ["git", "-C", str(dest), "pull", "--ff-only"],
            check=False, timeout=300,
        )
        assert result == dest


# ─── Git Availability Tests ───────────────────────────────────────────────

class TestCheckGitAvailable:
    @patch("subprocess.run")
    def test_git_available_does_not_exit(self, mock_run):
        mock_run.return_value = MagicMock(returncode=0)
        cli._check_git_available()  # should not raise

    @patch("subprocess.run", side_effect=FileNotFoundError)
    def test_git_not_available_exits(self, mock_run):
        with pytest.raises(SystemExit) as exc_info:
            cli._check_git_available()
        assert exc_info.value.code == 1


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

    def test_base_package_narrows_java_main(self, tmp_path):
        """Fix #3/#7: base_package should narrow java_main path for external repos."""
        state = cli.MigrationState(
            service_name="BillingService", base_package="com.acme.billing",
            db_prefix="", property_prefix="", tier="Standard",
            has_frontend=False, current_phase=0, phase_times={}, errors_encountered=[],
        )
        detector = cli.RepoDetector(tmp_path, state)
        assert str(detector.java_main).endswith(os.path.join("com", "acme", "billing"))
        assert str(detector.java_test).endswith(os.path.join("com", "acme", "billing"))

    def test_empty_base_package_uses_full_tree(self, tmp_path):
        """Without base_package, java_main is src/main/java/ (full tree)."""
        state = cli.MigrationState(
            service_name="Svc", base_package="", db_prefix="", property_prefix="",
            tier="Standard", has_frontend=False, current_phase=0,
            phase_times={}, errors_encountered=[],
        )
        detector = cli.RepoDetector(tmp_path, state)
        assert str(detector.java_main).endswith(os.path.join("src", "main", "java"))


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
        assert "2 port interface(s)" in result.detail

    def test_domain_test_coverage(self, reference_checker):
        result = reference_checker.check_domain_test_coverage()
        assert result.passed
        # Format: "N file(s), M @Test method(s): ..."
        assert "file(s)" in result.detail
        assert "@Test method(s)" in result.detail
        file_count = int(result.detail.split()[0])
        assert file_count >= 1

    def test_bridge_configs_all_present(self, reference_checker):
        result = reference_checker.check_bridge_configs()
        assert result.passed
        assert "persistence/" in result.detail
        assert "rest/" in result.detail
        assert "events/" in result.detail

    def test_integration_tests_found(self, reference_checker):
        result = reference_checker.check_adapter_integration_tests()
        assert result.passed
        assert "3 adapter(s)" in result.detail

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
        # Use >= to avoid breaking when new auto-configs are added
        count = int(result.detail.split()[0])
        assert count >= 5, f"Expected >= 5 auto-config(s), got {count}"

    def test_context_runner_tests(self, reference_checker):
        result = reference_checker.check_context_runner_tests()
        assert result.passed
        count = int(result.detail.split()[0])
        assert count >= 4, f"Expected >= 4 context runner test(s), got {count}"

    def test_angular_standalone_onpush(self, reference_checker):
        result = reference_checker.check_angular_standalone_onpush()
        assert result.passed
        # Verify all components match (X/X pattern) without hardcoding the count
        import re
        standalone_match = re.search(r"(\d+)/(\d+) standalone", result.detail)
        onpush_match = re.search(r"(\d+)/(\d+) OnPush", result.detail)
        assert standalone_match and standalone_match.group(1) == standalone_match.group(2), \
            f"Not all components standalone: {result.detail}"
        assert onpush_match and onpush_match.group(1) == onpush_match.group(2), \
            f"Not all components OnPush: {result.detail}"


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

    def test_static_import_forbidden_detected(self, tmp_repo):
        """Fix #4: import static of forbidden packages must be caught."""
        tmp_path, java_root, _, _ = tmp_repo
        core_class = java_root / "core" / "StaticImportService.java"
        core_class.write_text(textwrap.dedent("""\
            package com.example.svc.core;

            import static org.springframework.boot.test.Foo.bar;

            public class StaticImportService {}
        """))

        detector = cli.RepoDetector(tmp_path)
        checker = cli.ScorecardChecker(detector)
        result = checker.check_core_isolation()
        assert not result.passed
        assert "org.springframework.boot" in result.detail

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

    def test_yaml_extension_ci_pipeline_detected(self, tmp_path):
        """H4: CI pipeline files with .yaml extension should be detected."""
        workflows = tmp_path / ".github" / "workflows"
        workflows.mkdir(parents=True)
        (workflows / "build.yaml").write_text("name: Build")

        detector = cli.RepoDetector(tmp_path)
        checker = cli.ScorecardChecker(detector)
        result = checker.check_ci_pipeline()
        assert result.passed
        assert "build.yaml" in result.detail

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

    def test_match_if_missing_in_comment_not_flagged(self, tmp_repo):
        """Fix #6: matchIfMissing = true inside comments should be ignored."""
        tmp_path, java_root, _, _ = tmp_repo
        good_config = java_root / "autoconfigure" / "CommentConfig.java"
        good_config.write_text(textwrap.dedent("""\
            // Note: matchIfMissing = true is forbidden
            /* Do not use matchIfMissing = true */
            @ConditionalOnProperty(name = "foo")
            public class CommentConfig {}
        """))

        detector = cli.RepoDetector(tmp_path)
        checker = cli.ScorecardChecker(detector)
        result = checker.check_feature_flags_default()
        assert result.passed

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

    def test_standalone_with_spaces_detected(self, tmp_path):
        """Fix #5: standalone : true (with extra spaces) should be detected."""
        fe_dir = tmp_path / "frontend" / "src"
        fe_dir.mkdir(parents=True)
        comp = fe_dir / "my.component.ts"
        comp.write_text(textwrap.dedent("""\
            @Component({
              standalone :  true,
              changeDetection: ChangeDetectionStrategy.OnPush,
            })
            export class MyComponent {}
        """))

        detector = cli.RepoDetector(tmp_path)
        checker = cli.ScorecardChecker(detector)
        result = checker.check_angular_standalone_onpush()
        assert result.passed
        assert "1/1 standalone" in result.detail


# ─── Comment Detection Tests ─────────────────────────────────────────────────

class TestCommentDetection:
    """Verify _compute_comment_lines detects //, /*, and Javadoc comments in O(n)."""

    def test_line_comment_detected(self):
        lines = ["// @Bean", "public class Foo {}"]
        comment_lines = cli.ScorecardChecker._compute_comment_lines(lines)
        assert 0 in comment_lines
        assert 1 not in comment_lines

    def test_javadoc_continuation_detected(self):
        lines = ["/**", " * @Bean annotation", " */"]
        comment_lines = cli.ScorecardChecker._compute_comment_lines(lines)
        assert 0 in comment_lines
        assert 1 in comment_lines
        assert 2 in comment_lines

    def test_regular_code_not_comment(self):
        lines = ["@Bean", "public Foo foo() {}"]
        comment_lines = cli.ScorecardChecker._compute_comment_lines(lines)
        assert 0 not in comment_lines

    def test_block_comment_detected(self):
        lines = ["/*", "@Bean", "*/"]
        comment_lines = cli.ScorecardChecker._compute_comment_lines(lines)
        assert 1 in comment_lines

    def test_after_block_comment_not_detected(self):
        lines = ["/* comment */", "@Bean"]
        comment_lines = cli.ScorecardChecker._compute_comment_lines(lines)
        assert 1 not in comment_lines

    def test_single_line_block_comment(self):
        lines = ["/* @Bean */", "@Bean"]
        comment_lines = cli.ScorecardChecker._compute_comment_lines(lines)
        assert 0 in comment_lines
        assert 1 not in comment_lines

    def test_inline_block_comment_preserves_code(self):
        """C1: @Bean /* comment */ should NOT be marked as a comment line."""
        lines = ["@Bean /* overridable */", "public Foo foo() {}"]
        comment_lines = cli.ScorecardChecker._compute_comment_lines(lines)
        assert 0 not in comment_lines, "@Bean with inline comment should not be a comment line"
        assert 1 not in comment_lines

    def test_inline_block_comment_starts_multiline(self):
        """Mid-line /* without */ starts a block — next lines are comments."""
        lines = ["@Bean /* start", " * continued", " */", "public Foo foo() {}"]
        comment_lines = cli.ScorecardChecker._compute_comment_lines(lines)
        assert 0 not in comment_lines, "Line with code before /* should not be comment"
        assert 1 in comment_lines
        assert 2 in comment_lines
        assert 3 not in comment_lines

    def test_string_literal_with_slash_star_not_comment(self):
        """H2: /* inside a string literal should NOT trigger block comment mode."""
        lines = ['String s = "/* not a comment */";', "@Bean"]
        comment_lines = cli.ScorecardChecker._compute_comment_lines(lines)
        assert 0 not in comment_lines, "String literal containing /* should not be a comment"
        assert 1 not in comment_lines, "@Bean after string literal /* should not be a comment"

    def test_string_literal_with_unclosed_slash_star_not_comment(self):
        """H2: String s = "starts /*"; should NOT start a block comment."""
        lines = ['String s = "starts /*";', "@Bean", "public Foo foo() {}"]
        comment_lines = cli.ScorecardChecker._compute_comment_lines(lines)
        assert 0 not in comment_lines, "String literal with /* should not be a comment"
        assert 1 not in comment_lines, "@Bean after string /* should not be marked as comment"
        assert 2 not in comment_lines

    def test_real_midline_comment_after_code_and_string(self):
        """H2: Code with string then real /* comment should still work."""
        lines = ['System.out.println("hello"); /* real comment */', "@Bean"]
        comment_lines = cli.ScorecardChecker._compute_comment_lines(lines)
        # Line 0 has real code before /*, so not marked as comment (existing behavior)
        assert 0 not in comment_lines
        # /* and */ on same line, so block ends — @Bean is not a comment
        assert 1 not in comment_lines


# ─── COMB Per-Method Pairing Tests ──────────────────────────────────────────

class TestEffectiveDistance:
    """H3: Direct unit tests for _effective_distance boundary cases."""

    def test_adjacent_lines_distance_1(self):
        """Adjacent lines (no gap) should have effective distance 1."""
        lines = ["@ConditionalOnMissingBean", "@Bean"]
        comment_lines = set()
        dist = cli.ScorecardChecker._effective_distance(0, 1, lines, comment_lines)
        assert dist == 1

    def test_all_comments_between_distance_1(self):
        """Only comment lines between → effective distance 1 (comments are skipped)."""
        lines = ["@ConditionalOnMissingBean", "// a comment", "/* block */", "@Bean"]
        comment_lines = {1, 2}
        dist = cli.ScorecardChecker._effective_distance(0, 3, lines, comment_lines)
        assert dist == 1

    def test_all_blanks_between_distance_1(self):
        """Only blank lines between → effective distance 1 (blanks are skipped)."""
        lines = ["@ConditionalOnMissingBean", "", "  ", "@Bean"]
        comment_lines = set()
        dist = cli.ScorecardChecker._effective_distance(0, 3, lines, comment_lines)
        assert dist == 1

    def test_mixed_code_between(self):
        """Code lines between → counted in effective distance."""
        lines = ["@ConditionalOnMissingBean", "@Deprecated", "// comment", "@Override", "@Bean"]
        comment_lines = {2}
        # Between indices 0 and 4: lines 1 (@Deprecated) and 3 (@Override) are code → count=2, distance=3
        dist = cli.ScorecardChecker._effective_distance(0, 4, lines, comment_lines)
        assert dist == 3

    def test_reverse_order_same_result(self):
        """Distance is symmetric — order of a and b shouldn't matter."""
        lines = ["@ConditionalOnMissingBean", "@Bean"]
        comment_lines = set()
        assert cli.ScorecardChecker._effective_distance(1, 0, lines, comment_lines) == 1

    def test_same_line_distance_1(self):
        """Same line (a == b) → distance 1 (lo == hi, loop doesn't execute, returns 0+1)."""
        lines = ["@ConditionalOnMissingBean"]
        comment_lines = set()
        dist = cli.ScorecardChecker._effective_distance(0, 0, lines, comment_lines)
        assert dist == 1


class TestCombPerMethodPairing:
    """Fix #2: @ConditionalOnMissingBean must be paired per @Bean method."""

    def test_uneven_distribution_detected(self, tmp_repo):
        """File A has 2 @Bean with 1 COMB, File B has 1 @Bean with 2 COMB. Global sum 3==3 but A is missing one."""
        tmp_path, java_root, _, _ = tmp_repo
        # File A: 2 beans, only 1 COMB
        file_a = java_root / "autoconfigure" / "CoreAutoConfiguration.java"
        file_a.write_text(textwrap.dedent("""\
            public class CoreAutoConfiguration {
                @Bean
                @ConditionalOnMissingBean
                public Foo foo() { return new Foo(); }

                @Bean
                public Bar bar() { return new Bar(); }
            }
        """))
        # File B: 1 bean, but 2 COMB annotations (one above @Bean, one stray)
        file_b = java_root / "rest" / "RestConfiguration.java"
        file_b.write_text(textwrap.dedent("""\
            public class RestConfiguration {
                @ConditionalOnMissingBean
                @Bean
                public Baz baz() { return new Baz(); }
            }
        """))

        detector = cli.RepoDetector(tmp_path)
        checker = cli.ScorecardChecker(detector)
        result = checker.check_conditional_on_missing_bean()
        # Should FAIL: bar() in File A lacks COMB
        assert not result.passed
        assert "2/3" in result.detail  # 2 out of 3 beans have COMB

    def test_comb_after_bean_still_detected(self, tmp_repo):
        """@Bean followed by @ConditionalOnMissingBean (reference repo pattern)."""
        tmp_path, java_root, _, _ = tmp_repo
        config = java_root / "autoconfigure" / "CoreAutoConfiguration.java"
        config.write_text(textwrap.dedent("""\
            public class CoreAutoConfiguration {
                @Bean
                @ConditionalOnMissingBean
                public Foo foo() { return new Foo(); }
            }
        """))

        detector = cli.RepoDetector(tmp_path)
        checker = cli.ScorecardChecker(detector)
        result = checker.check_conditional_on_missing_bean()
        assert result.passed

    def test_comb_before_bean_still_detected(self, tmp_repo):
        """@ConditionalOnMissingBean before @Bean also valid."""
        tmp_path, java_root, _, _ = tmp_repo
        config = java_root / "persistence" / "PersistenceConfiguration.java"
        config.write_text(textwrap.dedent("""\
            public class PersistenceConfiguration {
                @ConditionalOnMissingBean
                @Bean
                public Repo repo() { return new Repo(); }
            }
        """))

        detector = cli.RepoDetector(tmp_path)
        checker = cli.ScorecardChecker(detector)
        result = checker.check_conditional_on_missing_bean()
        assert result.passed

    def test_comb_with_javadoc_between_still_paired(self, tmp_repo):
        """@ConditionalOnMissingBean with Javadoc between it and @Bean should still pair (±5 range)."""
        tmp_path, java_root, _, _ = tmp_repo
        config = java_root / "autoconfigure" / "CoreAutoConfiguration.java"
        config.write_text(textwrap.dedent("""\
            public class CoreAutoConfiguration {
                @ConditionalOnMissingBean
                /**
                 * Creates the customer service.
                 */
                @Bean
                public Foo foo() { return new Foo(); }
            }
        """))

        detector = cli.RepoDetector(tmp_path)
        checker = cli.ScorecardChecker(detector)
        result = checker.check_conditional_on_missing_bean()
        assert result.passed

    def test_comb_with_7_line_javadoc_still_paired(self, tmp_repo):
        """C1: 7-line Javadoc between COMB and Bean should PASS (effective distance is 0)."""
        tmp_path, java_root, _, _ = tmp_repo
        config = java_root / "autoconfigure" / "CoreAutoConfiguration.java"
        config.write_text(textwrap.dedent("""\
            public class CoreAutoConfiguration {
                @ConditionalOnMissingBean
                /**
                 * Creates the customer service.
                 * This is a longer Javadoc block that
                 * spans multiple lines to describe
                 * the purpose of this bean factory
                 * method in great detail.
                 */
                @Bean
                public Foo foo() { return new Foo(); }
            }
        """))

        detector = cli.RepoDetector(tmp_path)
        checker = cli.ScorecardChecker(detector)
        result = checker.check_conditional_on_missing_bean()
        assert result.passed, f"7-line Javadoc between COMB and Bean should not break pairing: {result.detail}"

    def test_comb_boundary_6_non_comment_lines_fails(self, tmp_repo):
        """C1 boundary: 6 non-comment, non-blank lines between COMB and Bean should FAIL."""
        tmp_path, java_root, _, _ = tmp_repo
        config = java_root / "autoconfigure" / "CoreAutoConfiguration.java"
        config.write_text(textwrap.dedent("""\
            public class CoreAutoConfiguration {
                @ConditionalOnMissingBean
                @SuppressWarnings("unused")
                @Deprecated
                @Override
                @SafeVarargs
                @FunctionalInterface
                @SomeOtherAnnotation
                @Bean
                public Foo foo() { return new Foo(); }
            }
        """))

        detector = cli.RepoDetector(tmp_path)
        checker = cli.ScorecardChecker(detector)
        result = checker.check_conditional_on_missing_bean()
        # 6 non-comment lines between COMB and Bean → effective distance = 7 > 5 → FAIL
        assert not result.passed, f"6 non-comment lines between COMB and Bean should fail: {result.detail}"

    def test_comb_mixed_comments_blanks_code_passes(self, tmp_repo):
        """H3: Mix of blank lines, Javadoc, and annotations between COMB and Bean."""
        tmp_path, java_root, _, _ = tmp_repo
        config = java_root / "autoconfigure" / "CoreAutoConfiguration.java"
        config.write_text(textwrap.dedent("""\
            public class CoreAutoConfiguration {
                @ConditionalOnMissingBean

                /** Creates the bean. */
                @Deprecated
                @Bean
                public Foo foo() { return new Foo(); }
            }
        """))

        detector = cli.RepoDetector(tmp_path)
        checker = cli.ScorecardChecker(detector)
        result = checker.check_conditional_on_missing_bean()
        # 1 blank + 3 Javadoc lines + 1 code line → effective distance = 2 (only @Deprecated counts)
        assert result.passed, f"Mixed comments/blanks/code should pass: {result.detail}"

    def test_comb_boundary_4_non_comment_lines_passes(self, tmp_repo):
        """C1 boundary: 4 non-comment, non-blank lines between COMB and Bean should PASS."""
        tmp_path, java_root, _, _ = tmp_repo
        config = java_root / "autoconfigure" / "CoreAutoConfiguration.java"
        config.write_text(textwrap.dedent("""\
            public class CoreAutoConfiguration {
                @ConditionalOnMissingBean
                @SuppressWarnings("unused")
                @Deprecated
                @Override
                @SafeVarargs
                @Bean
                public Foo foo() { return new Foo(); }
            }
        """))

        detector = cli.RepoDetector(tmp_path)
        checker = cli.ScorecardChecker(detector)
        result = checker.check_conditional_on_missing_bean()
        # 4 non-comment lines between → effective distance = 5 → PASS
        assert result.passed, f"4 non-comment lines between COMB and Bean should pass: {result.detail}"


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

    def test_load_rejects_non_mapping(self, tmp_path):
        """State file with non-dict content should raise ValueError."""
        mgr = cli.StateManager(tmp_path)
        mgr.state_dir.mkdir(parents=True, exist_ok=True)
        mgr.state_file.write_text("just a string\n")
        with pytest.raises(ValueError, match="expected YAML mapping"):
            mgr.load()

    def test_load_rejects_missing_service_name(self, tmp_path):
        """State file without service_name should raise ValueError."""
        import yaml
        mgr = cli.StateManager(tmp_path)
        mgr.state_dir.mkdir(parents=True, exist_ok=True)
        mgr.state_file.write_text(yaml.dump({"tier": "Standard"}))
        with pytest.raises(ValueError, match="service_name"):
            mgr.load()


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
        assert data["all_automated_passed"] is True
        assert data["overall_score"] == 100.0
        assert len(data["phases"]) == 5

    def test_json_contains_all_dimensions(self, reference_checker):
        result = reference_checker.run_all()
        data = json.loads(cli.JsonFormatter.format_result(result))
        all_checks = [c for p in data["phases"] for c in p["checks"]]
        assert len(all_checks) == 13

    def test_json_metadata_fields(self, reference_checker):
        """L1: JSON output includes CLI version, repo path, git hash."""
        result = reference_checker.run_all()
        data = json.loads(cli.JsonFormatter.format_result(result))
        assert data["cli_version"] == cli.CLI_VERSION
        assert data["repo_path"] == str(cli.REPO_ROOT)
        assert "git_hash" in data  # may be None in CI
        assert data["automated_passing"] == 13
        assert data["automated_total"] == 13
        assert data["manual_dimensions"] == 10


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

    def test_verify_quiet_flag(self):
        parser = cli.build_parser()
        args = parser.parse_args(["verify", "--quiet"])
        assert args.quiet is True

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
        assert data["all_automated_passed"] is True

    def test_phase_filter(self, capsys):
        parser = cli.build_parser()
        args = parser.parse_args(["verify", "--self-test", "--phase", "3", "--json"])
        exit_code = cli.cmd_verify(args)
        assert exit_code == 0
        data = json.loads(capsys.readouterr().out)
        assert len(data["phases"]) == 1
        assert data["phases"][0]["phase"] == 3

    def test_quiet_output(self, capsys):
        """Fix #12: --quiet outputs a single CI-friendly line."""
        parser = cli.build_parser()
        args = parser.parse_args(["verify", "--self-test", "--quiet"])
        exit_code = cli.cmd_verify(args)
        assert exit_code == 0
        output = capsys.readouterr().out.strip()
        assert output == "PASS 13/13 100%"

    def test_invalid_phase_returns_error(self, capsys):
        """M5: verify --phase 99 should return error code 1 with message."""
        parser = cli.build_parser()
        args = parser.parse_args(["verify", "--self-test", "--phase", "99"])
        exit_code = cli.cmd_verify(args)
        assert exit_code == 1
        captured = capsys.readouterr()
        assert "Invalid phase" in captured.err


# ─── End-to-End: cmd_status ──────────────────────────────────────────────────

class TestCmdStatus:
    def test_status_self_test_returns_zero(self):
        parser = cli.build_parser()
        args = parser.parse_args(["status"])
        exit_code = cli.cmd_status(args)
        assert exit_code == 0

    def test_status_json_output(self, capsys):
        parser = cli.build_parser()
        args = parser.parse_args(["status", "--json"])
        exit_code = cli.cmd_status(args)
        assert exit_code == 0
        data = json.loads(capsys.readouterr().out)
        assert data["all_automated_passed"] is True

    def test_status_quiet_output(self, capsys):
        parser = cli.build_parser()
        args = parser.parse_args(["status", "--quiet"])
        exit_code = cli.cmd_status(args)
        assert exit_code == 0
        output = capsys.readouterr().out.strip()
        assert "PASS" in output
        assert "13/13" in output

    def test_status_no_state_on_external_fails(self, tmp_path):
        parser = cli.build_parser()
        args = parser.parse_args(["status", "--target", str(tmp_path)])
        exit_code = cli.cmd_status(args)
        assert exit_code == 1


# ─── End-to-End: cmd_guide ───────────────────────────────────────────────────

class TestCmdGuide:
    def test_guide_self_test_runs_verify(self, capsys):
        """Guide on reference repo runs scorecard in self-test mode."""
        parser = cli.build_parser()
        args = parser.parse_args(["guide"])
        exit_code = cli.cmd_guide(args)
        assert exit_code == 0
        output = capsys.readouterr().out
        assert "SCORECARD" in output or "self-test" in output.lower()

    def test_guide_no_state_on_external_fails(self, tmp_path):
        parser = cli.build_parser()
        args = parser.parse_args(["guide", "--target", str(tmp_path)])
        with pytest.raises(SystemExit):
            cli.cmd_guide(args)

    def test_guide_with_state_phase_zero(self, tmp_path, capsys):
        """Guide with initialized state at phase 0 shows setup info."""
        state_mgr = cli.StateManager(tmp_path)
        state_mgr.create(service_name="TestSvc", tier="Standard")

        parser = cli.build_parser()
        args = parser.parse_args(["guide", "--target", str(tmp_path)])
        exit_code = cli.cmd_guide(args)
        assert exit_code == 0
        output = capsys.readouterr().out
        assert "Phase 0" in output
        assert "TestSvc" in output

    def test_guide_skip_gate_records_in_state(self, tmp_path, capsys):
        """--skip-gate should record that the gate was bypassed (audit trail)."""
        state_mgr = cli.StateManager(tmp_path)
        state_mgr.create(service_name="SkipTest", tier="Standard")

        parser = cli.build_parser()
        args = parser.parse_args(["guide", "--target", str(tmp_path), "--phase", "1", "--skip-gate"])
        exit_code = cli.cmd_guide(args)
        assert exit_code == 0

        state = state_mgr.load()
        assert "1" in state.phase_times
        assert state.phase_times["1"].get("gate_skipped") is True
        assert "gate_skipped_at" in state.phase_times["1"]


# ─── End-to-End: cmd_init ────────────────────────────────────────────────────

class TestCmdInit:
    def test_init_creates_state(self, tmp_path):
        parser = cli.build_parser()
        args = parser.parse_args([
            "init", "--service-name", "NewSvc", "--tier", "Standard",
            "--target", str(tmp_path),
        ])
        exit_code = cli.cmd_init(args)
        assert exit_code == 0
        assert (tmp_path / ".migration" / "state.yml").is_file()

    def test_init_rejects_duplicate(self, tmp_path):
        state_mgr = cli.StateManager(tmp_path)
        state_mgr.create(service_name="Existing", tier="Simple")

        parser = cli.build_parser()
        args = parser.parse_args([
            "init", "--service-name", "Dup", "--tier", "Standard",
            "--target", str(tmp_path),
        ])
        exit_code = cli.cmd_init(args)
        assert exit_code == 1


# ─── End-to-End: cmd_record ──────────────────────────────────────────────────

class TestCmdRecord:
    def test_record_self_test_creates_entry(self, tmp_path):
        poc_file = tmp_path / "poc-data.yml"
        parser = cli.build_parser()
        args = parser.parse_args([
            "record", "--service-name", "RefRepo", "--poc-data", str(poc_file),
        ])
        exit_code = cli.cmd_record(args)
        assert exit_code == 0
        assert poc_file.is_file()

        import yaml
        with open(poc_file) as f:
            data = yaml.safe_load(f)
        assert data["services"][0]["name"] == "RefRepo"
        assert data["services"][0]["attempts"][0]["attempt"] == 1

    def test_record_dry_run(self, tmp_path, capsys):
        poc_file = tmp_path / "poc-data.yml"
        parser = cli.build_parser()
        args = parser.parse_args([
            "record", "--service-name", "DryRun", "--poc-data", str(poc_file), "--dry-run",
        ])
        exit_code = cli.cmd_record(args)
        assert exit_code == 0
        assert not poc_file.exists()

    def test_record_timing_estimated_key(self, tmp_path, reference_checker):
        """M2: timing key renamed to timing_estimated, now starts empty (populated later)."""
        poc_file = tmp_path / "poc-data.yml"
        recorder = cli.ResultsRecorder(poc_file)
        result = reference_checker.run_all()

        attempt = recorder.record("TestService", result, dry_run=True)

        assert "timing_estimated" in attempt
        assert attempt["timing_estimated"] == {}  # empty until populated manually
        assert "timing" not in attempt  # old key must be gone


# ─── Semantic Check Tests (C1) ───────────────────────────────────────────────

class TestPackageDirCache:
    """M4: Verify _find_package_dir cache avoids repeated rglob calls."""

    def test_cache_returns_same_result_on_second_call(self, tmp_repo):
        tmp_path, java_root, _, _ = tmp_repo
        detector = cli.RepoDetector(tmp_path)
        checker = cli.ScorecardChecker(detector)
        first = checker._find_package_dir("core")
        second = checker._find_package_dir("core")
        assert first is second  # exact same object from cache
        assert "core" in checker._pkg_cache

    def test_cache_stores_none_for_missing_package(self, tmp_path):
        java_root = tmp_path / "src" / "main" / "java"
        java_root.mkdir(parents=True)
        detector = cli.RepoDetector(tmp_path)
        checker = cli.ScorecardChecker(detector)
        result = checker._find_package_dir("nonexistent")
        assert result is None
        assert "nonexistent" in checker._pkg_cache
        # Second call should return cached None without rglob
        result2 = checker._find_package_dir("nonexistent")
        assert result2 is None


class TestFindPackageDirNonExistent:
    """M1: _find_package_dir should not crash on non-existent java_main path."""

    def test_nonexistent_java_main_returns_none(self, tmp_path):
        """When java_main points to a non-existent directory, _find_package_dir returns None."""
        state = cli.MigrationState(
            service_name="Ghost", base_package="com.example.ghost",
            db_prefix="", property_prefix="", tier="Standard",
            has_frontend=False, current_phase=0, phase_times={}, errors_encountered=[],
        )
        # java_main will be tmp_path/src/main/java/com/example/ghost — which doesn't exist
        detector = cli.RepoDetector(tmp_path, state)
        assert not detector.java_main.exists()

        checker = cli.ScorecardChecker(detector)
        result = checker._find_package_dir("core")
        assert result is None  # should not raise

    def test_nonexistent_java_test_returns_none(self, tmp_path):
        """When java_test points to a non-existent directory, _find_test_package_dir returns None."""
        state = cli.MigrationState(
            service_name="Ghost", base_package="com.example.ghost",
            db_prefix="", property_prefix="", tier="Standard",
            has_frontend=False, current_phase=0, phase_times={}, errors_encountered=[],
        )
        detector = cli.RepoDetector(tmp_path, state)
        assert not detector.java_test.exists()

        checker = cli.ScorecardChecker(detector)
        result = checker._find_test_package_dir("core")
        assert result is None  # should not raise


class TestRunPhaseInvalidInput:
    """M2: run_phase() should raise ValueError on invalid phase numbers."""

    def test_invalid_phase_raises_value_error(self, tmp_repo):
        tmp_path, _, _, _ = tmp_repo
        detector = cli.RepoDetector(tmp_path)
        checker = cli.ScorecardChecker(detector)
        with pytest.raises(ValueError, match="Invalid phase"):
            checker.run_phase(99)

    def test_phase_zero_raises_value_error(self, tmp_repo):
        tmp_path, _, _, _ = tmp_repo
        detector = cli.RepoDetector(tmp_path)
        checker = cli.ScorecardChecker(detector)
        with pytest.raises(ValueError, match="Invalid phase"):
            checker.run_phase(0)

    def test_negative_phase_raises_value_error(self, tmp_repo):
        tmp_path, _, _, _ = tmp_repo
        detector = cli.RepoDetector(tmp_path)
        checker = cli.ScorecardChecker(detector)
        with pytest.raises(ValueError, match="Invalid phase"):
            checker.run_phase(-1)


class TestSemanticPortChecks:
    """C1: Port interface check verifies 'interface' keyword."""

    def test_class_in_port_dir_fails(self, tmp_repo):
        """A class (not interface) in port/ should not count as a port interface."""
        tmp_path, java_root, _, _ = tmp_repo
        port_dir = java_root / "core" / "port"
        port_dir.mkdir(parents=True, exist_ok=True)
        bad_port = port_dir / "BadPort.java"
        bad_port.write_text(textwrap.dedent("""\
            package com.example.svc.core.port;
            public class BadPort {}
        """))

        detector = cli.RepoDetector(tmp_path)
        checker = cli.ScorecardChecker(detector)
        result = checker.check_port_interfaces()
        assert not result.passed
        assert "No port interfaces found" in result.detail

    def test_interface_keyword_in_block_comment_not_matched(self, tmp_repo):
        """H1: 'interface' in a block comment body should not produce a false positive."""
        tmp_path, java_root, _, _ = tmp_repo
        port_dir = java_root / "core" / "port"
        port_dir.mkdir(parents=True, exist_ok=True)
        bad_port = port_dir / "NotAPort.java"
        bad_port.write_text(textwrap.dedent("""\
            package com.example.svc.core.port;
            /*
            The interface contract for persistence
            is described in this block comment.
            */
            public class NotAPort {}
        """))

        detector = cli.RepoDetector(tmp_path)
        checker = cli.ScorecardChecker(detector)
        result = checker.check_port_interfaces()
        assert not result.passed, f"'interface' in block comment should not match: {result.detail}"

    def test_interface_in_port_dir_passes(self, tmp_repo):
        """A proper Java interface in port/ should pass."""
        tmp_path, java_root, _, _ = tmp_repo
        port_dir = java_root / "core" / "port"
        port_dir.mkdir(parents=True, exist_ok=True)
        good_port = port_dir / "GoodPort.java"
        good_port.write_text(textwrap.dedent("""\
            package com.example.svc.core.port;
            public interface GoodPort {
                void doSomething();
            }
        """))

        detector = cli.RepoDetector(tmp_path)
        checker = cli.ScorecardChecker(detector)
        result = checker.check_port_interfaces()
        assert result.passed
        assert "1 port interface(s)" in result.detail


class TestPortCrossReference:
    """H3: Port interfaces should warn when no adapter implements them."""

    def test_port_with_adapter_no_warning(self, tmp_repo):
        """Port interface that has an implementing adapter → no WARNING."""
        tmp_path, java_root, _, _ = tmp_repo
        port_dir = java_root / "core" / "port"
        port_dir.mkdir(parents=True, exist_ok=True)
        port_dir.joinpath("FooRepository.java").write_text(textwrap.dedent("""\
            package com.example.svc.core.port;
            public interface FooRepository {
                void save();
            }
        """))
        # Adapter implementing the port
        adapter = java_root / "persistence" / "FooPersistenceAdapter.java"
        adapter.write_text(textwrap.dedent("""\
            package com.example.svc.persistence;
            class FooPersistenceAdapter implements FooRepository {
                public void save() {}
            }
        """))

        detector = cli.RepoDetector(tmp_path)
        checker = cli.ScorecardChecker(detector)
        result = checker.check_port_interfaces()
        assert result.passed
        assert "WARNING" not in result.detail

    def test_port_without_adapter_warns(self, tmp_repo):
        """Port interface with no implementing adapter → WARNING in detail."""
        tmp_path, java_root, _, _ = tmp_repo
        port_dir = java_root / "core" / "port"
        port_dir.mkdir(parents=True, exist_ok=True)
        port_dir.joinpath("OrphanPort.java").write_text(textwrap.dedent("""\
            package com.example.svc.core.port;
            public interface OrphanPort {
                void doSomething();
            }
        """))

        detector = cli.RepoDetector(tmp_path)
        checker = cli.ScorecardChecker(detector)
        result = checker.check_port_interfaces()
        assert result.passed  # still passes — WARNING, not FAIL
        assert "WARNING" in result.detail
        assert "OrphanPort" in result.detail

    def test_mixed_port_implementation_warns_orphan_only(self, tmp_repo):
        """M1: Two ports — one implemented, one orphan. Only the orphan is warned."""
        tmp_path, java_root, _, _ = tmp_repo
        port_dir = java_root / "core" / "port"
        port_dir.mkdir(parents=True, exist_ok=True)
        port_dir.joinpath("FooRepository.java").write_text(textwrap.dedent("""\
            package com.example.svc.core.port;
            public interface FooRepository { void save(); }
        """))
        port_dir.joinpath("BarPublisher.java").write_text(textwrap.dedent("""\
            package com.example.svc.core.port;
            public interface BarPublisher { void publish(); }
        """))
        # Only FooRepository has an adapter
        adapter = java_root / "persistence" / "FooPersistenceAdapter.java"
        adapter.write_text(textwrap.dedent("""\
            package com.example.svc.persistence;
            class FooPersistenceAdapter implements FooRepository {
                public void save() {}
            }
        """))

        detector = cli.RepoDetector(tmp_path)
        checker = cli.ScorecardChecker(detector)
        result = checker.check_port_interfaces()
        assert result.passed  # still passes — WARNING, not FAIL
        assert "WARNING" in result.detail
        assert "BarPublisher" in result.detail
        assert "FooRepository" not in result.detail.split("WARNING")[1]  # only orphan warned

    def test_reference_repo_ports_all_implemented(self, reference_checker):
        """Reference repo should have all ports implemented (no warnings)."""
        result = reference_checker.check_port_interfaces()
        assert result.passed
        assert "WARNING" not in result.detail


class TestSemanticBridgeConfigChecks:
    """C1: Bridge config check verifies @Bean methods exist."""

    def test_config_without_bean_fails(self, tmp_repo):
        """A *Configuration.java without any bean-producing methods should fail."""
        tmp_path, java_root, _, _ = tmp_repo
        for pkg in ["persistence", "rest", "events"]:
            cfg = java_root / pkg / f"{pkg.title()}Configuration.java"
            cfg.write_text(textwrap.dedent(f"""\
                public class {pkg.title()}Configuration {{
                    // empty config, no methods
                }}
            """))

        detector = cli.RepoDetector(tmp_path)
        checker = cli.ScorecardChecker(detector)
        result = checker.check_bridge_configs()
        assert not result.passed
        assert "No @Bean" in result.detail

    def test_config_with_bean_passes(self, tmp_repo):
        """A *Configuration.java with @Bean should pass."""
        tmp_path, java_root, _, _ = tmp_repo
        for pkg in ["persistence", "rest", "events"]:
            cfg = java_root / pkg / f"{pkg.title()}Configuration.java"
            cfg.write_text(textwrap.dedent(f"""\
                public class {pkg.title()}Configuration {{
                    @Bean
                    public Object {pkg}Bean() {{ return new Object(); }}
                }}
            """))

        detector = cli.RepoDetector(tmp_path)
        checker = cli.ScorecardChecker(detector)
        result = checker.check_bridge_configs()
        assert result.passed


class TestPerAdapterIntegrationTests:
    """H4: Adapter test check requires >= 1 test per present adapter module."""

    def test_all_adapters_covered_passes(self, tmp_repo):
        """All present adapters have test files → PASS."""
        tmp_path, java_root, test_root, _ = tmp_repo
        # Create adapter test dirs and tests
        for mod in ["persistence", "rest", "events"]:
            tdir = test_root / mod
            tdir.mkdir(parents=True, exist_ok=True)
            (tdir / f"{mod.title()}AdapterTest.java").write_text("// test")

        detector = cli.RepoDetector(tmp_path)
        checker = cli.ScorecardChecker(detector)
        result = checker.check_adapter_integration_tests()
        assert result.passed
        assert "3 adapter(s)" in result.detail

    def test_missing_adapter_test_fails(self, tmp_repo):
        """Persistence has tests but rest does not → FAIL naming rest."""
        tmp_path, java_root, test_root, _ = tmp_repo
        # persistence has test
        p_test = test_root / "persistence"
        p_test.mkdir(parents=True, exist_ok=True)
        (p_test / "PersistenceAdapterTest.java").write_text("// test")
        # rest has no test (but rest package exists in main)

        detector = cli.RepoDetector(tmp_path)
        checker = cli.ScorecardChecker(detector)
        result = checker.check_adapter_integration_tests()
        assert not result.passed
        assert "rest" in result.detail
        assert "events" in result.detail  # events also present but no test

    def test_single_adapter_with_test_passes(self, tmp_path):
        """M2: Repo with only persistence/ (no rest, no events) and a test → PASS."""
        java_root = tmp_path / "src" / "main" / "java" / "com" / "example" / "svc"
        test_root = tmp_path / "src" / "test" / "java" / "com" / "example" / "svc"
        (java_root / "persistence").mkdir(parents=True)
        p_test = test_root / "persistence"
        p_test.mkdir(parents=True)
        (p_test / "PersistenceAdapterTest.java").write_text("// test")

        detector = cli.RepoDetector(tmp_path)
        checker = cli.ScorecardChecker(detector)
        result = checker.check_adapter_integration_tests()
        assert result.passed
        assert "1 adapter(s)" in result.detail

    def test_no_adapters_fails(self, tmp_path):
        """No adapter modules at all → FAIL."""
        # bare repo with only src structure, no adapter packages
        java_root = tmp_path / "src" / "main" / "java"
        java_root.mkdir(parents=True)
        test_root = tmp_path / "src" / "test" / "java"
        test_root.mkdir(parents=True)

        detector = cli.RepoDetector(tmp_path)
        checker = cli.ScorecardChecker(detector)
        result = checker.check_adapter_integration_tests()
        assert not result.passed


class TestSemanticDomainTestChecks:
    """C1: Domain test coverage check verifies @Test annotations."""

    def test_test_file_without_test_annotation_fails(self, tmp_repo):
        """A *Test.java without any test method annotations should fail."""
        tmp_path, _, test_root, _ = tmp_repo
        core_test = test_root / "core"
        core_test.mkdir(parents=True, exist_ok=True)
        bad_test = core_test / "EmptyTest.java"
        bad_test.write_text(textwrap.dedent("""\
            package com.example.svc.core;
            public class EmptyTest {
                // intentionally empty
            }
        """))

        detector = cli.RepoDetector(tmp_path)
        checker = cli.ScorecardChecker(detector)
        result = checker.check_domain_test_coverage()
        assert not result.passed
        assert "No test files" in result.detail or "without @Test" in result.detail

    def test_test_file_with_insufficient_tests_fails(self, tmp_repo):
        """A *Test.java with only 1 @Test should fail (minimum 3 @Test methods required)."""
        tmp_path, _, test_root, _ = tmp_repo
        core_test = test_root / "core"
        core_test.mkdir(parents=True, exist_ok=True)
        single_test = core_test / "DomainModelTest.java"
        single_test.write_text(textwrap.dedent("""\
            package com.example.svc.core;
            import org.junit.jupiter.api.Test;
            class DomainModelTest {
                @Test
                void should_create_model() {}
            }
        """))

        detector = cli.RepoDetector(tmp_path)
        checker = cli.ScorecardChecker(detector)
        result = checker.check_domain_test_coverage()
        assert not result.passed
        assert "minimum 3 required" in result.detail

    def test_test_file_with_sufficient_tests_passes(self, tmp_repo):
        """A *Test.java with >= 3 @Test methods should pass."""
        tmp_path, _, test_root, _ = tmp_repo
        core_test = test_root / "core"
        core_test.mkdir(parents=True, exist_ok=True)
        good_test = core_test / "DomainModelTest.java"
        good_test.write_text(textwrap.dedent("""\
            package com.example.svc.core;
            import org.junit.jupiter.api.Test;
            class DomainModelTest {
                @Test void should_create_model() {}
                @Test void should_validate_fields() {}
                @Test void should_generate_uuid() {}
            }
        """))

        detector = cli.RepoDetector(tmp_path)
        checker = cli.ScorecardChecker(detector)
        result = checker.check_domain_test_coverage()
        assert result.passed
        assert "1 file(s)" in result.detail
        assert "3 @Test method(s)" in result.detail


# ─── Source Root Tests (H2) ──────────────────────────────────────────────────

class TestSourceRoot:
    """H2: --source-root narrows the module root for multi-module repos."""

    def test_source_root_changes_module_root(self, tmp_path):
        """RepoDetector with source_root should look under that sub-path."""
        sub = tmp_path / "billing-core" / "src" / "main" / "java"
        sub.mkdir(parents=True)
        detector = cli.RepoDetector(tmp_path, source_root="billing-core")
        assert str(detector.java_main).endswith(
            os.path.join("billing-core", "src", "main", "java")
        )

    def test_source_root_affects_resources(self, tmp_path):
        detector = cli.RepoDetector(tmp_path, source_root="billing-core")
        assert str(detector.resources).endswith(
            os.path.join("billing-core", "src", "main", "resources")
        )

    def test_source_root_arg_parsing(self):
        """Parser accepts --source-root on verify subcommand."""
        parser = cli.build_parser()
        args = parser.parse_args(["verify", "--source-root", "billing-core"])
        assert args.source_root == "billing-core"

    def test_source_root_arg_on_guide(self):
        parser = cli.build_parser()
        args = parser.parse_args(["guide", "--source-root", "my-module"])
        assert args.source_root == "my-module"


# ─── Version Flag Tests (L3) ─────────────────────────────────────────────────

class TestVersionFlag:
    """L3: CLI has --version flag."""

    def test_version_flag_exits_zero(self):
        parser = cli.build_parser()
        with pytest.raises(SystemExit) as exc_info:
            parser.parse_args(["--version"])
        assert exc_info.value.code == 0

    def test_cli_version_constant_exists(self):
        assert hasattr(cli, "CLI_VERSION")
        assert isinstance(cli.CLI_VERSION, str)
        assert "." in cli.CLI_VERSION


# ─── Weight Validation Tests (L2) ────────────────────────────────────────────

class TestWeightValidation:
    """L2: Dimension methods are validated against DIMENSIONS keys."""

    def test_all_dimensions_have_methods(self):
        """Every DIMENSIONS key must have a check_<key> method."""
        for dim_key in cli.DIMENSIONS:
            method_name = f"check_{dim_key}"
            assert hasattr(cli.ScorecardChecker, method_name), (
                f"Missing ScorecardChecker.{method_name}() for dimension '{dim_key}'"
            )

    def test_validate_runs_without_error(self):
        """_validate_dimension_methods should not raise."""
        cli._validate_dimension_methods()


# ─── Workspace Setup Check Tests (M5) ────────────────────────────────────────

class TestWorkspaceSetupCheck:
    """M5: Phase 0 workspace verification."""

    def test_missing_instructions_dir_reported(self, tmp_path):
        """Missing .github/instructions/ should be flagged."""
        # Create just pom.xml to avoid a double-issue
        (tmp_path / "pom.xml").touch()

        state = cli.MigrationState(
            service_name="Svc", base_package="", db_prefix="", property_prefix="",
            tier="Standard", has_frontend=False, current_phase=0,
            phase_times={}, errors_encountered=[],
        )
        detector = cli.RepoDetector(tmp_path, state)
        checker = cli.ScorecardChecker(detector)
        state_mgr = cli.StateManager(tmp_path)
        formatter = cli.TerminalFormatter()
        guide = cli.WorkflowGuide(detector, checker, state_mgr, formatter)
        issues = guide._check_workspace_setup()
        assert any(".github/instructions/" in i for i in issues)

    def test_missing_build_file_reported(self, tmp_path):
        """Missing pom.xml / build.gradle should be flagged."""
        (tmp_path / ".github" / "instructions").mkdir(parents=True)

        state = cli.MigrationState(
            service_name="Svc", base_package="", db_prefix="", property_prefix="",
            tier="Standard", has_frontend=False, current_phase=0,
            phase_times={}, errors_encountered=[],
        )
        detector = cli.RepoDetector(tmp_path, state)
        checker = cli.ScorecardChecker(detector)
        state_mgr = cli.StateManager(tmp_path)
        formatter = cli.TerminalFormatter()
        guide = cli.WorkflowGuide(detector, checker, state_mgr, formatter)
        issues = guide._check_workspace_setup()
        assert any("pom.xml" in i for i in issues)

    def test_all_present_no_issues(self, tmp_path):
        """With .github/instructions/ and pom.xml, no issues."""
        (tmp_path / ".github" / "instructions").mkdir(parents=True)
        (tmp_path / "pom.xml").touch()

        state = cli.MigrationState(
            service_name="Svc", base_package="", db_prefix="", property_prefix="",
            tier="Standard", has_frontend=False, current_phase=0,
            phase_times={}, errors_encountered=[],
        )
        detector = cli.RepoDetector(tmp_path, state)
        checker = cli.ScorecardChecker(detector)
        state_mgr = cli.StateManager(tmp_path)
        formatter = cli.TerminalFormatter()
        guide = cli.WorkflowGuide(detector, checker, state_mgr, formatter)
        issues = guide._check_workspace_setup()
        assert len(issues) == 0


# ─── Guide State Tracking Tests (H3) ────────────────────────────────────────

class TestGuideStateTracking:
    """H3: prompts_applied field in migration state."""

    def test_prompts_applied_defaults_to_empty(self, tmp_path):
        mgr = cli.StateManager(tmp_path)
        state = mgr.create(service_name="Svc", tier="Standard")
        loaded = mgr.load()
        assert loaded.prompts_applied == []

    def test_prompts_applied_roundtrips(self, tmp_path):
        mgr = cli.StateManager(tmp_path)
        state = mgr.create(service_name="Svc", tier="Standard")
        state.prompts_applied = ["Prompt 0.1: Workspace Setup", "Prompt 1.1: Scaffold"]
        mgr.save(state)
        loaded = mgr.load()
        assert loaded.prompts_applied == ["Prompt 0.1: Workspace Setup", "Prompt 1.1: Scaffold"]

    def test_migration_state_has_prompts_applied_field(self):
        state = cli.MigrationState(
            service_name="Svc", base_package="", db_prefix="", property_prefix="",
            tier="Standard", has_frontend=False, current_phase=0,
            phase_times={}, errors_encountered=[],
        )
        assert hasattr(state, "prompts_applied")
        assert state.prompts_applied == []


# ─── Automated vs Manual Dimensions (H4) ─────────────────────────────────────

class TestAutomatedVsManual:
    """H4: Output clearly distinguishes automated and manual dimensions."""

    def test_terminal_output_mentions_manual(self, reference_checker, capsys):
        result = reference_checker.run_all()
        cli.TerminalFormatter().print_result(result)
        output = capsys.readouterr().out
        assert "AUTOMATED SCORE" in output
        assert "10 manual" in output

    def test_json_output_has_manual_count(self, reference_checker):
        result = reference_checker.run_all()
        data = json.loads(cli.JsonFormatter.format_result(result))
        assert data["manual_dimensions"] == 10
        assert data["automated_total"] == 13
