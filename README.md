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
