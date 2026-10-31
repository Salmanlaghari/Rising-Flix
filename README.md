# 🎬 Rising Flix — Premium Video Streaming App

Welcome to **Rising Flix** — the premium cinematic video streaming hub designed to play your favorite movies, dramas, sports, and cartoons all in one lightweight, modern application. Built with **Kotlin, Jetpack Compose, Retrofit, and Media3/ExoPlayer**, Rising Flix is designed from the ground up for extreme size and performance optimization, weighing in at only **4.0MB**!

---

## ✨ Key Features

- 📱 **Horizontal Cinematic Dashboard**: Elegant cards layouts containing high-quality Unsplash poster images, gold rating badges (`⭐ 9.5`), and status tags (`Live` or `2026`).
- 🎥 **Built-in Media3 ExoPlayer**: Fully customized landscape media player featuring auto-hiding overlays, seek timeline progress bars, rewind/forward 10s buttons, subtitles selection triggers, and cast-to-TV integration.
- ⏬ **Premium Offline Downloads**: Simulated background downloads managing queueing, speeds (MB/s), file sizes, and pausing/resuming controls directly from the Library Screen.
- 🚀 **Hardware Acceleration & Caching**: A last-known-good catalog is saved in app-private storage for quick startup and offline browsing, alongside hardware-accelerated rendering.
- 🔒 **Privacy Focused & Safe**: 100% compliant and safe out-of-the-box. Includes fully accessible legal documents (Privacy Policy and Terms of Service) directly inside assets.

---

## 🔄 MovieBox.pk Content Sync

Rising Flix imports candidate media URLs and title metadata from MovieBox.pk. A title's reported runtime is not proof that its linked stream is a full movie: catalog generation measures each unique direct MP4 with `ffprobe` and uses the measured length when available, while keeping the complete title catalog visible.

### Runtime validation
- `actualDurationSeconds` and the displayed duration come from `ffprobe` reading the direct media container, not the title-page's claimed runtime.
- Audit of the currently tracked feed: **300/300** unique MP4 URLs had measurable durations, but only **1** was at least 45 minutes; the other **299** are marked as short clips rather than full movies. All **446** original rows across eight genre/type categories are retained.
- Measured clips stay in their original Movies, genre, TV, or Animation categories; cards and the player identify them as **Short Clip** and show the actual measured duration. Direct streams without a successful measurement stay visible and playable but are marked **Runtime unverified** and never display the title-page runtime as if it were confirmed. Provider-only pages are marked details-only, not playable media.
- `data/stream_duration_audit.json` records each unique stream URL, its measured duration (or probe failure), and whether it passed the feature-length threshold.
- A successful duration probe confirms the media's reported length at import time; it does not guarantee the upstream host will remain reachable or allow playback later.

### Files produced
- `content.json` — primary catalog consumed by the app (`featured` + `categories[]`), schema matches `MovieItem.kt` / `ContentResponse.kt`.
- `trending.json` — up to 30 entries from the complete catalog, with measured short clips labeled (powers the Trending screen / `getTrendingMovies()`).
- `popular_dramas.json` — up to 25 Drama/Fantasy/TV entries from the complete catalog, with measured short clips labeled (powers `getPopularDramas()`).
- `search.json` — searchable index of all unique catalog streams, including measured clips and runtime-unverified entries (`SearchResponse { results[] }` / `searchMovies()`).
- `data/moviebox_content.json` — flat catalog reference, including measured runtime fields.
- `app/src/main/assets/verified_content.json` — packaged measured catalog for first launch and offline use, including before the updated remote feed is published.

### How the sync works
1. **Collect** detail-page links from MovieBox listing pages (`/web/movie`, `/web/tv-series`, `/web/animated-series`).
2. **Scrape** candidate `contentUrl` values and descriptive metadata from embedded **schema.org `VideoObject` JSON-LD**; its `duration` is treated as unverified metadata.
3. **Probe** each unique candidate stream with bounded-concurrency `ffprobe`; mark runtimes below 45 minutes as short clips, and keep probe failures visible as runtime-unverified instead of deleting catalog entries.
4. **Transform** every candidate into its original movie/genre/TV catalog rows with an explicit content/runtime label. Only a measured feature-length stream can be used to match a provider detail page to a full movie; supporting feeds retain all entries with their labels.

