plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.mora.gamespace"
    compileSdk = 36
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.mora.gamespace"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-skeleton"
        vectorDrawables.useSupportLibrary = true

        // libVLC ships native decoders per-ABI. Target arm64 (RedMagic/Nubia) to keep the APK
        // reasonable. Add "armeabi-v7a" here too if you need 32-bit device support.
        ndk { abiFilters += listOf("arm64-v8a") }
    }

    signingConfigs {
        create("releaseDebugKey") {
            val keystorePath = System.getenv("GAMESPACE_RELEASE_KEYSTORE")
                ?: "${rootProject.layout.buildDirectory.get().asFile}/generated-debug-release.keystore"
            storeFile = file(keystorePath)
            storePassword = System.getenv("GAMESPACE_RELEASE_KEYSTORE_PASSWORD") ?: "android"
            keyAlias = System.getenv("GAMESPACE_RELEASE_KEY_ALIAS") ?: "androiddebugkey"
            keyPassword = System.getenv("GAMESPACE_RELEASE_KEY_PASSWORD") ?: "android"
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
            enableV4Signing = false
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("releaseDebugKey")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures { compose = true }

    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")

    implementation(platform("androidx.compose:compose-bom:2026.04.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // VLC engine embedded as the audio decoder. It bundles its own codecs (FFmpeg-based),
    // so background music plays independently of the device's system media codecs.
    implementation("org.videolan.android:libvlc-all:3.6.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
