#!/usr/bin/env python3
"""Tests for update_presentation.py.

Run with:
    python3 -m pytest scripts/test_update_presentation.py -v

Or via Docker:
    docker compose -f docker/docker-compose.yml run --rm --no-deps python-build \
        sh -c "pip install --quiet pyyaml==6.0.2 pytest && \
               python3 -m pytest scripts/test_update_presentation.py -v"
"""

from __future__ import annotations

import sys
from pathlib import Path

import pytest

# Import the module under test
sys_path_entry = str(Path(__file__).resolve().parent)
if sys_path_entry not in sys.path:
    sys.path.insert(0, sys_path_entry)

import update_presentation as up


# ─── Fixtures ───────────────────────────────────────────────────────────────


@pytest.fixture
def sample_data():
    """Reusable POC data with 2 services covering edge cases.

    Alpha: Standard tier, 2 attempts (progression), full scores on attempt 2.
    Beta: Simple tier, 1 attempt, null scores for later phases.
    """
    return {
        "services": [
            {
                "name": "Alpha",
                "tier": "Standard",
                "attempts": [
                    {
                        "attempt": 1,
                        "date": "2025-01-15",
                        "phase_reached": 3,
                        "scores": {
                            "phase_1": 100,
                            "phase_2": 80,
                            "phase_3": 60,
                            "phase_4": None,
                            "phase_5": None,
                        },
                        "timing": {
                            "total_hours": 10.0,
                            "phase_0_hours": 0.5,
                            "phase_1_hours": 2.0,
                            "phase_2_hours": 4.0,
                            "phase_3_hours": 3.5,
                        },
                        "quality": {
                            "first_try_correct_pct": 75,
                            "recovery_prompts_used": 4,
                            "archunit_violations": 2,
                        },
                        "errors": [
                            {
                                "violation": "Missing @ConditionalOnMissingBean",
                                "phase": "Phase 4",
                                "count": 2,
                                "corrected": True,
                                "recovery_prompt": "Add COMB annotations",
                            },
                        ],
                        "improvements": [
                            {
                                "artifact": "migration-guide.md",
                                "change": "Added bridge config section",
                                "impact": "Reduced adapter errors",
                            },
                        ],
                        "notes": "First attempt",
                    },
                    {
                        "attempt": 2,
                        "date": "2025-02-01",
                        "phase_reached": 5,
                        "scores": {
                            "phase_1": 100,
                            "phase_2": 100,
                            "phase_3": 100,
                            "phase_4": 90,
                            "phase_5": 80,
                        },
                        "timing": {
                            "total_hours": 8.0,
                            "phase_0_hours": 0.5,
                            "phase_1_hours": 1.5,
                            "phase_2_hours": 3.0,
                            "phase_3_hours": 2.0,
                            "phase_4_hours": 0.5,
                            "phase_5_hours": 0.5,
                        },
                        "quality": {
                            "first_try_correct_pct": 90,
                            "recovery_prompts_used": 1,
                            "archunit_violations": 0,
                        },
                        "errors": [],
                        "improvements": [],
                        "notes": "Second attempt, much improved",
                    },
                ],
            },
            {
                "name": "Beta",
                "tier": "Simple",
                "attempts": [
                    {
                        "attempt": 1,
                        "date": "2025-01-20",
                        "phase_reached": 2,
                        "scores": {
                            "phase_1": 100,
                            "phase_2": 70,
                            "phase_3": None,
                            "phase_4": None,
                            "phase_5": None,
                        },
                        "timing": {
                            "total_hours": 6.0,
                            "phase_0_hours": 0.5,
                            "phase_1_hours": 1.5,
                            "phase_2_hours": 4.0,
                        },
                        "quality": {
                            "first_try_correct_pct": 80,
                            "recovery_prompts_used": 2,
                            "archunit_violations": 1,
                        },
                        "errors": [
                            {
                                "violation": "Missing @ConditionalOnMissingBean",
                                "phase": "Phase 4",
                                "count": 1,
                                "corrected": False,
                                "recovery_prompt": "Add COMB annotations",
                            },
                            {
                                "violation": "Forbidden import in core",
                                "phase": "Phase 2",
                                "count": 3,
                                "corrected": True,
                                "recovery_prompt": "Remove jakarta.persistence from core",
                            },
                        ],
                        "improvements": [],
                        "notes": "Initial beta attempt",
                    },
                ],
            },
        ],
        "learnings": {
            "worked_well": [
                {"text": "TDD approach reduced rework", "pocs": ["Alpha", "Beta"]},
                {"text": "Bridge config pattern", "pocs": ["Alpha"]},
            ],
            "needs_improvement": [
                {"text": "Auto-config ordering", "pocs": ["Alpha"]},
            ],
        },
    }


