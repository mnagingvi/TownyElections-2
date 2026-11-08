#!/usr/bin/env python3
"""Validate the shipped resource files.

Runs in CI (`Lint` workflow) and locally with:

    python scripts/check_messages.py

It answers four questions that are easy to get wrong in a plugin with five
locale files:

1. Does every `messages_<locale>.yml` contain the same keys as `messages_en.yml`?
2. Does a translated string keep the `{placeholders}` its English original has?
3. Does every message key used from Java exist in `messages_en.yml`?
4. Do `config.yml` and `plugin.yml` still contain the keys the plugin needs?

Exit code 0 means clean; 1 means errors were reported.
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

try:
    import yaml
except ImportError:  # pragma: no cover - only hit when PyYAML is missing
    sys.exit("PyYAML is required: python -m pip install pyyaml")

ROOT = Path(__file__).resolve().parent.parent
RESOURCES = ROOT / "src" / "main" / "resources"
JAVA = ROOT / "src" / "main" / "java"

PLACEHOLDER = re.compile(r"\{([a-zA-Z0-9_]+)\}")
# Call shapes that take a message key as their first string argument, e.g.
#   messages.send(sender, "general.no-permission")
#   messages.send(sender, "election.started", placeholders(...))
#   messages.raw("prefix")
#   messages.component("election.system-plurality", ...)
KEY_CALL = re.compile(
    r"\.(?:send|sendNoPrefix|raw|rawList|component|legacy|prefixed)\s*\(\s*"
    r"(?:[A-Za-z0-9_.()]+\s*,\s*)?"
    r"\"([a-z0-9][a-z0-9.\-]*)\""
)

errors: list[str] = []
warnings: list[str] = []


def error(message: str) -> None:
    errors.append(message)


def warn(message: str) -> None:
    warnings.append(message)


def flatten(node, prefix="") -> dict[str, object]:
    """Flatten a nested YAML mapping into {'a.b.c': value}."""
    flat: dict[str, object] = {}
    if isinstance(node, dict):
        for key, value in node.items():
            path = f"{prefix}.{key}" if prefix else str(key)
            if isinstance(value, dict):
                flat.update(flatten(value, path))
            else:
                flat[path] = value
    return flat


def load(name: str):
    path = RESOURCES / name
    try:
        with path.open(encoding="utf-8") as handle:
            return yaml.safe_load(handle)
    except Exception as exc:  # noqa: BLE001 - report anything yaml raises
        error(f"{name}: not valid YAML - {exc}")
        return None


def check_locales() -> set[str]:
    reference_doc = load("messages_en.yml")
    if reference_doc is None:
        return set()
    reference = flatten(reference_doc)
    if not reference:
        error("messages_en.yml: contains no message keys")
        return set()

    locales = sorted(RESOURCES.glob("messages_*.yml"))
    for path in locales:
        if path.name == "messages_en.yml":
            continue
        document = load(path.name)
        if document is None:
            continue
        translated = flatten(document)

        for key in sorted(set(reference) - set(translated)):
            error(f"{path.name}: missing key '{key}'")
        for key in sorted(set(translated) - set(reference)):
            warn(f"{path.name}: key '{key}' is not in messages_en.yml")

        for key in sorted(set(reference) & set(translated)):
            expected = set(PLACEHOLDER.findall(str(reference[key])))
            found = set(PLACEHOLDER.findall(str(translated[key])))
            missing = expected - found
            extra = found - expected
            if missing:
                error(
                    f"{path.name}: '{key}' is missing placeholder(s) "
                    f"{', '.join('{' + p + '}' for p in sorted(missing))}"
                )
            if extra:
                warn(
                    f"{path.name}: '{key}' has unknown placeholder(s) "
                    f"{', '.join('{' + p + '}' for p in sorted(extra))}"
                )
    return set(reference)


def check_java_keys(reference: set[str]) -> None:
    if not reference:
        return
    used = 0
    for path in sorted(JAVA.rglob("*.java")):
        text = path.read_text(encoding="utf-8")
        for match in KEY_CALL.finditer(text):
            key = match.group(1)
            # Skip keys the code builds by concatenation, e.g. "election.system-"
            if key.endswith("-") or key.endswith("."):
                continue
            if "." not in key:
                continue
            used += 1
            if key not in reference:
                line = text[: match.start()].count("\n") + 1
                error(f"{path.relative_to(ROOT)}:{line}: message key '{key}' is not in messages_en.yml")
    if used == 0:
        warn("no message keys were found in the Java sources - the check may be stale")


def check_config() -> None:
    document = load("config.yml")
    if document is None:
        return
    if "config-version" not in document:
        error("config.yml: missing 'config-version'")
    for section in ("general", "election", "winner", "notifications"):
        if section not in document:
            error(f"config.yml: missing '{section}' section")


def check_plugin_yml() -> None:
    document = load("plugin.yml")
    if document is None:
        return
    for key in ("name", "version", "main", "api-version"):
        if key not in document:
            error(f"plugin.yml: missing '{key}'")
    if not document.get("depend"):
        warn("plugin.yml: no hard dependency on Towny")

    permissions = document.get("permissions") or {}
    for name, definition in permissions.items():
        if not isinstance(definition, dict):
            error(f"plugin.yml: permission '{name}' is not a mapping")
            continue
        unknown = set(definition) - {"description", "default", "children"}
        if unknown:
            error(
                f"plugin.yml: permission '{name}' has unknown key(s) "
                f"{', '.join(sorted(unknown))} - check the indentation"
            )
        if "description" not in definition:
            warn(f"plugin.yml: permission '{name}' has no description")
        children = definition.get("children") or {}
        for child in children:
            if child not in permissions:
                warn(f"plugin.yml: '{name}' grants undeclared permission '{child}'")


def main() -> int:
    reference = check_locales()
    check_java_keys(reference)
    check_config()
    check_plugin_yml()

    for warning in warnings:
        print(f"warning: {warning}")
    for problem in errors:
        print(f"error: {problem}")

    if errors:
        print(f"\n{len(errors)} error(s), {len(warnings)} warning(s)")
        return 1
    print(f"resource files OK ({len(reference)} message keys, {len(warnings)} warning(s))")
    return 0


if __name__ == "__main__":
    sys.exit(main())
