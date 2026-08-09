# AgroYar

AgroYar is an Android agriculture reference app focused on searchable pesticide information, safe-use constraints, crop/application guidance, bilingual Persian/English UI, theme controls, and app update awareness.

## Current MVP scope

- Search by scientific name, trade name, active ingredient, or formulation.
- Pesticide detail view with mode of action, target, crops, application guidance, restrictions, weather/water-stress cautions, and pre-harvest interval field.
- Persian and English UI.
- Light, dark, and system theme modes.
- Update check against the latest GitHub Release.
- Offline-first sample catalog with a repository boundary so a validated database/API can replace it later.
- GitHub Actions debug APK build.

## Safety and data quality

The pesticide records currently included are **schema/demo records only**. They intentionally do not provide prescriptive field doses. Real application rates, crop registrations, PHI/REI, incompatibilities, and restrictions must be populated from authoritative, jurisdiction-specific labels and regulatory sources before field use.

## Tech stack

- Kotlin
- Jetpack Compose + Material 3
- Android Gradle Plugin 8.13.2
- Kotlin 2.3.21
- Compose BOM 2026.06.00
- Min SDK 24 / Compile & Target SDK 36

## Build

With Gradle 8.13 and JDK 17 installed:

```bash
gradle :app:assembleDebug
```

GitHub Actions also builds `app-debug.apk` and uploads it as a workflow artifact.
