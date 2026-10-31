#!/usr/bin/env python3
"""Regenerate supporting link-only indexes from the checked-in catalog snapshot."""
from __future__ import annotations

import json
from pathlib import Path

from build_content import ROOT, build_supporting_catalogs, sanitize_content


def main() -> None:
    licensed = json.loads((ROOT / "licensed_content.json").read_text(encoding="utf-8"))
    source = json.loads((ROOT / "content.json").read_text(encoding="utf-8"))
    safe = sanitize_content(source, licensed)
    build_supporting_catalogs(safe)


if __name__ == "__main__":
    main()
