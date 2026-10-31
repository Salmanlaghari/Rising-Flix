#!/usr/bin/env python3
"""Validate the curated, creator-authorized feature catalog without scraping any provider."""
from __future__ import annotations

import argparse
import json
import shutil
import subprocess
from pathlib import Path
from typing import Any

EXPECTED = {
    "id": "valkaama_cc_by_sa",
    "title": "Valkaama",
    "videoUrl": "https://www.valkaama.com/media/movie/Valkaama_720p.mkv",
    "sourcePageUrl": "https://www.valkaama.com/index.php?page=valkaama&l=en",
    "sourceName": "Valkaama official site",
    "licenseName": "CC BY-SA 3.0",
    "licenseUrl": "https://creativecommons.org/licenses/by-sa/3.0/",
    "actualDurationSeconds": 5586,
    "contentType": "feature",
    "attribution": (
        "Valkaama, directed and produced by Tim Baumann; story by Hendrik Behnisch and Lynn Gray. "
        "Film licensed CC BY-SA 3.0. Thumbnail is a resized still from the film."
    ),
}
MIN_FEATURE_SECONDS = 45 * 60


def catalog_items(data: dict[str, Any]) -> list[dict[str, Any]]:
    rows: list[dict[str, Any]] = []
    featured = data.get("featured")
    if isinstance(featured, dict):
        rows.append(featured)
    for category in data.get("categories", []):
        if isinstance(category, dict):
            rows.extend(item for item in category.get("items", []) if isinstance(item, dict))
    deduped: dict[str, dict[str, Any]] = {}
    for item in rows:
        if item.get("id"):
            deduped[str(item["id"])] = item
    return list(deduped.values())


def validate_catalog(data: dict[str, Any]) -> list[dict[str, Any]]:
    items = catalog_items(data)
    if len(items) != 1:
        raise ValueError(f"Expected exactly one reviewed feature source; found {len(items)} unique entries")
    item = items[0]
    for key, expected in EXPECTED.items():
        if item.get(key) != expected:
            raise ValueError(f"{key} does not match the reviewed source allowlist")
    if int(item["actualDurationSeconds"]) < MIN_FEATURE_SECONDS:
        raise ValueError("Measured runtime is below the feature-length threshold")
    return items


def probe_duration(url: str, timeout_seconds: int = 30) -> float:
    """Probe only after validate_catalog has restricted the URL to the creator's reviewed media file."""
    executable = shutil.which("ffprobe")
    if not executable:
        raise RuntimeError("ffprobe is required for --probe")
    completed = subprocess.run(
        [executable, "-v", "error", "-rw_timeout", str(timeout_seconds * 1_000_000),
         "-show_entries", "format=duration", "-of", "json", url],
        capture_output=True,
        text=True,
        timeout=timeout_seconds + 5,
        check=True,
    )
    return float(json.loads(completed.stdout)["format"]["duration"])


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", default="licensed_content.json")
    parser.add_argument("--probe", action="store_true", help="also ffprobe the exact allowlisted creator-hosted media URL")
    args = parser.parse_args()
    data = json.loads(Path(args.input).read_text(encoding="utf-8"))
    items = validate_catalog(data)
    item = items[0]
    print(f"Catalog OK: {item['title']} · {item['actualDurationSeconds']} seconds · {item['licenseName']}")
    if args.probe:
        measured = probe_duration(item["videoUrl"])
        if round(measured) != EXPECTED["actualDurationSeconds"]:
            raise SystemExit(f"Runtime changed: expected {EXPECTED['actualDurationSeconds']}s, got {measured:.3f}s")
        print(f"Live ffprobe OK: {measured:.3f} seconds")


if __name__ == "__main__":
    main()
