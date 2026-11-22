plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val propVersionName: String = (project.findProperty("versionName") as String?) ?: "1.0"
val propVersionCode: Int = (project.findProperty("versionCode") as String?)?.toIntOrNull() ?: 1

// Keystore: w CI ścieżka z env KEYSTORE_FILE; bez niego build używa zwykłego podpisu debug
val ksFile = file(System.getenv("KEYSTORE_FILE") ?: "${rootDir}/release.jks")
val hasKs = ksFile.exists() && ksFile.length() > 0

android {
    namespace = "pl.mdinstal.muzyka"
    compileSdk = 34

    defaultConfig {
        applicationId = "pl.mdinstal.muzyka"
        minSdk = 24
        targetSdk = 34
        versionCode = propVersionCode
        versionName = propVersionName
    }

    signingConfigs {
        if (hasKs) create("release") {
            storeFile = ksFile
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
            keyAlias = System.getenv("KEY_ALIAS") ?: "mdinstal"
            keyPassword = System.getenv("KEY_PASSWORD") ?: ""
        }
    }

    buildTypes {
        debug { if (hasKs) signingConfig = signingConfigs.getByName("release") }
        release {
            isMinifyEnabled = false
            if (hasKs) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // NewPipeExtractor korzysta z java.time i innych API nowszych niż minSdk 24
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    applicationVariants.all {
        outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            output.outputFileName = "Muzyka-${versionName}-${versionCode}.apk"
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.0")

    // Odtwarzanie w tle: radio i YT Music
    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.3.1")
    implementation("androidx.media3:media3-session:1.3.1")

    // YouTube Music: wyszukiwanie i strumienie audio bez reklam (ta sama biblioteka co w NewPipe)
    implementation("com.github.TeamNewPipe:NewPipeExtractor:v0.27.1")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
