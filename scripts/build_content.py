#!/usr/bin/env python3
"""
Transform data/moviebox_raw.json (MovieBox.pk scraped catalog)
into the Rising-Flix content.json format consumed by the Android app.

Output: content.json  (replaces repo root content.json)
        data/moviebox_content.json (cleaned catalog, kept for the app/server reference)
"""
import json
import re
import os

RAW = "data/moviebox_raw.json"
OUT_CONTENT = "content.json"
OUT_CATALOG = "data/moviebox_content.json"

def slug_id(prefix, title, idx):
    base = re.sub(r"[^a-z0-9]+", "-", title.lower()).strip("-") or "title"
    return f"{prefix}_{idx:03d}_{base[:24]}"

def quality_from_url(url):
    if "-hd.mp4" in url or "-uhd.mp4" in url:
        return "HD"
    if "-sd.mp4" in url:
        return "HD"
    if "-ld.mp4" in url:
        return "SD"
    return "HD"

def rating_for(title, year, idx):
    # Deterministic pseudo rating in 6.5-9.2 range for display purposes
    h = (sum(ord(c) for c in title) + (int(year) if year.isdigit() else 0) + idx) % 100
    return f"{6.5 + (h % 28) / 10.0:.1f}"

def to_movie_item(it, prefix, idx):
    vid = it["videoUrl"]
    poster = it.get("thumbnailUrl") or ""
    backdrop = it.get("backdrop") or poster
    desc = it.get("description") or ""
    dur = it.get("duration") or ""
    # ensure a sensible duration fallback
    if not dur:
        dur = "1 h 30 min" if it["category"] == "Movies" else "45 min"
    return {
        "id": slug_id(prefix, it["title"], idx),
        "title": it["title"],
        "category": it["category"],
        "videoUrl": vid,
        "thumbnailUrl": poster,
        "backdrop": backdrop,
        "description": desc,
        "rating": rating_for(it["title"], it.get("year", ""), idx),
        "quality": quality_from_url(vid),
        "year": it.get("year") or "2025",
        "duration": dur,
        "source": "moviebox.pk",
        "detailUrl": it.get("detailUrl", ""),
    }

