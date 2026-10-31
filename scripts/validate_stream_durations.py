#!/usr/bin/env python3
"""Measure direct media runtimes with ffprobe; never trust title-page runtime metadata."""
from __future__ import annotations

import argparse
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime, timezone
import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
from typing import Any
from urllib.parse import urlparse

DEFAULT_MIN_FEATURE_SECONDS = 45 * 60


def format_duration_seconds(seconds: int | float) -> str:
    total = max(0, int(round(float(seconds))))
    hours, remainder = divmod(total, 3600)
    minutes, secs = divmod(remainder, 60)
    if hours:
        if minutes == 0 and secs == 0:
            return f"{hours} h"
        if minutes == 0:
            return f"{hours} h {secs} sec"
        return f"{hours} h {minutes} min" if secs == 0 else f"{hours} h {minutes} min {secs} sec"
    if minutes:
        return f"{minutes} min" if secs == 0 else f"{minutes} min {secs} sec"
    return f"{secs} sec"


def _is_direct_mp4(url: str) -> bool:
    parsed = urlparse(url)
    return parsed.scheme in ("http", "https") and bool(parsed.netloc) and parsed.path.lower().endswith(".mp4")


def collect_streams(data: dict[str, Any]) -> dict[str, dict[str, Any]]:
    """Return one representative title per unique MP4 URL, including featured entries."""
    representatives: dict[str, dict[str, Any]] = {}
    all_items = [data.get("featured")] + [
        item for category in data.get("categories", []) for item in category.get("items", [])
    ]
    for item in all_items:
        if not isinstance(item, dict):
            continue
        url = str(item.get("videoUrl") or "").strip()
        if _is_direct_mp4(url):
            representatives.setdefault(url, item)
    return representatives


def probe_stream_duration(url: str, timeout_seconds: int = 15) -> dict[str, Any]:
    """Ask ffprobe for the media container duration with a bounded network timeout."""
    executable = shutil.which("ffprobe")
    if not executable:
        return {"actualDurationSeconds": None, "error": "ffprobe executable is not installed"}
    try:
        result = subprocess.run(
            [
                executable,
                "-v", "error",
                "-rw_timeout", str(int(timeout_seconds * 1_000_000)),
                "-show_entries", "format=duration",
                "-of", "json",
                url,
            ],
            capture_output=True,
            text=True,
            timeout=timeout_seconds + 3,
            check=False,
        )
    except subprocess.TimeoutExpired:
        return {"actualDurationSeconds": None, "error": f"ffprobe timed out after {timeout_seconds}s"}
    except OSError as exc:
        return {"actualDurationSeconds": None, "error": str(exc)[:300]}

    if result.returncode != 0:
        return {
            "actualDurationSeconds": None,
            "error": (result.stderr.strip() or f"ffprobe exited {result.returncode}")[:300],
        }
    try:
        raw_duration = json.loads(result.stdout).get("format", {}).get("duration")
        duration = float(raw_duration)
        if duration <= 0:
            raise ValueError("non-positive duration")
        return {"actualDurationSeconds": int(round(duration)), "error": None}
    except (TypeError, ValueError, json.JSONDecodeError) as exc:
        return {"actualDurationSeconds": None, "error": f"duration unavailable: {exc}"[:300]}


def probe_streams(
    streams: dict[str, dict[str, Any]], workers: int = 4, timeout_seconds: int = 15
) -> dict[str, dict[str, Any]]:
    """Probe unique URLs with bounded concurrency and periodic progress output."""
    results: dict[str, dict[str, Any]] = {}
    if not streams:
        return results
    worker_count = max(1, min(int(workers), 8))
    with ThreadPoolExecutor(max_workers=worker_count) as pool:
        futures = {
            pool.submit(probe_stream_duration, url, timeout_seconds): url for url in streams
        }
        for done, future in enumerate(as_completed(futures), start=1):
            url = futures[future]
            try:
                results[url] = future.result()
            except Exception as exc:  # defensive: a failed probe never becomes a verified runtime
                results[url] = {"actualDurationSeconds": None, "error": str(exc)[:300]}
            if done % 20 == 0 or done == len(futures):
                measured = sum(r.get("actualDurationSeconds") is not None for r in results.values())
                print(f"Probed {done}/{len(futures)} unique streams; measured={measured}", flush=True)
    return results


