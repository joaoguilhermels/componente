#!/usr/bin/env python3
"""Regenerate POC data slides in the CTO presentation from poc-data.yml.

Usage:
    python3 scripts/update_presentation.py              # overwrites presentation in-place
    python3 scripts/update_presentation.py --dry-run    # prints to stdout instead

Dependencies: PyYAML (stdlib-only otherwise).
"""

import argparse
import re
import sys
from pathlib import Path

import yaml

SCRIPT_DIR = Path(__file__).resolve().parent
REPO_ROOT = SCRIPT_DIR.parent
YAML_PATH = REPO_ROOT / "docs" / "presentations" / "poc-data.yml"
PRESENTATION_PATH = REPO_ROOT / "docs" / "presentations" / "cto-ai-migration-strategy.md"

# Phase weights from scorecard.md
PHASE_WEIGHTS = {1: 0.25, 2: 0.35, 3: 0.15, 4: 0.20, 5: 0.05}

BLOCK_PATTERN = re.compile(
    r"(<!-- POC_DATA_START -->)\n(.*?)\n(<!-- POC_DATA_END -->)",
    re.DOTALL,
)


def load_data(yaml_path: Path) -> dict:
    """Parse and validate the POC data YAML file."""
    with open(yaml_path, encoding="utf-8") as f:
        data = yaml.safe_load(f)
    if not data or "services" not in data:
        return {"services": [], "learnings": {"worked_well": [], "needs_improvement": []}}
    if "learnings" not in data:
        data["learnings"] = {"worked_well": [], "needs_improvement": []}
    return data


def latest_attempt(service: dict) -> dict:
    """Return the most recent attempt for a service."""
    return service["attempts"][-1]


def weighted_score(scores: dict) -> float:
    """Compute weighted score, re-normalizing when phases are null."""
    total_weight = 0.0
    weighted_sum = 0.0
    for phase_num, weight in PHASE_WEIGHTS.items():
        key = f"phase_{phase_num}"
        value = scores.get(key)
        if value is not None:
            total_weight += weight
            weighted_sum += weight * value
    if total_weight == 0:
        return 0.0
    return weighted_sum / total_weight


def fmt_score(value) -> str:
    """Format a score value: null -> 'N/A', number -> 'XX%'."""
    if value is None:
        return "N/A"
    return f"{value:.0f}%" if isinstance(value, float) else f"{value}%"


def fmt_hours(value) -> str:
    """Format hours: null -> 'N/A', number -> 'X.Xh'."""
    if value is None:
        return "N/A"
    return f"{value:.1f}h"


def progression_note(service: dict) -> str:
    """Build a progression note when multiple attempts exist."""
    attempts = service.get("attempts", [])
    if len(attempts) < 2:
        return ""
    first = attempts[0]
    last = attempts[-1]
    first_score = weighted_score(first["scores"])
    last_score = weighted_score(last["scores"])
    return f" (tentativa {last['attempt']}, {first_score:.0f}% → {last_score:.0f}%)"


def generate_block_1(data: dict) -> str:
    """POC Overview — latest attempt per service."""
    services = data.get("services", [])
    if not services:
        return _empty_placeholder("Nenhuma POC registrada ainda")

    lines = []
    lines.append("")
    lines.append("| Servico | Tier | Fase Alcancada | Score Final | Tempo Total | Observacoes |")
    lines.append("|---------|------|---------------|-------------|-------------|-------------|")
    for svc in services:
        attempt = latest_attempt(svc)
        score = weighted_score(attempt["scores"])
        note = progression_note(svc)
        timing = attempt.get("timing", {})
        lines.append(
            f"| {svc['name']} | {svc['tier']} "
            f"| {attempt['phase_reached']} "
            f"| {score:.0f}% "
            f"| {fmt_hours(timing.get('total_hours'))} "
            f"| {attempt.get('notes', '')}{note} |"
        )
    lines.append("")
    return "\n".join(lines)


