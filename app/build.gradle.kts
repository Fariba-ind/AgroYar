import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "ir.agroyar.app"
    compileSdk = 36

    defaultConfig {
        // Fresh package id prevents any signature/package conflict with earlier test builds.
        applicationId = "ir.agroyar.android"
        minSdk = 29
        targetSdk = 35
        versionCode = 100
        versionName = "1.0.0"
    }

    signingConfigs {
        create("agroyarRelease") {
            storeFile = rootProject.file("keystore/agroyar-debug.keystore")
            storePassword = "agroyar-debug"
            keyAlias = "agroyar-debug"
            keyPassword = "agroyar-debug"
            // Android 10+ supports v2/v3. V1 is also kept for broad installer compatibility.
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
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
