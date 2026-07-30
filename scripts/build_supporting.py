#!/usr/bin/env python3
"""
Generate supporting JSON files referenced by ApiService.kt from the MovieBox catalog:
  - trending.json        : List[MovieItem] (top movies, sd/hd preferred)
  - popular_dramas.json  : List[MovieItem] (drama/fantasy/TV series)
  - search.json          : SearchResponse { results: List[MovieItem] } (all items)

These mirror content.json MovieItem schema.
"""
import json

CONTENT = "content.json"  # already built

def main():
    data = json.load(open(CONTENT, encoding="utf-8"))
    featured = data["featured"]
    categories = data["categories"]
    by_name = {c["name"]: c for c in categories}
    all_items = []
    seen = set()
    for c in categories:
        for it in c["items"]:
            if it["id"] not in seen:
                seen.add(it["id"])
                all_items.append(it)

    def strip(it):
        """Return a clean MovieItem dict matching MovieItem.kt schema."""
        return {
            "id": it["id"],
            "title": it["title"],
            "thumbnailUrl": it.get("thumbnailUrl", ""),
            "backdrop": it.get("backdrop", it.get("thumbnailUrl", "")),
            "description": it.get("description", ""),
            "rating": it.get("rating", "8.0"),
            "duration": it.get("duration", ""),
            "videoUrl": it["videoUrl"],
            "category": it.get("category", "Movies"),
            "year": it.get("year", ""),
            "quality": it.get("quality", "HD"),
        }

    # Trending: prefer recent + sd/hd; take 30
    def trending_score(it):
        y = int(it.get("year")) if str(it.get("year","")).isdigit() else 2000
        q = 2 if "-sd.mp4" in it["videoUrl"] else 1
        return (y, q)
    trending = [strip(it) for it in sorted(all_items, key=trending_score, reverse=True)[:30]]
    json.dump(trending, open("trending.json", "w", encoding="utf-8"), ensure_ascii=False, indent=2)
    print(f"[*] trending.json: {len(trending)} items")

    # Popular Dramas: Drama / Sci-Fi & Fantasy / TV Series
    pool = []
    for nm in ["Drama", "Sci-Fi & Fantasy", "TV Series"]:
        if nm in by_name:
            pool += by_name[nm]["items"]
    # dedupe
    seen2 = set(); popd = []
    for it in pool:
        if it["id"] in seen2: continue
        seen2.add(it["id"]); popd.append(strip(it))
    popd = popd[:25]
    json.dump(popd, open("popular_dramas.json", "w", encoding="utf-8"), ensure_ascii=False, indent=2)
    print(f"[*] popular_dramas.json: {len(popd)} items")

    # search.json: SearchResponse with results = all unique items
    search = {"results": [strip(it) for it in all_items]}
    json.dump(search, open("search.json", "w", encoding="utf-8"), ensure_ascii=False, indent=2)
    print(f"[*] search.json: {len(search['results'])} results")

if __name__ == "__main__":
    main()
