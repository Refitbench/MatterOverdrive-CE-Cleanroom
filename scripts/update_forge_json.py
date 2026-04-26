#!/usr/bin/env python3
"""
Reads CHANGELOG.md and updates update.json for Forge's built-in update checker.

Rules:
- Only processes the topmost changelog entry.
- Skips silently if that entry has no date (## [X.Y.Z] - ) — treated as WIP.
- Proceeds if the entry is fully dated   (## [X.Y.Z] - YYYY-MM-DD).

The MC version is read from mcmod.info ("mcversion" field).
"""

import json
import os
import re
import sys
from pathlib import Path

CHANGELOG  = Path("CHANGELOG.md")
UPDATE_JSON = Path("update.json")
MCMOD_INFO  = Path("src/main/resources/mcmod.info")

# Matches a finalized entry:  ## [1.0.1] - 2026-03-31
DATED_RE = re.compile(
    r"^## \[(\d+\.\d+\.\d+)\] - (\d{4}-\d{2}-\d{2})\s*$", re.MULTILINE
)
# Matches any entry header (dated or not):  ## [1.0.1]
ANY_HEADER_RE = re.compile(r"^## \[\d+\.\d+\.\d+\]", re.MULTILINE)


def get_mc_version() -> str:
    """Extract mcversion from mcmod.info, falling back to 1.12.2."""
    try:
        text = MCMOD_INFO.read_text(encoding="utf-8")
        # mcmod.info may contain template tokens; look for the literal value
        m = re.search(r'"mcversion"\s*:\s*"([^"]+)"', text)
        if m and not m.group(1).startswith("$"):
            return m.group(1)
    except FileNotFoundError:
        pass
    return "1.12.2"


def parse_changelog():
    """
    Returns (version, changelog_text) for the topmost dated entry,
    or exits with code 0 if no dated entry is found.
    """
    text = CHANGELOG.read_text(encoding="utf-8")

    # Find all entry headers (any version, dated or not)
    all_headers = list(ANY_HEADER_RE.finditer(text))
    if not all_headers:
        print("CHANGELOG.md has no version entries. Skipping.")
        sys.exit(0)

    # Only look at the topmost entry
    first = all_headers[0]
    first_line = text[first.start():text.index("\n", first.start())]

    # Check it is dated
    dated = DATED_RE.match(first_line)
    if not dated:
        print(f"Topmost entry '{first_line.strip()}' has no date. Treating as WIP — skipping.")
        sys.exit(0)

    version = dated.group(1)

    # Extract body: from end of header line to the start of the next header (or EOF)
    body_start = first.end()
    next_header = all_headers[1] if len(all_headers) > 1 else None
    body_end = next_header.start() if next_header else len(text)
    body = text[body_start:body_end].strip()

    # Collapse to non-empty lines
    lines = [l.rstrip() for l in body.splitlines() if l.strip()]
    changelog_text = "\n".join(lines)

    return version, changelog_text


def update_json(mc_version: str, version: str, changelog_text: str):
    if UPDATE_JSON.exists():
        data = json.loads(UPDATE_JSON.read_text(encoding="utf-8"))
    else:
        data = {
            "homepage": "https://www.curseforge.com/minecraft/mc-mods/matter-overdrive-refitted",
            mc_version: {},
            "promos": {}
        }

    data.setdefault(mc_version, {})
    data.setdefault("promos", {})

    data[mc_version][version] = changelog_text
    data["promos"][f"{mc_version}-latest"] = version
    data["promos"][f"{mc_version}-recommended"] = version

    UPDATE_JSON.write_text(
        json.dumps(data, indent=4, ensure_ascii=False) + "\n",
        encoding="utf-8"
    )
    print(f"update.json updated: {mc_version} -> {version}")

    # Export version for the workflow commit message
    github_output = os.environ.get("GITHUB_OUTPUT", "")
    if github_output:
        with open(github_output, "a") as f:
            f.write(f"version={version}\n")
    # Also set env var used in the commit step
    github_env = os.environ.get("GITHUB_ENV", "")
    if github_env:
        with open(github_env, "a") as f:
            f.write(f"RELEASE_VERSION={version}\n")


if __name__ == "__main__":
    mc_ver = get_mc_version()
    ver, log = parse_changelog()
    update_json(mc_ver, ver, log)