def generate_block_2(data: dict) -> str:
    """Resultados Quantitativos — aggregated metrics."""
    services = data.get("services", [])
    if not services:
        return _empty_placeholder("Nenhuma POC registrada ainda")

    all_attempts = [a for svc in services for a in svc["attempts"]]
    latest_attempts = [latest_attempt(svc) for svc in services]

    # Timing aggregation (latest attempt per service)
    total_hours = [a.get("timing", {}).get("total_hours") for a in latest_attempts]
    total_hours = [h for h in total_hours if h is not None]
    avg_total = sum(total_hours) / len(total_hours) if total_hours else 0

    # Per-phase average timing across latest attempts
    phase_hours = {}
    for phase_num in range(6):
        key = f"phase_{phase_num}_hours"
        values = [a.get("timing", {}).get(key) for a in latest_attempts]
        values = [v for v in values if v is not None]
        phase_hours[phase_num] = sum(values) / len(values) if values else None

    # Tier-specific timing
    tier_hours = {}
    for svc in services:
        attempt = latest_attempt(svc)
        tier = svc["tier"]
        h = attempt.get("timing", {}).get("total_hours")
        if h is not None:
            tier_hours.setdefault(tier, []).append(h)

    # Quality aggregation (latest attempt per service)
    first_try_pcts = [a.get("quality", {}).get("first_try_correct_pct", 0) for a in latest_attempts]
    recovery_prompts = [a.get("quality", {}).get("recovery_prompts_used", 0) for a in latest_attempts]
    archunit_violations = [a.get("quality", {}).get("archunit_violations", 0) for a in latest_attempts]
    scores = [weighted_score(a["scores"]) for a in latest_attempts]

    avg_first_try = sum(first_try_pcts) / len(first_try_pcts) if first_try_pcts else 0
    avg_recovery = sum(recovery_prompts) / len(recovery_prompts) if recovery_prompts else 0
    avg_violations = sum(archunit_violations) / len(archunit_violations) if archunit_violations else 0
    avg_score = sum(scores) / len(scores) if scores else 0

    lines = []
    lines.append("")
    lines.append('<div class="columns">')
    lines.append("<div>")
    lines.append("")
    lines.append("### Tempo")
    lines.append("")
    lines.append("| Metrica | Valor |")
    lines.append("|---------|-------|")

    phase_avg_str = " / ".join(
        fmt_hours(phase_hours.get(p))
        for p in range(6)
        if phase_hours.get(p) is not None
    )
    lines.append(f"| Tempo medio por fase | {phase_avg_str} |")

    for tier_name in ["Simple", "Standard", "Advanced"]:
        if tier_name in tier_hours:
            avg_t = sum(tier_hours[tier_name]) / len(tier_hours[tier_name])
            lines.append(f"| Tempo total ({tier_name}) | {fmt_hours(avg_t)} |")

    lines.append(f"| Tempo medio total | {fmt_hours(avg_total)} |")
    lines.append("")
    lines.append("</div>")
    lines.append("<div>")
    lines.append("")
    lines.append("### Qualidade")
    lines.append("")
    lines.append("| Metrica | Valor |")
    lines.append("|---------|-------|")
    lines.append(f"| Codigo correto na 1a tentativa | {avg_first_try:.0f}% |")
    lines.append(f"| Recovery prompts usados (media) | {avg_recovery:.1f} |")
    lines.append(f"| Violacoes ArchUnit pos-geracao (media) | {avg_violations:.1f} |")
    lines.append(f"| Score final medio | {avg_score:.0f}% |")
    lines.append("")
    lines.append("</div>")
    lines.append("</div>")
    lines.append("")
    return "\n".join(lines)


