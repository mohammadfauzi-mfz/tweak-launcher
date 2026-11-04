plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Nomor versi naik otomatis dari GitHub Actions (-PverCode=<run number>).
val verCode = (project.findProperty("verCode") as String?)?.toIntOrNull() ?: 1

android {
    namespace = "com.fauzi.tweaklauncher"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.fauzi.tweaklauncher"
        minSdk = 30
        targetSdk = 35
        versionCode = verCode
        versionName = "1.0.$verCode"
    }

    // Keystore tetap supaya APK versi baru bisa menimpa versi lama (update dari aplikasi).
    signingConfigs {
        create("release") {
            storeFile = file("release.keystore")
            storePassword = "tweaklauncher"
            keyAlias = "tweak"
            keyPassword = "tweaklauncher"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
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
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.6")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")
}