The scripts live under `scripts/` (`moviebox_scraper.py`, `validate_stream_durations.py`, `build_content.py`, `build_supporting.py`). `build_content.py` requires `ffprobe` and writes the per-URL report to `data/stream_duration_audit.json`.

## Automatic in-app catalog refresh

When the app starts, it schedules a one-time catch-up refresh and a unique WorkManager job that refreshes the catalog about every 12 hours while a network connection is available. Android may defer background work to conserve battery, so the interval is approximate. The maintained catalog and live title listing are fetched concurrently; the home feed no longer waits on unrelated Archive.org metadata requests or inserts demo shorts and promo clips as feature movies. All recognized direct streams can be attempted by Media3 regardless of whether their runtime was measured. Short clips show their measured duration and badge; unknown runtimes show “Runtime unverified.” Only a measured feature-length stream can be matched to a provider detail page. Successful results are stored in the app's private `content_catalog_cache.json` file. Existing downloads and other app data are not modified.

If a refresh fails or the device is offline, the app keeps the last successful catalog and shows it immediately; a new install uses the packaged complete catalog before falling back to the bundled title listing. Provider details pages are identified as not directly playable, with an option to open that page in the browser; the page is not presented as a playing video. A duration probe does not guarantee the upstream host will remain reachable or allow playback on the device.

---

## 🎨 Premium Dark & Cyan Theme

Rising Flix features a striking, clean dark design system:
- **Primary Background**: Velvet Deep Blue (`#0F172A`)
- **Accent Highlight**: Neon Cyan (`#06B6D4`)
- **Surfaces**: Slate Card Surface (`#1E293B`)
- **Shape System**: Curved rounded corners (`12dp` to `16dp`) representing modern TV dashboards.

---

## ⚙️ Architecture and Technical Stack

- **UI Framework**: Modern declarative Jetpack Compose.
- **Dependency Management**: Standard Gradle Version Catalog (`libs.versions.toml`).
- **Media Engine**: Modern `androidx.media3` (`1.3.1`) ExoPlayer APIs with custom overlay controls.
- **Network Engine**: Retrofit 2 + GSON for automatic, safe content deserialization.
- **Image Loader**: Coil Compose with memory/disk caching policies.
- **Obfuscation**: Comprehensive `proguard-rules.pro` mappings safeguarding against model and GSON reflection breaks.

---

## 🛠️ How to Build From Source

### Prerequisites:
- Android Studio Ladybug or newer.
- Android SDK 35 (compileSdk/targetSdk).
- Java Development Kit (JDK) 17.

### Step 1: Clone and Checkout the Release Branch:
```bash
git clone https://github.com/Salmanlaghari/Rising-Flix.git
cd Rising-Flix
git checkout feature/phase-4-final-release
```

### Step 2: Configure Signing:
Create a `keystore.properties` file in the root folder containing the signing parameters:
```properties
storeFile=release-key.jks
storePassword=your_keystore_password
keyAlias=risingflix
keyPassword=your_key_password
```

### Step 3: Compile and Package:
- **Build Immersive Debug APK**:
  ```bash
  ./gradlew assembleDebug
  ```
- **Build Signed Minified Production APK (Only 4.0MB!)**:
  ```bash
  ./gradlew assembleRelease
  ```

---

## 📜 Licenses and Legal Compliance

- **License**: MIT License (view [LICENSE](LICENSE) for details).
- **Privacy Policy**: Refer to [privacy.html](app/src/main/assets/privacy.html) directly inside assets.
- **Terms of Service**: Refer to [terms.html](app/src/main/assets/terms.html) directly inside assets.

---

*Made with 💙 by Salman Laghari and Jules.*