@pytest.fixture
def empty_data():
    """Data with no services or learnings."""
    return {"services": [], "learnings": {"worked_well": [], "needs_improvement": []}}


# ─── TestLoadData ────────────────────────────────────────────────────────────


class TestLoadData:
    def test_valid_file(self, tmp_path):
        import yaml

        data = {"services": [{"name": "Svc"}], "learnings": {"worked_well": []}}
        yaml_file = tmp_path / "data.yml"
        yaml_file.write_text(yaml.dump(data))
        result = up.load_data(yaml_file)
        assert result["services"][0]["name"] == "Svc"

    def test_empty_file_returns_defaults(self, tmp_path):
        yaml_file = tmp_path / "empty.yml"
        yaml_file.write_text("")
        result = up.load_data(yaml_file)
        assert result == {"services": [], "learnings": {"worked_well": [], "needs_improvement": []}}

    def test_missing_learnings_adds_defaults(self, tmp_path):
        import yaml

        yaml_file = tmp_path / "no-learnings.yml"
        yaml_file.write_text(yaml.dump({"services": [{"name": "Svc"}]}))
        result = up.load_data(yaml_file)
        assert "learnings" in result
        assert result["learnings"]["worked_well"] == []
        assert result["learnings"]["needs_improvement"] == []


# ─── TestLatestAttempt ───────────────────────────────────────────────────────


class TestLatestAttempt:
    def test_returns_last_element(self, sample_data):
        svc = sample_data["services"][0]  # Alpha with 2 attempts
        result = up.latest_attempt(svc)
        assert result["attempt"] == 2

    def test_single_attempt(self, sample_data):
        svc = sample_data["services"][1]  # Beta with 1 attempt
        result = up.latest_attempt(svc)
        assert result["attempt"] == 1


# ─── TestWeightedScore ───────────────────────────────────────────────────────


class TestWeightedScore:
    def test_all_phases_present(self):
        scores = {"phase_1": 100, "phase_2": 100, "phase_3": 100, "phase_4": 100, "phase_5": 100}
        assert up.weighted_score(scores) == 100.0

    def test_some_null_phases(self):
        scores = {"phase_1": 100, "phase_2": 80, "phase_3": None, "phase_4": None, "phase_5": None}
        # Weights: phase_1=0.25, phase_2=0.35 -> total_weight=0.60
        # weighted_sum = 0.25*100 + 0.35*80 = 25 + 28 = 53
        # result = 53 / 0.60 = 88.33...
        result = up.weighted_score(scores)
        assert result == pytest.approx(88.33, abs=0.01)

    def test_all_null_returns_zero(self):
        scores = {"phase_1": None, "phase_2": None, "phase_3": None, "phase_4": None, "phase_5": None}
        assert up.weighted_score(scores) == 0.0

    def test_empty_scores_returns_zero(self):
        assert up.weighted_score({}) == 0.0

    def test_partial_keys(self):
        # Only phase_1 present
        scores = {"phase_1": 80}
        # weight 0.25, weighted_sum = 20, total = 0.25 -> 80.0
        assert up.weighted_score(scores) == 80.0


# ─── TestFmtScore ────────────────────────────────────────────────────────────


class TestFmtScore:
    def test_none_returns_na(self):
        assert up.fmt_score(None) == "N/A"

    def test_int_value(self):
        assert up.fmt_score(85) == "85%"

    def test_float_value(self):
        assert up.fmt_score(85.7) == "86%"

    def test_zero(self):
        assert up.fmt_score(0) == "0%"


# ─── TestFmtHours ────────────────────────────────────────────────────────────


class TestFmtHours:
    def test_none_returns_na(self):
        assert up.fmt_hours(None) == "N/A"

    def test_float_value(self):
        assert up.fmt_hours(12.5) == "12.5h"

    def test_integer_value(self):
        assert up.fmt_hours(3.0) == "3.0h"

    def test_zero(self):
        assert up.fmt_hours(0.0) == "0.0h"


