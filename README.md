# 🎬 Rising Flix — Premium Video Streaming App

Welcome to **Rising Flix** — the premium cinematic video streaming hub designed to play your favorite movies, dramas, sports, and cartoons all in one lightweight, modern application. Built with **Kotlin, Jetpack Compose, Retrofit, and Media3/ExoPlayer**, Rising Flix is designed from the ground up for extreme size and performance optimization, weighing in at only **4.0MB**!

---

## ✨ Key Features

- 📱 **Horizontal Cinematic Dashboard**: Elegant cards layouts containing high-quality Unsplash poster images, gold rating badges (`⭐ 9.5`), and status tags (`Live` or `2026`).
- 🎥 **Built-in Media3 ExoPlayer**: Fully customized landscape media player featuring auto-hiding overlays, seek timeline progress bars, rewind/forward 10s buttons, subtitles selection triggers, and cast-to-TV integration.
- ⏬ **Premium Offline Downloads**: Simulated background downloads managing queueing, speeds (MB/s), file sizes, and pausing/resuming controls directly from the Library Screen.
- 🚀 **Hardware Acceleration & Caching**: Complete in-memory repository caching for instant listings load, automated bitmap recycling, and full hardware accelerated rendering.
- 🔒 **Privacy Focused & Safe**: 100% compliant and safe out-of-the-box. Includes fully accessible legal documents (Privacy Policy and Terms of Service) directly inside assets.

---

## 🔄 MovieBox.pk Content Sync

Rising Flix now ships with a **fully synced content catalog sourced from MovieBox.pk**. The streaming links are real, playable `.mp4` streams extracted directly from MovieBox's CDN.

### What was synced
- **300 unique titles** (240 Movies, 33 TV Series, 27 Animation) — all with **real streaming URLs** from `macdn.aoneroom.com` (`-sd.mp4` / `-ld.mp4`).
- Every item includes a **verified playable video URL**, poster image, backdrop, description, year, duration, quality, and rating.
- Content is organized into **8 categories**: Movies, Action & Thriller, Sci-Fi & Fantasy, Horror, Romance & Comedy, Drama, TV Series, and Animation.

### Files produced
- `content.json` — primary catalog consumed by the app (`featured` + `categories[]`), schema matches `MovieItem.kt` / `ContentResponse.kt`.
- `trending.json` — 30 trending titles (powers the Trending screen / `getTrendingMovies()`).
- `popular_dramas.json` — 25 drama/fantasy/TV titles (powers `getPopularDramas()`).
- `search.json` — full searchable index (`SearchResponse { results[] }` / `searchMovies()`).
- `data/moviebox_content.json` — flat catalog reference (446 entries incl. genre cross-listings).

### How the sync works
1. **Collect** detail-page links from MovieBox listing pages (`/web/movie`, `/web/tv-series`, `/web/animated-series`).
2. **Scrape** each detail page and extract the embedded **schema.org `VideoObject` JSON-LD**, which contains the real `contentUrl` (streaming `.mp4`), `name`, `description`, `thumbnailUrl`, `uploadDate`, and `duration`. The OpenGraph `og:image` is used as the high-res poster.
3. **Transform** the scraped data into the Rising-Flix `content.json` format with stable IDs, genre-based subcategories, ratings, and quality tags.
4. **Verify** every streaming URL returns HTTP 206 (playable) before shipping.

The scraper and build scripts live under `scripts/` (`moviebox_scraper.py`, `build_content.py`, `build_supporting.py`).

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
