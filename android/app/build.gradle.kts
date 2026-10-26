plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "uk.motobay.app"
    compileSdk = 36

    defaultConfig {
        // Change to your own reverse-domain id before publishing to Google Play.
        applicationId = "uk.motobays.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 11
        versionName = "0.7.3"
        val sampleData = (project.findProperty("motobay.sampleData") as String?)?.toBoolean() ?: false
        buildConfigField("boolean", "SAMPLE_DATA", sampleData.toString())
        // Built weekly from OpenStreetMap by .github/workflows/update-bay-data.yml.
        buildConfigField("String", "BAY_DATA_URL", "\"https://raw.githubusercontent.com/Dwayne011/Motobay/main/data/uk-bays.json\"")
        // Dark map style, built by scripts/build_map_style.py from OpenFreeMap's dark style.
        buildConfigField("String", "DARK_MAP_STYLE_URL", "\"https://raw.githubusercontent.com/Dwayne011/Motobay/main/data/map/graphite.json\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
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
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.coil.compose)
    implementation(libs.material.kolor)
    implementation(libs.maplibre)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
