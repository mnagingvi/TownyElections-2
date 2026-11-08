#!/usr/bin/env python3
"""Write a Markdown build report for CI.

Used twice by `.github/workflows/build.yml`:

    # job summary shown on the workflow run page
    python3 scripts/build_summary.py >> "$GITHUB_STEP_SUMMARY"

    # sticky comment on the pull request
    python3 scripts/build_summary.py --out build-report.md

Everything is read from the files Maven leaves behind (`target/`), so the script
never needs to know how the build was invoked. Missing pieces are reported
instead of crashing: the build may have failed before producing a jar.
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
TARGET = ROOT / "target"
REPORTS = TARGET / "surefire-reports"

# "Tests run: 5, Failures: 0, Errors: 0, Skipped: 0"
TOTALS = re.compile(r"Tests run: (\d+), Failures: (\d+), Errors: (\d+), Skipped: (\d+)")


def pom_version() -> str:
    text = (ROOT / "pom.xml").read_text(encoding="utf-8")
    match = re.search(r"<version>([^<]+)</version>", text)
    return match.group(1) if match else "unknown"


def jar_summary() -> tuple[str, str]:
    jars = sorted(TARGET.glob("TownyElections-*.jar"))
    if not jars:
        return "—", "no jar was produced"
    jar = jars[-1]
    size = jar.stat().st_size
    return jar.name, f"{size / 1024:.1f} KiB"


def test_summary() -> tuple[list[tuple[str, int, int, int, int]], tuple[int, int, int, int]]:
    rows: list[tuple[str, int, int, int, int]] = []
    totals = [0, 0, 0, 0]
    if not REPORTS.is_dir():
        return rows, tuple(totals)  # type: ignore[return-value]

    for report in sorted(REPORTS.glob("*.txt")):
        text = report.read_text(encoding="utf-8", errors="replace")
        match = TOTALS.search(text)
        if not match:
            continue
        numbers = tuple(int(value) for value in match.groups())
        rows.append((report.stem, *numbers))
        for index, value in enumerate(numbers):
            totals[index] += value
    return rows, tuple(totals)  # type: ignore[return-value]


def render() -> str:
    version = pom_version()
    jar_name, jar_size = jar_summary()
    rows, totals = test_summary()
    tests, failures, errors, skipped = totals

    lines = [
        f"### TownyElections {version}",
        "",
        f"**Jar:** `{jar_name}` — {jar_size}",
        "",
    ]

    if tests:
        verdict = "✅ all passing" if failures == 0 and errors == 0 else "❌ see the failed run"
        lines += [
            f"**Tests:** {tests} run, {failures} failed, {errors} errored, "
            f"{skipped} skipped — {verdict}",
            "",
            "<details><summary>Per test class</summary>",
            "",
            "| Test class | Run | Failed | Errored | Skipped |",
            "| --- | --- | --- | --- | --- |",
        ]
        for name, run, fail, error_count, skip in rows:
            lines.append(f"| `{name}` | {run} | {fail} | {error_count} | {skip} |")
        lines += ["", "</details>", ""]
    else:
        lines += ["**Tests:** no Surefire reports were produced", ""]

    lines += [
        "The jar is attached to this workflow run as the **TownyElections** "
        "artifact (see the run page).",
    ]
    return "\n".join(lines) + "\n"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--out", help="write the report to this file instead of stdout")
    args = parser.parse_args()

    report = render()
    if args.out:
        Path(args.out).write_text(report, encoding="utf-8")
        print(f"wrote {args.out}")
    else:
        sys.stdout.write(report)
    return 0


if __name__ == "__main__":
    sys.exit(main())
