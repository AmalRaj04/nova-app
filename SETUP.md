# NOVA: setup on a new laptop

Step by step, from a clean machine to the app running on a phone. There are two parts:

- **Part A: the Android app** (the real NOVA, reads ArUco markers). Needs Android Studio.
- **Part B: the Expo test version** (`nova-expo/`, no marker reading, team-mode buttons instead). Needs Node.js.

Do Part A for real testing. Part B is optional.

Commands below are for macOS or Linux. On Windows, use `gradlew.bat` instead of `./gradlew`, and PowerShell or Git Bash.

---

## 0. Get the code

1. Install Git: https://git-scm.com/downloads
2. Clone and enter the project:

```bash
git clone <your repository URL> nova-app
cd nova-app
```

Check that the folder contains `app/`, `core/`, `gradlew` and `nova-expo/`.

---

## Part A: Android app

### A1. Install Android Studio

1. Download Android Studio from https://developer.android.com/studio and install it.
2. Open it once and finish the setup wizard (accept the default SDK location, accept the licences).

Android Studio ships its own Java (JDK), so you do not need to install Java separately.

### A2. Install the Android SDK pieces

In Android Studio: **Settings (Preferences on Mac) > Languages & Frameworks > Android SDK**.

- **SDK Platforms** tab: tick **the platform with API level 37**. The project compiles against API 37.
- **SDK Tools** tab: tick **Android SDK Build-Tools**, **Android SDK Platform-Tools** and **Android Emulator** (the emulator is only needed if you test without a phone).
- Press **Apply** and wait for the downloads.

If a later build says `Failed to find target with hash string 'android-37'`, this step is the fix.

### A3. Open the project

1. In Android Studio choose **Open** and select the `nova-app` folder (the one with `settings.gradle.kts`, **not** `nova-expo`).
2. Wait for **Gradle sync** to finish (bottom right). The first sync downloads Gradle 9.9.1 and all libraries (including OpenCV), so it needs internet and can take several minutes.
3. If Android Studio asks to trust the project, choose **Trust**.
4. If it asks for the SDK location, it writes `local.properties` for you. That file is machine-specific and is not committed.

If sync fails with a memory error, the project already sets `org.gradle.jvmargs=-Xmx3g` in `gradle.properties`. Close other heavy apps or raise that number.

### A4. Run the tests (optional, no phone needed)

In a terminal opened in the project folder:

```bash
./gradlew :core:test :app:testDebugUnitTest
```

All tests should pass. The `:core` module has the routing and instruction tests, and `:app` has the settings, log and speech-routing tests.

### A5. Connect the phone

1. On the phone: **Settings > About phone**, tap **Build number** 7 times to turn on Developer options.
2. **Settings > System > Developer options**: turn on **USB debugging**.
3. Connect the phone to the laptop with a **data** USB cable (some cables only charge). Set the USB mode to **File transfer**.
4. Tap **Allow** on the phone's "Allow USB debugging?" dialog.
5. Check that the laptop sees the phone. The phone must be listed as `device`, not `unauthorized`:

```bash
~/Library/Android/sdk/platform-tools/adb devices
```

(On Windows the path is `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe`.)

The phone needs Android 8.0 (API 26) or newer.

### A6. Build and run

In Android Studio:

1. Pick your phone in the device dropdown at the top.
2. Pick the **app** run configuration.
3. Press the green **Run** button.

Or from the terminal (installs onto the connected phone):

```bash
./gradlew :app:installDebug
```

The first build is slow. The debug app is about 148 MB because it includes OpenCV for three CPU types.

When the app opens, allow the **camera** when it asks.

### A7. Make and print the markers

The app reads 11 printed ArUco markers (IDs 0 to 10, dictionary `DICT_4X4_50`).

1. Install Python 3 (https://www.python.org/downloads/), then the two libraries:

```bash
pip install opencv-python numpy
```

2. Generate the images (10 cm markers by default):

```bash
python3 tools/make_markers.py
```

This writes `tools/markers/marker_00.png` to `marker_10.png`.

3. Print them at **100 percent / actual size** (not "fit to page") on matte paper. Do not test from a screen, it causes glare. Keep the white border around each marker. Measure one printed marker and write the size down.
4. Place them where `app/src/main/assets/markers.json` says (the `where` field of each marker).

### A8. Test on the phone

1. Choose a destination (D11, D12 or D13).
2. Point the camera at marker 0 from about 1 to 2 m and listen for the first instruction.
3. Show the other markers in order. For D13 the order is 0, 2, 4, 6, 7, 8, 10.
4. Try the **Repeat**, **Where am I** and **Stop** buttons.
5. To test without printed markers, open **Settings**, turn on **Developer mode**, and use the marker buttons to simulate sightings.
6. To test with TalkBack: **Settings > Accessibility > TalkBack**, turn it on, then use the app with swipes and double taps.

`README.md` has the full field-test checklist. `TEST_REPORT.md` lists what has and has not been verified.

### A9. If something goes wrong

| Problem | Try |
|---|---|
| Phone not in the device list | Another USB cable, File transfer mode, re-plug, accept the debugging dialog, `adb kill-server` then `adb devices` |
| `Failed to find target ... android-37` | Install **the platform with API level 37** in the SDK Manager (step A2) |
| Gradle sync: `SDK location not found` | Open **Settings > Android SDK**, copy the SDK path, and put `sdk.dir=<that path>` in `local.properties` |
| Out of memory during build | Raise `-Xmx3g` in `gradle.properties` |
| No speech | Raise media volume. Check **Settings > System > Languages > Text-to-speech** is set to an English voice |
| No marker is read | Better light, 1 to 2 m, flat print, white border visible, correct size. Turn on Developer mode to see the camera preview |
| App will not install: `INSTALL_FAILED_...` | Uninstall the old NOVA from the phone and run again |

---

## Part B: Expo test version (optional)

Use this to check the voice, the wording and the flow on a phone **without markers**. It does not replace Part A: it cannot read markers and needs Wi-Fi.

### B1. Install Node.js

1. Install the LTS version of Node.js from https://nodejs.org (includes npm).
2. Check:

```bash
node -v
npm -v
```

### B2. Install the phone app

Install **Expo Go** from the Play Store (Android) or App Store (iPhone).

### B3. Install dependencies and start

```bash
cd nova-expo
npm install
npx expo start
```

A QR code appears in the terminal.

### B4. Open on the phone

1. Put the phone and the laptop on the **same Wi-Fi**.
2. Android: open Expo Go and scan the QR code. iPhone: scan it with the Camera app.
3. If it will not connect, run `npx expo start --tunnel` instead (slower, but works across networks), or check the laptop firewall.

### B5. Test

1. Pick a destination, allow the camera.
2. Leave **Team mode** on and press the marker buttons in order (for D13: 0, 2, 4, 6, 7, 8, 10). Each press acts as if the camera had just read that marker.
3. Try Repeat, Where am I, Stop, a wrong marker, and waiting without pressing anything.

Run its tests and type check with:

```bash
npm test
npm run typecheck
```

`nova-expo/README.md` explains what differs from the Android app.

---

## Notes

- The Android app is fully offline once installed: it has no network permission. Only the build needs internet.
- Map and marker facts live in `app/src/main/assets/map.json` and `markers.json`. Copies are in `core/src/test/resources/` and `nova-expo/src/data/`. Change all copies together.
- NOVA does not replace a white cane or a guide dog. It has not been tested as a safe way to guide a person.