def generate_block_3(data: dict) -> str:
    """Scorecard Comparativo — dynamic columns per service."""
    services = data.get("services", [])
    if not services:
        return _empty_placeholder("Nenhuma POC registrada ainda")

    # Build header
    svc_headers = " | ".join(svc["name"] for svc in services)
    header = f"| Fase | Peso | {svc_headers} | Meta |"
    separator = "|------|------|" + "|".join("----" for _ in services) + "|------|"

    phase_names = {
        1: "1 — Fundacao",
        2: "2 — Core",
        3: "3 — Adapters",
        4: "4 — Auto-Config",
        5: "5 — Frontend",
    }

    lines = []
    lines.append("")
    lines.append(header)
    lines.append(separator)

    for phase_num in range(1, 6):
        weight_pct = f"{PHASE_WEIGHTS[phase_num] * 100:.0f}%"
        name = phase_names[phase_num]
        meta = "N/A" if phase_num == 5 else "100%"

        cells = []
        for svc in services:
            attempt = latest_attempt(svc)
            score_val = attempt["scores"].get(f"phase_{phase_num}")
            cells.append(fmt_score(score_val))

        svc_cells = " | ".join(cells)
        lines.append(f"| {name} | {weight_pct} | {svc_cells} | {meta} |")

    # Total row
    total_cells = []
    for svc in services:
        attempt = latest_attempt(svc)
        total_cells.append(f"{weighted_score(attempt['scores']):.0f}%")

    total_svc = " | ".join(total_cells)
    lines.append(f"| **Total** | **100%** | {total_svc} | **95%+** |")
    lines.append("")
    return "\n".join(lines)


def generate_block_4(data: dict) -> str:
    """Erros Comuns — aggregated across ALL attempts for trend visibility."""
    services = data.get("services", [])
    if not services:
        return _empty_placeholder("Nenhuma POC registrada ainda")

    # Aggregate errors across all attempts of all services
    error_map: dict[str, dict] = {}
    for svc in services:
        for attempt in svc["attempts"]:
            for err in attempt.get("errors", []):
                key = err["violation"]
                if key not in error_map:
                    error_map[key] = {
                        "violation": err["violation"],
                        "phase": err["phase"],
                        "total_count": 0,
                        "corrected_count": 0,
                        "recovery_prompt": err.get("recovery_prompt", ""),
                        "pocs": set(),
                    }
                error_map[key]["total_count"] += err.get("count", 1)
                if err.get("corrected", False):
                    error_map[key]["corrected_count"] += err.get("count", 1)
                error_map[key]["pocs"].add(svc["name"])

    # Sort by total_count descending
    sorted_errors = sorted(error_map.values(), key=lambda e: e["total_count"], reverse=True)

    lines = []
    lines.append("")
    lines.append("| Violacao | Fase | Frequencia | Recovery Prompt | Taxa de Correcao |")
    lines.append("|----------|------|-----------|-----------------|------------------|")
    for err in sorted_errors:
        total = err["total_count"]
        corrected = err["corrected_count"]
        rate = f"{corrected}/{total}" if total > 0 else "N/A"
        poc_list = ", ".join(sorted(err["pocs"]))
        lines.append(
            f"| {err['violation']} | {err['phase']} "
            f"| {total}x ({poc_list}) "
            f"| {err['recovery_prompt']} "
            f"| {rate} |"
        )
    lines.append("")
    return "\n".join(lines)


def generate_block_5(data: dict) -> str:
    """Feedback Loop — all improvements, chronological."""
    services = data.get("services", [])
    if not services:
        return _empty_placeholder("Nenhuma POC registrada ainda")

    all_improvements = []
    for svc in services:
        for attempt in svc["attempts"]:
            for imp in attempt.get("improvements", []):
                all_improvements.append({
                    "poc": svc["name"],
                    "date": attempt.get("date", ""),
                    "attempt": attempt.get("attempt", 1),
                    **imp,
                })

    if not all_improvements:
        return _empty_placeholder("Nenhuma melhoria registrada ainda")

    # Sort chronologically
    all_improvements.sort(key=lambda x: (x["date"], x["poc"]))

    lines = []
    lines.append("")
    lines.append("### Melhorias Derivadas das POCs")
    lines.append("")
    lines.append("| POC | Artefato Modificado | Melhoria | Impacto |")
    lines.append("|-----|--------------------|-----------| --------|")
    for imp in all_improvements:
        lines.append(
            f"| {imp['poc']} (#{imp['attempt']}) "
            f"| {imp.get('artifact', '')} "
            f"| {imp.get('change', '')} "
            f"| {imp.get('impact', '')} |"
        )
    lines.append("")
    return "\n".join(lines)


