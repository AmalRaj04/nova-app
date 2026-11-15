# NOVA

Offline Android app that gives spoken wayfinding for a visually impaired student along the first-floor D-block route, using printed ArUco markers. See NOVA_APP_SPEC.md.

**Status: early development. Not tested in the building. NOVA gives wayfinding only and does not replace a white cane or guide dog.**

## Build and test

```bash
./gradlew test
./gradlew assembleDebug
```

Requires a JDK (17 or newer) and the Android SDK (path in `local.properties`, not committed).

## Versions

Gradle 9.9.1, Android Gradle Plugin 9.5.1, Kotlin 2.4.21, kotlinx.serialization 1.13.0, Compose BOM 2026.09.00, CameraX 1.6.2, OpenCV 5.1.0.
