import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.salmanlaghari.risingflix"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.salmanlaghari.risingflix"
        minSdk = 24
        targetSdk = 34
        versionCode = 10000
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Load keystore.properties for signed release setup
    val keystorePropertiesFile = rootProject.file("keystore.properties")
    val keystoreProperties = Properties()
    var isSigningConfigured = false
    if (keystorePropertiesFile.exists()) {
        try {
            keystorePropertiesFile.inputStream().use { input ->
                keystoreProperties.load(input)
            }
            val storeFilePath = keystoreProperties.getProperty("storeFile")
            val storePasswordProp = keystoreProperties.getProperty("storePassword")
            val keyAliasProp = keystoreProperties.getProperty("keyAlias")
            val keyPasswordProp = keystoreProperties.getProperty("keyPassword")

            if (!storeFilePath.isNullOrEmpty() &&
                !storePasswordProp.isNullOrEmpty() &&
                !keyAliasProp.isNullOrEmpty() &&
                !keyPasswordProp.isNullOrEmpty()) {
                val keystoreFile = rootProject.file(storeFilePath)
                if (keystoreFile.exists() && keystoreFile.isFile && keystoreFile.length() > 0) {
                    isSigningConfigured = true
                }
            }
        } catch (e: Exception) {
            isSigningConfigured = false
        }
    }

    signingConfigs {
        create("release") {
            if (isSigningConfigured) {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (isSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            } else {
                signingConfig = signingConfigs.getByName("debug")
            }
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.ui)
    implementation(libs.coil.compose)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.androidx.mediarouter)
    implementation(libs.google.play.services.cast)
    implementation(libs.jsoup)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
