plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.marvis.silentcamera"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.marvis.silentcamera"
        minSdk = 26
        targetSdk = 35
        versionCode = 21
        versionName = "2.1"
        val sourceCommit = System.getenv("SILENTCAMERA_SOURCE_COMMIT") ?: "local"
        require(sourceCommit == "local" || sourceCommit.matches(Regex("[a-f0-9]{40}")))
        buildConfigField("String", "SOURCE_COMMIT", "\"$sourceCommit\"")
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".preview"
            versionNameSuffix = "-preview"
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
        aidl = true
        buildConfig = true
    }

    // All locales are shipped together, including in optional App Bundle builds.
    bundle { language { enableSplit = false } }

    val externalKeystore = System.getenv("SILENTCAMERA_KEYSTORE")
    if (!externalKeystore.isNullOrBlank()) {
        signingConfigs {
            create("externalRelease") {
                storeFile = file(externalKeystore)
                storePassword = System.getenv("SILENTCAMERA_STORE_PASSWORD")
                keyAlias = System.getenv("SILENTCAMERA_KEY_ALIAS")
                keyPassword = System.getenv("SILENTCAMERA_KEY_PASSWORD")
            }
        }
        buildTypes.getByName("release").signingConfig = signingConfigs.getByName("externalRelease")
    }

    testOptions { unitTests.isReturnDefaultValues = true }
}

dependencies {
    // Shizuku API
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")

    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Activity Compose
    implementation("androidx.activity:activity-compose:1.9.0")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    implementation("androidx.camera:camera-camera2:1.4.2")
    implementation("androidx.camera:camera-lifecycle:1.4.2")
    implementation("androidx.camera:camera-view:1.4.2")
    testImplementation("junit:junit:4.13.2")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
