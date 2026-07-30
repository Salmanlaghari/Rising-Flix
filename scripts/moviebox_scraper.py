#!/usr/bin/env python3
"""
MovieBox.pk -> Rising-Flix content scraper.

Stage 1: Collect moviedetail links from listing pages.
Stage 2: Scrape each detail page, extract JSON-LD VideoObject + og meta.
Stage 3: Save raw scraped catalog to data/moviebox_raw.json
"""
import re
import json
import time
import sys
import subprocess
import os
from concurrent.futures import ThreadPoolExecutor, as_completed

UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"

def curl_get(url, timeout=30):
    """Fetch a URL using curl, return text or None."""
    try:
        r = subprocess.run(
            ["curl", "-s", "-A", UA, "--max-time", str(timeout),
             "-H", "Accept: text/html,application/xhtml+xml",
             "-H", "Accept-Language: en-US,en;q=0.9", url],
            capture_output=True, text=True, timeout=timeout + 10
        )
        return r.stdout if r.stdout else None
    except Exception as e:
        print(f"  ! fetch error {url}: {e}", file=sys.stderr)
        return None

# ---------- Stage 1: collect detail links ----------
LISTING_PAGES = {
    "Movies": "https://moviebox.pk/web/movie",
    "TV Series": "https://moviebox.pk/web/tv-series",
    "Animation": "https://moviebox.pk/web/animated-series",
}

LINK_RE = re.compile(r'href="(/moviedetail/[^"]+)"')

def collect_links():
    """Return dict category -> list of {url, slug}."""
    catalog = {}
    for cat, url in LISTING_PAGES.items():
        print(f"[*] Fetching listing page: {cat} ({url})")
        html = curl_get(url)
        if not html:
            print(f"  ! failed to fetch {url}", file=sys.stderr)
            continue
        links = sorted(set(LINK_RE.findall(html)))
        # filter out non-detail noise
        links = [l for l in links if l.startswith("/moviedetail/")]
        print(f"  -> {len(links)} detail links found")
        catalog[cat] = [{"url": "https://moviebox.pk" + l, "slug": l.split("/")[-1]} for l in links]
    return catalog

# ---------- Stage 2: scrape detail pages ----------
JSONLD_RE = re.compile(r'<script type="application/ld\+json">({.*?})</script>', re.S)
OG_RE = {
    "og:title": re.compile(r'<meta[^>]+property="og:title"[^>]+content="([^"]*)"'),
    "og:image": re.compile(r'<meta[^>]+property="og:image"[^>]+content="([^"]*)"'),
    "og:description": re.compile(r'<meta[^>]+property="og:description"[^>]+content="([^"]*)"'),
}
# Extract year + rating + genres from the detail page metadata blocks
YEAR_RE = re.compile(r'<h1[^>]*>([^<]+)</h1>')  # not used directly

def parse_detail(html, url, slug, category):
    if not html:
        return None
    m = JSONLD_RE.search(html)
    if not m:
        return None
    try:
        ld = json.loads(m.group(1))
    except Exception:
        return None

    name = ld.get("name") or ""
    desc = ld.get("description") or ""
    content_url = ld.get("contentUrl") or ""
    thumb = ""
    if isinstance(ld.get("thumbnailUrl"), list) and ld["thumbnailUrl"]:
        thumb = ld["thumbnailUrl"][0]
    elif isinstance(ld.get("thumbnailUrl"), str):
        thumb = ld["thumbnailUrl"]
    upload = ld.get("uploadDate") or ""
    duration_sec = ld.get("duration") or 0

    # og:image is usually a nicer poster
    og = {}
    for k, rx in OG_RE.items():
        mm = rx.search(html)
        if mm:
            og[k] = mm.group(1)
    poster = og.get("og:image") or thumb or ""

    # year from uploadDate or og:title
    year = ""
    if upload and re.match(r"\d{4}", upload):
        year = upload[:4]
    if not year:
        ym = re.search(r"\b(19\d{2}|20\d{2})\b", og.get("og:title", ""))
        if ym:
            year = ym.group(1)

    # duration -> "X h Y m" or "Y min"
    duration_str = ""
    try:
        ds = int(duration_sec)
        if ds > 0:
            h, rem = divmod(ds, 3600)
            mm_, ss = divmod(rem, 60)
            if h:
                duration_str = f"{h} h {mm_} m" if mm_ else f"{h} h"
            else:
                duration_str = f"{mm_} min"
    except Exception:
        pass

    if not content_url or not name:
        return None

    return {
        "id": slug,
        "title": name.strip(),
        "category": category,
        "videoUrl": content_url,
        "streaming_url": content_url,
        "thumbnailUrl": poster,
        "poster_image_url": poster,
        "backdrop": poster,
        "description": desc.strip(),
        "year": year,
        "duration": duration_str,
        "duration_sec": duration_sec,
        "uploadDate": upload,
        "detailUrl": url,
        "source": "moviebox.pk",
    }

def scrape_all(catalog):
    """Scrape every detail link. Returns list of parsed items."""
    # Build a flat list of (category, url, slug)
    tasks = []
    for cat, items in catalog.items():
        for it in items:
            tasks.append((cat, it["url"], it["slug"]))

    results = []
    print(f"[*] Scraping {len(tasks)} detail pages with 8 workers...")
    done = 0

    def worker(t):
        cat, url, slug = t
        html = curl_get(url, timeout=25)
        return parse_detail(html, url, slug, cat)

    with ThreadPoolExecutor(max_workers=8) as ex:
        futs = {ex.submit(worker, t): t for t in tasks}
        for fut in as_completed(futs):
            done += 1
            res = fut.result()
            if res:
                results.append(res)
            if done % 25 == 0 or done == len(tasks):
                print(f"  [{done}/{len(tasks)}] parsed={len(results)}", flush=True)
    return results

def main():
    catalog = collect_links()
    total = sum(len(v) for v in catalog.values())
    print(f"[*] Total detail links to scrape: {total}")

    items = scrape_all(catalog)
    print(f"[*] Successfully parsed {len(items)} items with streaming URLs")

    # dedupe by videoUrl, keep first
    seen = set()
    deduped = []
    for it in items:
        if it["videoUrl"] in seen:
            continue
        seen.add(it["videoUrl"])
        deduped.append(it)
    print(f"[*] After dedupe by videoUrl: {len(deduped)}")

    os.makedirs("data", exist_ok=True)
    with open("data/moviebox_raw.json", "w", encoding="utf-8") as f:
        json.dump(deduped, f, ensure_ascii=False, indent=2)
    print(f"[*] Saved raw catalog -> data/moviebox_raw.json")

    # quick stats
    by_cat = {}
    for it in deduped:
        by_cat[it["category"]] = by_cat.get(it["category"], 0) + 1
    print("[*] Items per category:", by_cat)
    # quality check
    ld_count = sum(1 for it in deduped if "-ld.mp4" in it["videoUrl"])
    sd_count = sum(1 for it in deduped if "-sd.mp4" in it["videoUrl"])
    print(f"[*] -ld.mp4: {ld_count}, -sd.mp4: {sd_count}")

if __name__ == "__main__":
    main()
