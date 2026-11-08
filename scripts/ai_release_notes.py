#!/usr/bin/env python3
"""Write the body of a GitHub release, using OpenRouter for the prose.

The script collects what actually happened since the last release - the git
commit subjects and the matching `CHANGELOG.md` section - and asks an LLM for a
short, human release announcement. When no API key is configured, or the request
fails, it degrades to the changelog section (or a plain commit list) so a release
never breaks because of a third party.

Usage
-----

    # notes for the version in pom.xml, against the previous tag
    python3 scripts/ai_release_notes.py

    # notes for a specific tag/version, written to a file
    python3 scripts/ai_release_notes.py --version 2.0.0 --out release-notes.md

    # different model or a self-hosted gateway
    python3 scripts/ai_release_notes.py --model anthropic/claude-sonnet-4.5
    OPENROUTER_ENDPOINT=http://localhost:8080/v1/chat/completions python3 ...

The API key comes from `--api-key` or the `OPENROUTER_API_KEY` environment
variable and is never echoed. Only the request is sent to the endpoint; nothing
else in the repository leaves the machine.
"""

from __future__ import annotations

import argparse
import json
import os
import re
import subprocess
import sys
import urllib.error
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DEFAULT_ENDPOINT = "https://openrouter.ai/api/v1/chat/completions"
# openrouter/auto lets OpenRouter pick a current model, so a decommissioned
# model name cannot silently break releases. Override with --model.
DEFAULT_MODEL = "openrouter/auto"
REPO_URL = "https://github.com/mnagingvi/TownyElections-2"

SYSTEM_PROMPT = (
    "You write release notes for TownyElections, a Paper (Minecraft 1.21.4+) "
    "plugin that adds a formal election system to towns managed by the Towny "
    "plugin: residents run for office, campaign, and vote in plurality, "
    "ranked-choice or approval elections, and the winner gets town ranks or "
    "mayorship. Your readers are server owners deciding whether to update."
)


def git(*args: str) -> str:
    result = subprocess.run(
        ["git", *args],
        cwd=ROOT,
        capture_output=True,
        text=True,
        check=False,
    )
    return result.stdout.strip() if result.returncode == 0 else ""


def env_or(name: str, default: str) -> str:
    """Read an environment variable, treating an empty value as unset.

    GitHub Actions passes an unset repository variable as an empty string, so
    `os.environ.get(name, default)` would happily hand the API an empty model.
    """
    value = os.environ.get(name)
    return value if value else default


def normalise(version: str) -> str:
    return version.strip().lstrip("vV")


def pom_version() -> str:
    text = (ROOT / "pom.xml").read_text(encoding="utf-8")
    match = re.search(r"<version>([^<]+)</version>", text)
    return normalise(match.group(1)) if match else "0.0.0"


def version_key(version: str) -> tuple[int, ...]:
    parts = []
    for piece in normalise(version).split("."):
        digits = re.match(r"\d+", piece)
        parts.append(int(digits.group(0)) if digits else 0)
    while len(parts) < 3:
        parts.append(0)
    return tuple(parts)


def previous_tag(version: str) -> str | None:
    """The highest semver-ish tag below `version` that HEAD contains."""
    tags = [tag for tag in git("tag", "--merged", "HEAD").splitlines() if tag.strip()]
    candidates = [
        tag
        for tag in tags
        if re.fullmatch(r"v?\d+\.\d+(\.\d+)?", tag) and version_key(tag) < version_key(version)
    ]
    if not candidates:
        return None
    return max(candidates, key=version_key)


def commits(previous: str | None, limit: int) -> list[str]:
    span = f"{previous}..HEAD" if previous else "HEAD"
    output = git("log", span, f"--max-count={limit}", "--pretty=format:%s")
    lines = [line.strip() for line in output.splitlines() if line.strip()]
    # Merge commits and version bumps rarely tell a reader anything.
    noisy = re.compile(r"^(Merge |Bump the version|Changelog the|Release )", re.IGNORECASE)
    return [line for line in lines if not noisy.match(line)]


def changelog_section(version: str) -> str:
    path = ROOT / "CHANGELOG.md"
    if not path.is_file():
        return ""
    text = path.read_text(encoding="utf-8")
    pattern = re.compile(
        r"^##\s*\[?v?" + re.escape(version) + r"\]?.*?$(.*?)(?=^##\s|\Z)",
        re.MULTILINE | re.DOTALL,
    )
    match = pattern.search(text)
    return match.group(1).strip() if match else ""