def generate_block_6(data: dict) -> str:
    """Licoes Aprendidas — from global learnings."""
    learnings = data.get("learnings", {})
    worked = learnings.get("worked_well", [])
    needs_improvement = learnings.get("needs_improvement", [])

    if not worked and not needs_improvement:
        return _empty_placeholder("Nenhuma licao registrada ainda")

    lines = []
    lines.append("")
    lines.append('<div class="columns">')
    lines.append("<div>")
    lines.append("")
    lines.append("### O Que Funcionou Bem")
    if worked:
        for item in worked:
            poc_list = ", ".join(item.get("pocs", []))
            lines.append(f"- {item['text']} ({poc_list})")
    else:
        lines.append("- _Nenhuma observacao ainda_")
    lines.append("")
    lines.append("</div>")
    lines.append("<div>")
    lines.append("")
    lines.append("### O Que Precisa Melhorar")
    if needs_improvement:
        for item in needs_improvement:
            poc_list = ", ".join(item.get("pocs", []))
            lines.append(f"- {item['text']} ({poc_list})")
    else:
        lines.append("- _Nenhuma observacao ainda_")
    lines.append("")
    lines.append("</div>")
    lines.append("</div>")
    lines.append("")
    return "\n".join(lines)


def _empty_placeholder(message: str) -> str:
    """Generate a Marp-compatible placeholder block."""
    return f"""
<div class="poc-placeholder">

**{message}**

Instrucoes: adicione dados em `docs/presentations/poc-data.yml`
e execute `make update-presentation`.

</div>
"""


def replace_blocks(content: str, data: dict) -> str:
    """Find all 6 POC_DATA_START/END pairs and replace their contents."""
    generators = [
        generate_block_1,
        generate_block_2,
        generate_block_3,
        generate_block_4,
        generate_block_5,
        generate_block_6,
    ]

    matches = list(BLOCK_PATTERN.finditer(content))
    if len(matches) != 6:
        print(
            f"WARNING: Expected 6 POC_DATA blocks, found {len(matches)}. "
            f"Processing {min(len(matches), 6)} blocks.",
            file=sys.stderr,
        )

    block_count = min(len(matches), len(generators))

    # Replace last-to-first to preserve character positions
    for i in range(block_count - 1, -1, -1):
        match = matches[i]
        new_block = generators[i](data)
        start_marker = match.group(1)
        end_marker = match.group(3)
        replacement = f"{start_marker}\n{new_block}\n{end_marker}"
        content = content[: match.start()] + replacement + content[match.end() :]

    return content


def main():
    parser = argparse.ArgumentParser(
        description="Regenerate POC data slides in the CTO presentation."
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Print result to stdout instead of overwriting the file.",
    )
    parser.add_argument(
        "--yaml",
        type=Path,
        default=YAML_PATH,
        help=f"Path to POC data YAML (default: {YAML_PATH})",
    )
    parser.add_argument(
        "--presentation",
        type=Path,
        default=PRESENTATION_PATH,
        help=f"Path to presentation markdown (default: {PRESENTATION_PATH})",
    )
    args = parser.parse_args()

    data = load_data(args.yaml)
    content = args.presentation.read_text(encoding="utf-8")
    result = replace_blocks(content, data)

    if args.dry_run:
        print(result)
    else:
        args.presentation.write_text(result, encoding="utf-8")
        print(f"Updated: {args.presentation}")


if __name__ == "__main__":
    main()
