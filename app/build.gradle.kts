import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "ir.agroyar.app"
    compileSdk = 36

    defaultConfig {
        // Completely new package identity to eliminate all conflicts with earlier AgroYar builds.
        applicationId = "com.agroyar.mobile"
        // Runtime is intended for Android 10+, while keeping minSdk conservative so APK signing
        // remains maximally compatible with OEM package installers.
        minSdk = 23
        targetSdk = 33
        versionCode = 101
        versionName = "1.0.1"
    }

    signingConfigs {
        create("agroyarRelease") {
            storeFile = rootProject.file("keystore/agroyar-debug.keystore")
            storePassword = "agroyar-debug"
            keyAlias = "agroyar-debug"
            keyPassword = "agroyar-debug"
            // V1 + V2 are the broadest-compatible schemes for Android 10+ sideloading.
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = false
            enableV4Signing = false
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("agroyarRelease")
            isDebuggable = true
        }
        getByName("release") {
            signingConfig = signingConfigs.getByName("agroyarRelease")
            isDebuggable = false
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}
