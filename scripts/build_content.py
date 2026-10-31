#!/usr/bin/env python3
"""Build safe catalogs from checked-in data only; performs no MovieBox requests or stream extraction."""
from __future__ import annotations

import json
import re
import shutil
from pathlib import Path
from typing import Any
from urllib.parse import urlparse

from verify_licensed_catalog import validate_catalog

ROOT = Path(__file__).resolve().parents[1]
CURRENT_BASE = "https://themoviebox.xyz"
ACCEPTED_HOSTS = {"themoviebox.xyz", "www.themoviebox.xyz", "moviebox.pk", "www.moviebox.pk"}
SLUG_RE = re.compile(r"^[A-Za-z0-9._~-]+$")
STREAM_KEYS = {"streamurl", "streamingurl", "downloadurl", "playbackurl", "contenturl", "url"}


def resolve_moviebox_page(value: Any) -> str | None:
    raw = str(value or "").strip()
    if not raw:
        return None
    parsed = urlparse(raw if "://" in raw else "")
    if parsed.scheme:
        if parsed.scheme.lower() not in {"http", "https"} or (parsed.hostname or "").lower() not in ACCEPTED_HOSTS:
            return None
        path = parsed.path.rstrip("/")
    else:
        path = "/" + raw.lstrip("/")
    if path.lower().startswith("/detail/"):
        slug = path[len("/detail/"):]
    elif path.lower().startswith("/moviedetail/"):
        slug = path[len("/moviedetail/"):]
    elif not parsed.scheme and "/" not in path.strip("/"):
        slug = path.strip("/")
    else:
        return None
    slug = slug.strip("/")
    if not slug or not SLUG_RE.fullmatch(slug):
        return None
    return f"{CURRENT_BASE}/detail/{slug}"


def page_for_item(item: dict[str, Any]) -> str:
    for key in ("sourcePageUrl", "detailUrl", "detailPath", "videoUrl"):
        page = resolve_moviebox_page(item.get(key))
        if page:
            return page
    return ""


def sanitize_moviebox_item(item: dict[str, Any], category_name: str | None = None) -> dict[str, Any]:
    clean = dict(item)
    page = page_for_item(item)
    for key in list(clean):
        normalized = re.sub(r"[^a-z]", "", key.lower())
        if normalized in STREAM_KEYS and key not in {"videoUrl", "sourcePageUrl"}:
            clean.pop(key, None)
    clean["videoUrl"] = page
    clean["sourcePageUrl"] = page or None
    clean["sourceName"] = "MovieBox"
    clean["contentType"] = "details_only"
    clean["actualDurationSeconds"] = None
    clean["duration"] = "Runtime unverified"
    if category_name:
        clean["category"] = category_name
    for key in ("licenseName", "licenseUrl", "attribution"):
        clean.pop(key, None)
    return clean


def _atomic_json(path: Path, value: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix(path.suffix + ".tmp")
    temporary.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    temporary.replace(path)


def flatten_rows(content: dict[str, Any]) -> list[dict[str, Any]]:
    rows: list[dict[str, Any]] = []
    for category in content.get("categories", []):
        if isinstance(category, dict):
            rows.extend(item for item in category.get("items", []) if isinstance(item, dict))
    return rows


def sanitize_content(content: dict[str, Any], licensed: dict[str, Any]) -> dict[str, Any]:
    result = dict(content)
    result["featured"] = licensed.get("featured")
    categories = []
    for category in content.get("categories", []):
        if not isinstance(category, dict):
            continue
        clean_category = dict(category)
        clean_category["items"] = [
            sanitize_moviebox_item(item, str(category.get("name") or "Movies"))
            for item in category.get("items", []) if isinstance(item, dict)
        ]
        categories.append(clean_category)
    result["categories"] = categories
    return result


def unique_page_items(content: dict[str, Any]) -> list[dict[str, Any]]:
    by_page: dict[str, dict[str, Any]] = {}
    for row in flatten_rows(content):
        item = sanitize_moviebox_item(row)
        page = item.get("sourcePageUrl")
        if page:
            by_page.setdefault(page, item)
    return list(by_page.values())


def build_supporting_catalogs(content: dict[str, Any]) -> None:
    root = ROOT
    all_items = unique_page_items(content)
    categories = content.get("categories", [])
    drama_items = []
    for category in categories:
        name = str(category.get("name", ""))
        if any(term in name.lower() for term in ("drama", "fantasy", "tv")):
            drama_items.extend(sanitize_moviebox_item(item) for item in category.get("items", []) if isinstance(item, dict))
    drama_unique = {item["sourcePageUrl"]: item for item in drama_items if item.get("sourcePageUrl")}
    _atomic_json(root / "trending.json", {
        "featured": None,
        "categories": [{"id": "moviebox_trending_links", "name": "MovieBox · Trending Pages", "icon": "open_in_new", "items": all_items[:30]}],
    })
    _atomic_json(root / "popular_dramas.json", {
        "featured": None,
        "categories": [{"id": "moviebox_drama_links", "name": "MovieBox · Drama Pages", "icon": "open_in_new", "items": list(drama_unique.values())[:25]}],
    })
    _atomic_json(root / "search.json", {"results": all_items})
    _atomic_json(root / "data/moviebox_content.json", all_items)


def build() -> None:
    licensed_path = ROOT / "licensed_content.json"
    licensed = json.loads(licensed_path.read_text(encoding="utf-8"))
    validate_catalog(licensed)

    content_path = ROOT / "content.json"
    original = json.loads(content_path.read_text(encoding="utf-8"))
    sanitized = sanitize_content(original, licensed)
    _atomic_json(content_path, sanitized)

    # This asset retains the existing 446-row/category listing snapshot as page links only.
    moviebox_links = {"featured": None, "categories": sanitized.get("categories", [])}
    _atomic_json(ROOT / "app/src/main/assets/moviebox_links.json", moviebox_links)
    shutil.copyfile(licensed_path, ROOT / "app/src/main/assets/licensed_content.json")

    # Sanitize flat snapshots too; never leave extracted media URLs in generated catalogs.
    for relative in ("data/themoviebox_content.json",):
        path = ROOT / relative
        if path.exists():
            value = json.loads(path.read_text(encoding="utf-8"))
            rows = value if isinstance(value, list) else value.get("results", [])
            _atomic_json(path, [sanitize_moviebox_item(row) for row in rows if isinstance(row, dict)])

    build_supporting_catalogs(sanitized)
    print(f"Built licensed feed and {sum(len(c.get('items', [])) for c in sanitized.get('categories', []))} MovieBox page-only rows")


if __name__ == "__main__":
    build()