# ─── TestProgressionNote ────────────────────────────────────────────────────


class TestProgressionNote:
    def test_single_attempt_returns_empty(self, sample_data):
        svc = sample_data["services"][1]  # Beta with 1 attempt
        assert up.progression_note(svc) == ""

    def test_multiple_attempts_returns_progression(self, sample_data):
        svc = sample_data["services"][0]  # Alpha with 2 attempts
        result = up.progression_note(svc)
        assert "tentativa 2" in result
        assert "%" in result
        assert "\u2192" in result  # Arrow character

    def test_no_attempts_key_returns_empty(self):
        svc = {"name": "Empty"}
        assert up.progression_note(svc) == ""


# ─── TestEmptyPlaceholder ───────────────────────────────────────────────────


class TestEmptyPlaceholder:
    def test_contains_message(self):
        result = up._empty_placeholder("Test message")
        assert "Test message" in result

    def test_contains_div_class(self):
        result = up._empty_placeholder("Msg")
        assert "poc-placeholder" in result

    def test_contains_instructions(self):
        result = up._empty_placeholder("Msg")
        assert "poc-data.yml" in result
        assert "update-presentation" in result


# ─── TestGenerateBlock1 ─────────────────────────────────────────────────────


class TestGenerateBlock1:
    def test_table_headers(self, sample_data):
        result = up.generate_block_1(sample_data)
        assert "Servico" in result
        assert "Tier" in result
        assert "Score Final" in result

    def test_service_rows(self, sample_data):
        result = up.generate_block_1(sample_data)
        assert "Alpha" in result
        assert "Beta" in result
        assert "Standard" in result
        assert "Simple" in result

    def test_empty_services_returns_placeholder(self, empty_data):
        result = up.generate_block_1(empty_data)
        assert "poc-placeholder" in result

    def test_progression_note_included(self, sample_data):
        result = up.generate_block_1(sample_data)
        # Alpha has 2 attempts, so should show progression
        assert "tentativa 2" in result


# ─── TestGenerateBlock2 ─────────────────────────────────────────────────────


class TestGenerateBlock2:
    def test_timing_section(self, sample_data):
        result = up.generate_block_2(sample_data)
        assert "Tempo" in result
        assert "Tempo medio total" in result

    def test_quality_section(self, sample_data):
        result = up.generate_block_2(sample_data)
        assert "Qualidade" in result
        assert "1a tentativa" in result
        assert "Score final medio" in result

    def test_tier_hours_displayed(self, sample_data):
        result = up.generate_block_2(sample_data)
        assert "Standard" in result
        assert "Simple" in result

    def test_empty_services_returns_placeholder(self, empty_data):
        result = up.generate_block_2(empty_data)
        assert "poc-placeholder" in result


# ─── TestGenerateBlock3 ─────────────────────────────────────────────────────


class TestGenerateBlock3:
    def test_scorecard_header_columns(self, sample_data):
        result = up.generate_block_3(sample_data)
        assert "Alpha" in result
        assert "Beta" in result
        assert "Fase" in result
        assert "Meta" in result

    def test_phase_rows(self, sample_data):
        result = up.generate_block_3(sample_data)
        assert "Fundacao" in result
        assert "Core" in result
        assert "Adapters" in result
        assert "Auto-Config" in result
        assert "Frontend" in result

    def test_total_row(self, sample_data):
        result = up.generate_block_3(sample_data)
        assert "**Total**" in result

    def test_empty_services_returns_placeholder(self, empty_data):
        result = up.generate_block_3(empty_data)
        assert "poc-placeholder" in result


# ─── TestGenerateBlock4 ─────────────────────────────────────────────────────


class TestGenerateBlock4:
    def test_error_table_headers(self, sample_data):
        result = up.generate_block_4(sample_data)
        assert "Violacao" in result
        assert "Frequencia" in result
        assert "Recovery Prompt" in result

    def test_errors_aggregated(self, sample_data):
        result = up.generate_block_4(sample_data)
        assert "Missing @ConditionalOnMissingBean" in result
        assert "Forbidden import in core" in result

    def test_error_count_aggregation(self, sample_data):
        result = up.generate_block_4(sample_data)
        # COMB: 2 (Alpha attempt 1) + 1 (Beta attempt 1) = 3x
        assert "3x" in result

    def test_correction_rate_displayed(self, sample_data):
        result = up.generate_block_4(sample_data)
        # COMB: 2 corrected (Alpha) + 0 corrected (Beta) = 2/3
        assert "2/3" in result

    def test_empty_services_returns_placeholder(self, empty_data):
        result = up.generate_block_4(empty_data)
        assert "poc-placeholder" in result