def main():
    raw = json.load(open(RAW, encoding="utf-8"))
    print(f"[*] Loaded {len(raw)} raw items from {RAW}")

    # Sort: Movies first, then TV Series, then Animation; within each by title
    cat_order = ["Movies", "TV Series", "Animation"]
    by_cat = {c: [x for x in raw if x["category"] == c] for c in cat_order}
    for c in cat_order:
        by_cat[c].sort(key=lambda x: x["title"].lower())
        print(f"    {c}: {len(by_cat[c])}")

    # --- Build genre-based subcategories from Movies ---
    # Genre keywords derived from descriptions + titles
    GENRE_RULES = [
        ("Action", ["action", "fight", "warrior", "battle", "mission", "agent", "soldier", "assassin", "chase", "combat", "crime", "kidnap"]),
        ("Horror", ["horror", "scary", "ghost", "demon", "zombie", "vampire", "haunt", "terror", "slasher", "evil", "monster"]),
        ("Romance", ["romance", "love", "romantic", "relationship", "couple", "kiss", "heart", "dating", "marriage"]),
        ("Comedy", ["comedy", "funny", "humor", "laugh", "hilarious", "sitcom"]),
        ("Sci-Fi & Fantasy", ["sci-fi", "scifi", "science fiction", "fantasy", "alien", "space", "future", "robot", "android", "multiverse", "superhero", "magic", "wizard", "dragon"]),
        ("Thriller", ["thriller", "suspense", "mystery", "detective", "murder", "investigation", "conspiracy", "noir"]),
        ("Drama", ["drama", "family", "life", "emotional", "biopic", "true story"]),
        ("Animation", ["animated", "animation", "cartoon", "anime"]),
    ]

    def genre_of(it):
        text = (it.get("description") + " " + it["title"]).lower()
        for g, kws in GENRE_RULES:
            if any(k in text for k in kws):
                return g
        return None

    # Assign each movie its best genre
    movie_genre = {}
    for it in by_cat["Movies"]:
        movie_genre[it["videoUrl"]] = genre_of(it)

    # ---------- Build the categories list ----------
    categories = []

    # 1. Movies (all movies) - icon movie
    movies_items = [to_movie_item(it, "mov", i + 1) for i, it in enumerate(by_cat["Movies"])]
    categories.append({
        "id": "cat_movies",
        "name": "Movies",
        "icon": "movie",
        "items": movies_items,
    })

    # 2. Action & Thriller (movies with action or thriller genre)
    act_items = [to_movie_item(it, "act", i + 1) for i, it in enumerate(by_cat["Movies"])
                 if movie_genre[it["videoUrl"]] in ("Action", "Thriller")]
    if act_items:
        categories.append({"id": "cat_action", "name": "Action & Thriller", "icon": "star", "items": act_items})

    # 3. Sci-Fi & Fantasy
    sf_items = [to_movie_item(it, "scifi", i + 1) for i, it in enumerate(by_cat["Movies"])
                if movie_genre[it["videoUrl"]] == "Sci-Fi & Fantasy"]
    if sf_items:
        categories.append({"id": "cat_scifi", "name": "Sci-Fi & Fantasy", "icon": "rocket", "items": sf_items})

    # 4. Horror
    hor_items = [to_movie_item(it, "hor", i + 1) for i, it in enumerate(by_cat["Movies"])
                 if movie_genre[it["videoUrl"]] == "Horror"]
    if hor_items:
        categories.append({"id": "cat_horror", "name": "Horror", "icon": "ghost", "items": hor_items})

    # 5. Romance & Comedy
    rc_items = [to_movie_item(it, "rc", i + 1) for i, it in enumerate(by_cat["Movies"])
                if movie_genre[it["videoUrl"]] in ("Romance", "Comedy")]
    if rc_items:
        categories.append({"id": "cat_romance", "name": "Romance & Comedy", "icon": "heart", "items": rc_items})

    # 6. Drama
    dr_items = [to_movie_item(it, "dr", i + 1) for i, it in enumerate(by_cat["Movies"])
                if movie_genre[it["videoUrl"]] == "Drama"]
    if dr_items:
        categories.append({"id": "cat_drama", "name": "Drama", "icon": "film", "items": dr_items})

    # 7. TV Series
    tv_items = [to_movie_item(it, "tv", i + 1) for i, it in enumerate(by_cat["TV Series"])]
    if tv_items:
        categories.append({"id": "cat_tv", "name": "TV Series", "icon": "tv", "items": tv_items})

    # 8. Animation
    an_items = [to_movie_item(it, "an", i + 1) for i, it in enumerate(by_cat["Animation"])]
    if an_items:
        categories.append({"id": "cat_animation", "name": "Animation", "icon": "sparkles", "items": an_items})

    # ---------- Featured item ----------
    # Pick a high-profile title for featured: prefer a recent movie with sd quality
    featured_raw = None
    for it in by_cat["Movies"]:
        if "-sd.mp4" in it["videoUrl"] and it.get("year") and it["year"].isdigit() and int(it["year"]) >= 2020:
            featured_raw = it
            break
    if not featured_raw:
        featured_raw = by_cat["Movies"][0]
    featured = to_movie_item(featured_raw, "feat", 1)

    content = {
        "featured": featured,
        "categories": categories,
    }

    # Write content.json
    with open(OUT_CONTENT, "w", encoding="utf-8") as f:
        json.dump(content, f, ensure_ascii=False, indent=2)
    total_items = sum(len(c["items"]) for c in categories)
    print(f"[*] Wrote {OUT_CONTENT}: featured='{featured['title']}' | {len(categories)} categories | {total_items} total items")
    for c in categories:
        print(f"    - {c['name']:20} ({c['id']:14}) -> {len(c['items'])} items")

    # Also write a cleaned catalog to data/moviebox_content.json (flat list)
    flat = []
    for c in categories:
        for it in c["items"]:
            flat.append({
                "id": it["id"],
                "category": it["category"],
                "title": it["title"],
                "poster_image_url": it["thumbnailUrl"],
                "backdrop": it["backdrop"],
                "description": it["description"],
                "streaming_url": it["videoUrl"],
                "videoUrl": it["videoUrl"],
                "quality": it["quality"],
                "year": it["year"],
                "rating": float(it["rating"]),
                "duration": it["duration"],
                "source": it["source"],
                "detailUrl": it["detailUrl"],
            })
    with open(OUT_CATALOG, "w", encoding="utf-8") as f:
        json.dump(flat, f, ensure_ascii=False, indent=2)
    print(f"[*] Wrote {OUT_CATALOG}: {len(flat)} flat catalog entries")

if __name__ == "__main__":
    main()
