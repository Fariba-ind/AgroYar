import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "ir.agroyar.app"
    compileSdk = 36

    defaultConfig {
        // Fresh, stable install identity. This avoids signature/package conflicts
        // with early AgroYar preview APKs that used ir.agroyar.app.
        applicationId = "ir.agroyar.mobile"
        minSdk = 23
        targetSdk = 36
        versionCode = 3
        versionName = "0.2.1"
    }

    signingConfigs {
        create("agroyarStandalone") {
            // Standalone distribution key used consistently for direct APK builds.
            // A separate Play/App Store production key should be used for store release.
            storeFile = rootProject.file("keystore/agroyar-debug.keystore")
            storePassword = "agroyar-debug"
            keyAlias = "agroyar-debug"
            keyPassword = "agroyar-debug"
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
            enableV4Signing = true
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("agroyarStandalone")
            isDebuggable = true
        }
        getByName("release") {
            signingConfig = signingConfigs.getByName("agroyarStandalone")
            isDebuggable = false
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }

    buildFeatures {
        compose = true
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

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.06.00"))
    implementation("androidx.activity:activity-compose:1.12.3")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