# ─── TestGenerateBlock5 ─────────────────────────────────────────────────────


class TestGenerateBlock5:
    def test_improvements_table(self, sample_data):
        result = up.generate_block_5(sample_data)
        assert "Melhorias" in result
        assert "migration-guide.md" in result
        assert "bridge config" in result

    def test_no_improvements_placeholder(self):
        data = {"services": [{"name": "Svc", "attempts": [{"improvements": []}]}]}
        result = up.generate_block_5(data)
        assert "Nenhuma melhoria" in result

    def test_empty_services_returns_placeholder(self, empty_data):
        result = up.generate_block_5(empty_data)
        assert "poc-placeholder" in result


# ─── TestGenerateBlock6 ─────────────────────────────────────────────────────


class TestGenerateBlock6:
    def test_worked_well_section(self, sample_data):
        result = up.generate_block_6(sample_data)
        assert "Funcionou Bem" in result
        assert "TDD approach" in result

    def test_needs_improvement_section(self, sample_data):
        result = up.generate_block_6(sample_data)
        assert "Precisa Melhorar" in result
        assert "Auto-config ordering" in result

    def test_poc_names_included(self, sample_data):
        result = up.generate_block_6(sample_data)
        assert "Alpha" in result
        assert "Beta" in result

    def test_empty_learnings_placeholder(self):
        data = {"learnings": {"worked_well": [], "needs_improvement": []}}
        result = up.generate_block_6(data)
        assert "Nenhuma licao" in result

    def test_partial_learnings_worked_well_only(self):
        data = {
            "learnings": {
                "worked_well": [{"text": "Good thing", "pocs": ["A"]}],
                "needs_improvement": [],
            }
        }
        result = up.generate_block_6(data)
        assert "Good thing" in result
        assert "Nenhuma observacao" in result  # needs_improvement is empty


# ─── TestReplaceBlocks ──────────────────────────────────────────────────────


class TestReplaceBlocks:
    @staticmethod
    def _make_content(num_blocks=6):
        """Create mock presentation content with N POC_DATA blocks."""
        blocks = []
        for i in range(num_blocks):
            blocks.append(
                f"## Slide {i + 1}\n\n"
                f"<!-- POC_DATA_START -->\nold content {i}\n<!-- POC_DATA_END -->"
            )
        return "\n\n".join(blocks)

    def test_six_blocks_replaced(self, sample_data):
        content = self._make_content(6)
        result = up.replace_blocks(content, sample_data)
        assert "old content" not in result
        assert result.count("<!-- POC_DATA_START -->") == 6
        assert result.count("<!-- POC_DATA_END -->") == 6

    def test_replacement_contains_generated_content(self, sample_data):
        content = self._make_content(6)
        result = up.replace_blocks(content, sample_data)
        # Block 1 should contain service overview table
        assert "Alpha" in result
        # Block 6 should contain learnings
        assert "Funcionou Bem" in result

    def test_miscount_warns_on_fewer_blocks(self, sample_data, capsys):
        content = self._make_content(4)
        up.replace_blocks(content, sample_data)
        captured = capsys.readouterr()
        assert "WARNING" in captured.err
        assert "Expected 6" in captured.err

    def test_miscount_warns_on_extra_blocks(self, sample_data, capsys):
        content = self._make_content(8)
        up.replace_blocks(content, sample_data)
        captured = capsys.readouterr()
        assert "WARNING" in captured.err

    def test_preserves_surrounding_content(self, sample_data):
        preamble = "# Title\n\n"
        postamble = "\n\n# Footer"
        blocks = []
        for i in range(6):
            blocks.append(f"<!-- POC_DATA_START -->\nold {i}\n<!-- POC_DATA_END -->")
        content = preamble + "\n\n".join(blocks) + postamble
        result = up.replace_blocks(content, sample_data)
        assert result.startswith("# Title")
        assert result.endswith("# Footer")