def build_prompt(version: str, previous: str | None, subjects: list[str], changelog: str) -> str:
    commit_block = "\n".join(f"- {subject}" for subject in subjects) or "- (no commits found)"
    parts = [
        f"Version: {version}",
        f"Previous release: {previous or 'none - this is the first tagged release'}",
        "",
        "Commits since then:",
        commit_block,
    ]
    if changelog:
        parts += ["", "Maintainer changelog entry for this version:", changelog]
    parts += [
        "",
        "Write the release announcement as GitHub-flavoured Markdown:",
        "- open with one or two sentences on the theme of this release,",
        "- then 3 to 6 bullets, each starting with a bold 2-4 word topic, about "
        "what a server owner or player will notice,",
        "- fold CI, tooling and documentation work into one short final bullet,",
        "- state any breaking change, required config migration or minimum Paper/"
        "Towny version explicitly, but only if the input says so,",
        "- do not invent features, numbers, versions or links that are not in the input,",
        "- stay under 180 words, use no top-level heading, and at most one emoji per bullet.",
        "",
        "Reply with the Markdown body only - no preamble, no code fence.",
    ]
    return "\n".join(parts)


def call_openrouter(endpoint: str, api_key: str, model: str, prompt: str, timeout: float) -> str:
    payload = json.dumps(
        {
            "model": model,
            "temperature": 0.3,
            "max_tokens": 800,
            "messages": [
                {"role": "system", "content": SYSTEM_PROMPT},
                {"role": "user", "content": prompt},
            ],
        }
    ).encode("utf-8")

    request = urllib.request.Request(
        endpoint,
        data=payload,
        method="POST",
        headers={
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json",
            # OpenRouter uses these to attribute traffic; both are optional.
            "HTTP-Referer": REPO_URL,
            "X-Title": "TownyElections release notes",
        },
    )

    with urllib.request.urlopen(request, timeout=timeout) as response:
        body = json.loads(response.read().decode("utf-8"))

    choices = body.get("choices") or []
    if not choices:
        raise ValueError(f"no choices in response: {str(body)[:300]}")
    content = (choices[0].get("message") or {}).get("content") or ""
    content = content.strip()
    if not content:
        raise ValueError("empty completion")
    # Some models still wrap the answer in a fenced block.
    if content.startswith("```"):
        content = re.sub(r"^```[a-zA-Z]*\n?", "", content)
        content = re.sub(r"\n?```$", "", content).strip()
    usage = body.get("usage") or {}
    if usage:
        print(
            f"openrouter: model={body.get('model', model)} "
            f"prompt_tokens={usage.get('prompt_tokens', '?')} "
            f"completion_tokens={usage.get('completion_tokens', '?')}",
            file=sys.stderr,
        )
    return content


def fallback(version: str, previous: str | None, subjects: list[str], changelog: str, reason: str) -> str:
    print(f"openrouter: {reason} - falling back to the changelog", file=sys.stderr)
    if changelog:
        body = changelog
    else:
        body = "\n".join(f"- {subject}" for subject in subjects) or "- No changes recorded."
    header = (
        f"_Automated summary unavailable ({reason}). "
        f"Notes below are taken straight from the changelog._"
    )
    return f"{header}\n\n{body}"


def main() -> int:
    parser = argparse.ArgumentParser(description="Write a release body, with OpenRouter for the prose.")
    parser.add_argument("--version", default=None, help="version to write notes for (default: pom version)")
    parser.add_argument("--from", dest="previous", default=None, help="previous tag (default: highest tag below --version)")
    parser.add_argument("--model", default=env_or("OPENROUTER_MODEL", DEFAULT_MODEL))
    parser.add_argument("--endpoint", default=env_or("OPENROUTER_ENDPOINT", DEFAULT_ENDPOINT))
    parser.add_argument("--api-key", default=os.environ.get("OPENROUTER_API_KEY"))
    parser.add_argument("--max-commits", type=int, default=200)
    parser.add_argument("--timeout", type=float, default=60.0)
    parser.add_argument("--out", help="write the notes here instead of stdout")
    parser.add_argument("--no-ai", action="store_true", help="skip the API call and only use the changelog")
    args = parser.parse_args()

    version = normalise(args.version or pom_version())
    previous = args.previous if args.previous is not None else previous_tag(version)
    subjects = commits(previous, args.max_commits)
    changelog = changelog_section(version)

    if not subjects and not changelog:
        print(f"no commits or changelog entry found for {version}", file=sys.stderr)
        return 1

    if args.no_ai:
        notes = fallback(version, previous, subjects, changelog, "AI summaries disabled")
    elif not args.api_key:
        notes = fallback(
            version,
            previous,
            subjects,
            changelog,
            "OPENROUTER_API_KEY is not set",
        )
    else:
        prompt = build_prompt(version, previous, subjects, changelog)
        try:
            notes = call_openrouter(args.endpoint, args.api_key, args.model, prompt, args.timeout)
        except urllib.error.HTTPError as error:
            detail = error.read().decode("utf-8", errors="replace")[:300]
            notes = fallback(version, previous, subjects, changelog, f"HTTP {error.code}: {detail}")
        except Exception as error:  # noqa: BLE001 - a release must never die here
            notes = fallback(version, previous, subjects, changelog, f"{type(error).__name__}: {error}")

    if args.out:
        Path(args.out).write_text(notes + "\n", encoding="utf-8")
        print(f"wrote {args.out} ({len(notes.split())} words)", file=sys.stderr)
    else:
        sys.stdout.write(notes + "\n")
    return 0


if __name__ == "__main__":
    sys.exit(main())
