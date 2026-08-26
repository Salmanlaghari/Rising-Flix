#!/usr/bin/env python3
"""
Scrape themoviebox.xyz metadata from Wayback Machine captures.
"""
import json
import re
import urllib.request
import urllib.error
import time
import sys
import os

WORKSPACE = "/workspace/fabbb288-6c6d-4298-a0e2-93258e85cc1d/sessions/agent_26ade8cd-0f08-4994-afd0-c7d6fed7cb80/Rising-flix"
CDX_FILE = "/tmp/tmb_cdx.json"
OUTPUT_FILE = "/tmp/themoviebox_metadata.json"

def fetch_url(url, timeout=30):
    """Fetch URL using urllib"""
    try:
        req = urllib.request.Request(url, headers={
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'
        })
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            return resp.read().decode('utf-8', errors='ignore')
    except Exception as e:
        print(f"  ! fetch error {url}: {e}", file=sys.stderr)
        return None

def extract_metadata(html, url):
    """Extract metadata from detail page HTML"""
    meta = {
        "source_url": url,
        "title": "",
        "year": "",
        "rating": "",
        "genre": "",
        "country": "",
        "description": "",
        "cast": [],
        "rating_score": "",
        "poster_url": "",
        "backdrop_url": "",
        "detail_url": "https://themoviebox.xyz/detail/" + url.split('/detail/')[-1] if '/detail/' in url else url
    }
    
    # Extract title from title tag
    title_match = re.search(r'<title[^>]*>(.*?)</title>', html, re.DOTALL | re.IGNORECASE)
    if title_match:
        title = title_match.group(1).strip()
        title = re.sub(r'\s+', ' ', title)
        # Remove common suffixes
        title = re.sub(r'\s*[-|]\s*MovieBox.*$', '', title)
        title = re.sub(r'\s*[-|]\s*Watch.*$', '', title)
        meta["title"] = title.strip()
    
    # Convert HTML to text
    text = re.sub(r'<[^>]+>', ' ', html)
    text = re.sub(r'\s+', ' ', text)
    
    # Extract year (4 digits between 1900-2099)
    year_match = re.search(r'\b(19|20)\d{2}\b', text)
    if year_match:
        meta["year"] = year_match.group(0)
    
    # Extract content rating
    rating_match = re.search(r'\b(R|PG-13|PG|G|NC-17|TV-MA|TV-14|TV-PG)\b', text)
    if rating_match:
        meta["rating"] = rating_match.group(1)
    
    # Extract genre from known list
    genres = ["Action", "Adventure", "Animation", "Anime", "Comedy", "Crime", "Drama", "Fantasy", "Horror", "Mystery", "Romance", "Sci-Fi", "Thriller", "Western", "Reality-TV", "Game-show"]
    found_genres = []
    for genre in genres:
        if re.search(r'\b' + genre + r'\b', text, re.IGNORECASE):
            found_genres.append(genre)
    if found_genres:
        meta["genre"] = ", ".join(found_genres)
    
    # Extract country
    countries = ["USA", "United States", "UK", "United Kingdom", "Canada", "Australia", "India", "Japan", "Korea", "China", "France", "Germany", "Nigeria", "Nollywood"]
    for country in countries:
        if re.search(r'\b' + country + r'\b', text, re.IGNORECASE):
            meta["country"] = country
            break
    
    # Extract description
    # Look for text after the title/year/genre line
    desc_patterns = [
        r'(?:R\s+)?(?:Canada|USA|UK|Australia|India|Japan|Korea|China|France|Germany|Nigeria)?\s*(?:Action|Adventure|Animation|Comedy|Crime|Drama|Fantasy|Horror|Mystery|Romance|Sci-Fi|Thriller)(?:\s*,\s*(?:Action|Adventure|Animation|Comedy|Crime|Drama|Fantasy|Horror|Mystery|Romance|Sci-Fi|Thriller))*\s*([A-Z][^.]*(?:\.[^A-Z][^.]*){2,10})',
        r'When\s+(?:an|a|the)\s+[^.]*(?:discovers|sets|finds|gets|is|goes|travels|uses)[^.]*\.',
    ]
    for pattern in desc_patterns:
        desc_match = re.search(pattern, text, re.IGNORECASE)
        if desc_match:
            desc = desc_match.group(1) if desc_match.lastindex >= 1 else desc_match.group(0)
            if 30 < len(desc) < 500:
                meta["description"] = desc.strip()
                break
    
    # Extract cast
    cast_match = re.findall(r'([A-Z][a-z]+(?:\s+[A-Z][a-z]+)+)\s*\(([^)]+)\)', text)
    if cast_match:
        meta["cast"] = [f"{name} as {char}" for name, char in cast_match[:10]]
    
    # Extract rating score
    score_match = re.search(r'(\d+\.?\d*)\s*/\s*(\d+)', text)
    if score_match:
        meta["rating_score"] = f"{score_match.group(1)}/{score_match.group(2)}"
    
    # Extract image URLs
    img_matches = re.findall(r'https?://[^\s"\'<>]+\.(jpg|jpeg|png|webp)', text)
    if img_matches:
        meta["poster_url"] = img_matches[0]
        if len(img_matches) > 1:
            meta["backdrop_url"] = img_matches[1]
    
    return meta

def main():
    print("[*] Loading CDX data...")
    with open(CDX_FILE) as f:
        cdx_data = json.load(f)
    
    rows = cdx_data[1:]  # Skip header
    seen = set()
    items = []
    
    for row in rows:
        ts, url, status = row
        if status != '200':
            continue
        
        slug = url.split('/detail/')[-1]
        if slug in seen:
            continue
        seen.add(slug)
        
        wayback_url = f"https://web.archive.org/web/{ts}/{url}"
        print(f"[*] Fetching: {slug}")
        
        html = fetch_url(wayback_url)
        if not html:
            continue
        
        meta = extract_metadata(html, url)
        if meta["title"]:
            items.append(meta)
            print(f"    -> {meta['title']} ({meta['year']})")
        
        time.sleep(0.5)
    
    print(f"\n[*] Extracted {len(items)} items")
    
    os.makedirs(os.path.dirname(OUTPUT_FILE), exist_ok=True)
    with open(OUTPUT_FILE, 'w', encoding='utf-8') as f:
        json.dump(items, f, indent=2, ensure_ascii=False)
    
    print(f"[*] Saved to {OUTPUT_FILE}")

if __name__ == "__main__":
    main()
