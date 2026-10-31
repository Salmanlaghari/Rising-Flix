# Rising Flix

Rising Flix is an Android video catalog app built with Kotlin, Jetpack Compose, Retrofit, WorkManager, Coil, and Media3/ExoPlayer.

## Content sources and playback policy

In-app playback is fail-closed: the current curated feed contains one creator-authorized full-length film, **Valkaama**, hosted by its official project site under **Creative Commons Attribution-ShareAlike 3.0**. The app allowlists its exact media URL and reviewed runtime; a title-page claim alone cannot authorize playback. The observed media is 5,585.960 seconds (about 93 minutes 6 seconds), H.264 1280×720 with AAC audio. The app displays the measured runtime and shows source/license links and attribution on the detail screen. The thumbnail is a resized still from the film.

The MovieBox snapshot is retained only as a set of detail-page links. Tapping one opens the original MovieBox page in the browser; the app does not scrape MovieBox pages at runtime, extract its media URLs, download those videos, or play them in-app. Its existing catalog snapshot remains available offline, but it is not refreshed because no authorized MovieBox API or permission for crawler-based refresh was found. Do not add provider media URLs to the licensed feed without a documented rights basis and measured full-length media.

There is currently only one reviewed licensed feature, not enough to claim a large full-movie catalog. Finding additional creator-authorized, feature-length sources is required before the app can offer more licensed movies.

## Automatic sync and offline behavior

At startup the app schedules a unique WorkManager catch-up task and a network-constrained periodic refresh (approximately every 12 hours; Android may defer it for battery use). It tries this PR branch's repository-maintained `licensed_content.json` feed first so an unmerged build can sync, then falls back to the same feed on `main`. A refresh is accepted only when the entry matches the app's reviewed allowlist, license, source page, media URL, and measured runtime. The MovieBox page-link snapshot and the licensed film are bundled for first launch/offline use; successful refreshes are saved in the app-private `content_catalog_cache.json` file. A failed refresh leaves the last safe snapshot available. Existing downloads, preferences, and other app data are not changed by catalog migration.

After this PR is merged or its feature branch is deleted, the app falls back to the `main` feed. If neither repository endpoint is reachable, it keeps using the bundled or last-known-good cache and WorkManager retries later.

## Maintainer files

- `licensed_content.json`: the single curated licensed feature and its provenance.
- `content.json`, `trending.json`, `popular_dramas.json`, `search.json`: legacy-compatible MovieBox page-link indexes; no extracted media URLs.
- `app/src/main/assets/moviebox_links.json`: bundled page-only snapshot for offline browsing.
- `app/src/main/assets/licensed_content.json`: bundled copy of the curated feed.
- `scripts/build_content.py`: deterministic sanitizer/package builder for checked-in data only; it makes no network requests.
- `scripts/verify_licensed_catalog.py`: validates the explicit source allowlist and can optionally re-probe the approved media with `--probe`.

The checked-in MovieBox title/poster snapshot is a legacy dataset, not an authorization to copy or stream new provider data. No live scraper or extracted stream URL is part of the app's content flow.

## Build and test

Use JDK 17 and Android SDK 34 or newer:

```bash
./gradlew testDebugUnitTest assembleDebug
python3 -m unittest discover -s scripts -p 'test_*.py' -v
python3 scripts/verify_licensed_catalog.py
```

For a live runtime check of the allowlisted creator-hosted file, run:

```bash
python3 scripts/verify_licensed_catalog.py --probe
```

The app streams the 720p Matroska file from the creator's server; its roughly 4.56 GB size means playback quality depends on connection speed and device support. No physical-device playback has been certified.