def build_audit_report(
    streams: dict[str, dict[str, Any]],
    results: dict[str, dict[str, Any]],
    min_feature_seconds: int = DEFAULT_MIN_FEATURE_SECONDS,
    source_file: str = "content.json",
) -> dict[str, Any]:
    entries = []
    for url, item in sorted(streams.items()):
        result = results.get(url, {})
        seconds = result.get("actualDurationSeconds")
        entries.append({
            "id": item.get("id"),
            "title": item.get("title"),
            "year": item.get("year"),
            "catalogDuration": item.get("duration"),
            "videoUrl": url,
            "actualDurationSeconds": seconds,
            "actualDuration": format_duration_seconds(seconds) if seconds is not None else None,
            "featureLength": seconds >= min_feature_seconds if seconds is not None else None,
            "error": result.get("error"),
        })
    measured = [entry for entry in entries if entry["actualDurationSeconds"] is not None]
    return {
        "sourceFile": source_file,
        "measuredAtUtc": datetime.now(timezone.utc).isoformat(),
        "measurement": "ffprobe format.duration from the direct media URL",
        "minimumFeatureLengthSeconds": min_feature_seconds,
        "summary": {
            "uniqueStreams": len(entries),
            "measured": len(measured),
            "unverified": len(entries) - len(measured),
            "featureLength": sum(entry["featureLength"] is True for entry in entries),
            "shorterThanFeatureLength": sum(entry["featureLength"] is False for entry in entries),
        },
        "streams": entries,
    }


def apply_verified_durations(
    data: dict[str, Any],
    results: dict[str, dict[str, Any]],
    min_feature_seconds: int = DEFAULT_MIN_FEATURE_SECONDS,
) -> tuple[dict[str, Any], dict[str, int]]:
    """Write measured runtimes, separate short clips, and drop only unverified streams."""
    summary = {
        "featureRowsKept": 0,
        "shortClipsKept": 0,
        "duplicateShortRowsCollapsed": 0,
        "unverifiedRowsRemoved": 0,
    }
    categories = []
    short_clips: dict[str, dict[str, Any]] = {}
    for category in data.get("categories", []):
        kept_items = []
        for item in category.get("items", []):
            url = str(item.get("videoUrl") or "").strip()
            actual = results.get(url, {}).get("actualDurationSeconds")
            if actual is None or int(actual) <= 0:
                summary["unverifiedRowsRemoved"] += 1
                continue
            verified = dict(item)
            verified["actualDurationSeconds"] = int(actual)
            verified["duration"] = format_duration_seconds(actual)
            if actual >= min_feature_seconds:
                verified["contentType"] = "feature"
                kept_items.append(verified)
                summary["featureRowsKept"] += 1
            else:
                verified["contentType"] = "short_clip"
                verified["category"] = "Short Clips"
                if url in short_clips:
                    summary["duplicateShortRowsCollapsed"] += 1
                else:
                    short_clips[url] = verified
                    summary["shortClipsKept"] += 1
        if kept_items:
            updated_category = dict(category)
            updated_category["items"] = kept_items
            categories.append(updated_category)

    if short_clips:
        categories.append({
            "id": "cat_short_clips",
            "name": "Short Clips",
            "icon": "video",
            "items": list(short_clips.values()),
        })

    featured = data.get("featured")
    featured_url = str((featured or {}).get("videoUrl") or "").strip()
    featured_actual = results.get(featured_url, {}).get("actualDurationSeconds")
    if featured_actual is not None and featured_actual >= min_feature_seconds:
        featured = dict(featured)
        featured["actualDurationSeconds"] = int(featured_actual)
        featured["duration"] = format_duration_seconds(featured_actual)
        featured["contentType"] = "feature"
    else:
        featured = next(
            (item for category in categories if "movie" in category.get("name", "").lower() for item in category["items"]),
            None,
        )

    updated_data = dict(data)
    updated_data["featured"] = featured
    updated_data["categories"] = categories
    return updated_data, summary


