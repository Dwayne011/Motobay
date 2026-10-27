import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
}

// Release signing comes from android/keystore.properties (git-ignored):
//   storeFile=upload-keystore.jks
//   storePassword=...
//   keyAlias=upload
//   keyPassword=...
// Without it the release build is unsigned, which is fine for CI checks but not for Play.
val keystoreProperties = Properties().apply {
    rootProject.file("keystore.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}
val hasReleaseKey = keystoreProperties.getProperty("storeFile") != null

android {
    namespace = "uk.motobay.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "uk.motobays.app"
        minSdk = 26
        targetSdk = 36
        // Play needs versionCode to rise on every upload. CI can pass -Pmotobay.versionCode=N.
        versionCode = (project.findProperty("motobay.versionCode") as String?)?.toInt() ?: 16
        versionName = (project.findProperty("motobay.versionName") as String?) ?: "1.0.0"
        val sampleData = (project.findProperty("motobay.sampleData") as String?)?.toBoolean() ?: false
        buildConfigField("boolean", "SAMPLE_DATA", sampleData.toString())
        // Built weekly from OpenStreetMap by .github/workflows/update-bay-data.yml.
        buildConfigField("String", "BAY_DATA_URL", "\"https://raw.githubusercontent.com/Dwayne011/Motobay/main/data/uk-bays.json\"")
        // Dark map style, built by scripts/build_map_style.py from OpenFreeMap's dark style.
        buildConfigField("String", "DARK_MAP_STYLE_URL", "\"https://raw.githubusercontent.com/Dwayne011/Motobay/main/data/map/graphite.json\"")
    }

    // One debug certificate shared by every machine and cloud session, so its
    // SHA-1 can be registered once in Firebase for Google sign-in. Debug only;
    // the release key is never committed.
    signingConfigs {
        getByName("debug") {
            storeFile = rootProject.file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        if (hasReleaseKey) {
            create("release") {
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
            isDebuggable = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasReleaseKey) signingConfig = signingConfigs.getByName("release")
        }
    }

    bundle {
        // One upload, Play serves per-device splits.
        language { enableSplit = true }
        density { enableSplit = true }
        abi { enableSplit = true }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                // Screenshot tests write PNGs to app/build/outputs/roborazzi.
                it.systemProperty("roborazzi.test.record", "true")
            }
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material3.navigation.suite)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.material.kolor)
    implementation(libs.maplibre)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.google.id)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
