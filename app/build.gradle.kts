import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "ir.agroyar.app"
    compileSdk = 36

    defaultConfig {
        // Stable production package id for future in-place updates.
        applicationId = "com.agroyar.mobile"
        // Android 10+ is the supported delivery target; a conservative minSdk keeps
        // classic signature schemes available for broader OEM installer compatibility.
        minSdk = 23
        // Target the current Android 16 API so Play Protect does not classify the APK
        // as being built for an obsolete Android privacy/security model.
        targetSdk = 36
        versionCode = 102
        versionName = "1.0.2"
    }

    signingConfigs {
        create("agroyarRelease") {
            storeFile = rootProject.file("keystore/agroyar-debug.keystore")
            storePassword = "agroyar-debug"
            keyAlias = "agroyar-debug"
            keyPassword = "agroyar-debug"
            // Include all APK signature schemes relevant to Android 10+ while retaining
            // compatibility with package installers that still inspect V1/V2 metadata.
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