def write_json_atomic(path: Path, data: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    fd, temp_name = tempfile.mkstemp(prefix=path.name + ".", suffix=".tmp", dir=path.parent)
    try:
        with os.fdopen(fd, "w", encoding="utf-8") as output:
            json.dump(data, output, ensure_ascii=False, indent=2)
            output.write("\n")
        os.replace(temp_name, path)
    finally:
        if os.path.exists(temp_name):
            os.unlink(temp_name)


def verify_raw_items(
    items: list[dict[str, Any]],
    workers: int = 4,
    timeout_seconds: int = 15,
    min_feature_seconds: int = DEFAULT_MIN_FEATURE_SECONDS,
) -> tuple[list[dict[str, Any]], dict[str, Any]]:
    """Shared import pipeline: measure direct streams, classify shorts, fail closed on errors."""
    data = {"featured": None, "categories": [{"name": "Raw", "items": items}]}
    streams = collect_streams(data)
    results = probe_streams(streams, workers=workers, timeout_seconds=timeout_seconds)
    report = build_audit_report(streams, results, min_feature_seconds, source_file="data/moviebox_raw.json")
    verified = []
    for item in items:
        url = str(item.get("videoUrl") or "").strip()
        actual = results.get(url, {}).get("actualDurationSeconds")
        if actual is not None and int(actual) > 0:
            copy = dict(item)
            copy["actualDurationSeconds"] = int(actual)
            copy["duration"] = format_duration_seconds(actual)
            copy["contentType"] = "feature" if actual >= min_feature_seconds else "short_clip"
            verified.append(copy)
    return verified, report


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", default="content.json", help="catalog JSON to audit")
    parser.add_argument("--output", default="data/stream_duration_audit.json", help="JSON audit report path")
    parser.add_argument("--workers", type=int, default=4, help="bounded concurrent ffprobe calls (1-8)")
    parser.add_argument("--timeout", type=int, default=15, help="per-stream timeout in seconds")
    parser.add_argument("--min-feature-minutes", type=int, default=45, help="minimum measured duration to retain")
    parser.add_argument("--apply", action="store_true", help="replace input with verified feature-length streams")
    parser.add_argument("--apply-report", help="reuse a previously saved audit report instead of probing again")
    args = parser.parse_args()

    source_path = Path(args.input)
    data = json.loads(source_path.read_text(encoding="utf-8"))
    threshold = max(1, args.min_feature_minutes) * 60
    if args.apply_report:
        report = json.loads(Path(args.apply_report).read_text(encoding="utf-8"))
        threshold = int(report.get("minimumFeatureLengthSeconds", threshold))
        results = {
            entry["videoUrl"]: {
                "actualDurationSeconds": entry.get("actualDurationSeconds"),
                "error": entry.get("error"),
            }
            for entry in report.get("streams", [])
        }
        print(f"Reusing audit report: {json.dumps(report.get('summary', {}), sort_keys=True)}")
    else:
        streams = collect_streams(data)
        results = probe_streams(streams, workers=args.workers, timeout_seconds=args.timeout)
        report = build_audit_report(streams, results, threshold, source_file=str(source_path))
        write_json_atomic(Path(args.output), report)
        print(f"Audit summary: {json.dumps(report['summary'], sort_keys=True)}")

    if args.apply:
        updated, summary = apply_verified_durations(data, results, threshold)
        write_json_atomic(source_path, updated)
        print(f"Catalog applied: {json.dumps(summary, sort_keys=True)}")


if __name__ == "__main__":
    main()
