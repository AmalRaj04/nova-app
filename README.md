# NOVA

NOVA is an offline Android app that gives **spoken wayfinding** to a visually impaired student on the first-floor D-block route. The phone camera reads printed ArUco markers. At each marker the app speaks one short instruction (turn, steps, stairs) towards the chosen room: D11, D12 or D13.

The full specification is in [NOVA_APP_SPEC.md](NOVA_APP_SPEC.md). The choices made where the spec left room are in [DECISIONS.md](DECISIONS.md). What was tested, and what was not, is in [TEST_REPORT.md](TEST_REPORT.md).

## Read this first: limits

- **NOVA has not been tested in the building.** Marker positions come from an approximate drawing, distances are estimates, and the walking step length (default 0.70 m) has not been measured.
- NOVA **only speaks at markers.** Between two markers it says nothing except reminders, so a student can drift off course without being told.
- NOVA **does not warn about** people, open doors, glass, steps or other obstacles. It does **not replace a white cane or a guide dog**. A sighted person must be beside anyone testing it.
- Marker reading can fail in glare, low light or at a sharp angle, and can occasionally read the wrong ID. Requiring two sightings reduces this but does not remove it.
- A phone swinging on a chest holder can blur the image. Step counts are rough because people's steps differ.
- Whether the stairs-strip markers and the pillar markers can be read from the walking lane is not yet known.
- Guidance is one-way (entrance towards the D block) and English only.
- The app has not been checked with a real TalkBack user or on a real phone. See the test report.

## What you need

- A computer with **JDK 17 or newer** (`java -version`).
- The **Android SDK** (install Android Studio, or the command-line tools). Put its path in a file named `local.properties` in the project folder (this file is not stored in git):

  ```properties
  sdk.dir=/Users/yourname/Library/Android/sdk
  ```

  Android Studio creates this file for you when you open the project.
- Internet **while building**, so Gradle can download libraries and the Android SDK platform 37 (accept the licences when asked). The app itself uses no internet and does not ask for the INTERNET permission.
- An Android phone, version 8.0 (API 26) or newer, with a camera. Or an emulator.

## Build and test

Run these from the project folder. The `./gradlew` script downloads the right Gradle version by itself.

```bash
./gradlew test
```

Runs all unit tests (no phone needed): the whole `:core` module and the plain-Kotlin parts of `:app`.

```bash
./gradlew assembleDebug
```

Builds the app. The file appears at `app/build/outputs/apk/debug/app-debug.apk`. It is large (about 148 MB) because OpenCV carries native code for three phone types.

```bash
./gradlew connectedDebugAndroidTest
```

Runs the OpenCV marker-reading tests on a connected phone or running emulator.

If the build runs out of memory, the setting `org.gradle.jvmargs` in `gradle.properties` controls how much Gradle may use.

## Install on a phone

1. On the phone, turn on **Developer options** and then **USB debugging** (tap *Build number* seven times in *About phone*).
2. Connect the phone by USB and accept the "Allow USB debugging" question on the phone.
3. Install and start the app:

   ```bash
   ./gradlew installDebug
   adb shell am start -n com.genvision.nova/.MainActivity
   ```

   (`adb` is in the Android SDK folder `platform-tools`.)
4. The first time you choose a destination, allow the camera when asked. If you said no, the app tells you by voice and text and shows a button that opens the phone's settings.

To use an emulator instead, start one from Android Studio's Device Manager (give it a working speech engine, and use a back camera set to *VirtualScene*), then run the same commands.

## Using the app

1. The Home screen says "NOVA is ready. Where do you want to go?" and shows one large button each for D11, D12 and D13.
2. Choose a room, then point the phone forward. Hold it upright (portrait), rear camera facing ahead, on the chest or on a lanyard.
3. At each marker the app speaks one instruction. Buttons: **Repeat** (speaks the last instruction again), **Where am I** (last marker and how long ago) and **Stop**.
4. If no marker is seen for 12 seconds the app reminds you (again at 27 and 42 seconds), then gives a help message at 57 seconds. If you see a marker that is not on the planned route, the app plans a new route from there. If the room is behind you, it says so (this version cannot guide backwards).
5. On arrival it says which side the door is on and shows **Choose another destination**.

### Settings

*Settings* lets you change the walking step length (0.50 to 0.90 m, default 0.70), the speech rate, and the **For the team** switch (developer mode). Changes are saved and apply to the next instruction.

### Developer mode (for the team)

Turn on **For the team** in Settings. The Guidance screen then also shows:

- the live camera preview with a green box and ID around each marker it reads,
- the analysis rate (frames per second) and the markers in view,
- the current state and place,
- one button per marker ID (0 to 10) that **simulates a sighting without a camera**,
- the last 50 log lines and an **Export log** button (opens the phone's share sheet).

The event log is a text file (`logs/nova.log` in the app's private storage, rotated at about 1 MB). Each line is a timestamp, an event name and `key=value` pairs. It records app start, destination, sightings, spoken text, reminders, off-route, arrival and errors. **No camera images are ever stored.**

#### Checks you can do without a camera

1. Choose D13. Press simulated markers 0, 2, 4, 6, 7, 8, 10 in order. The spoken text must match the table in the spec and end with the arrival message.
2. Choose D12. Press 0, then 6. The app speaks the instruction for marker 6 (it is ahead).
3. Choose D13. Press 0, 2, 4, then 2 again. The app says "You are back at the stairs landing."
4. Choose D11. Press 10. The app says D11 is behind you.
5. Choose D13, press 0 and wait. Reminders come at 12, 27 and 42 seconds, the help message at 57.
6. Press Repeat, Where am I and Stop at each stage and listen.

## Voice commands and "Hey Google" (optional, off by default)

Buttons always work and always stay. Voice is an extra for people who cannot easily find the buttons.

**Voice commands.** In *Settings*, turn on **Voice commands**. The switch is only usable on a phone that can recognise speech **offline** (Android 13 or newer with the offline speech pack installed); NOVA never uses an online recogniser, so it stays offline. When it is on:

- a **Speak a command** button appears on the Home and Guidance screens, and
- the **Volume up** key starts listening, so it can be found without looking. (While voice commands are on, Volume up no longer raises the volume; use Volume down or the phone's volume panel.)

After a short beep, say one of these:

| Say | Does the same as |
| --- | --- |
| "go to D13" (also "D 13", "D thirteen", "13"), or any room in `map.json` | the Go to D13 button |
| "repeat" or "say again" | Repeat |
| "where am I" | Where am I |
| "stop" | Stop |
| "choose another" or "go home" | Choose another destination |

If NOVA does not understand it says so and lists what to say. The first time, the phone asks for the **microphone** permission (RECORD_AUDIO); NOVA asks for it only when you use voice, and never records or stores audio.

**"Hey Google".** Saying "Hey Google, open NOVA" opens the app by its name. NOVA also understands the link `nova://destination/D13` (any room in `map.json`), and publishes one app shortcut per room, so a voice assistant, a launcher shortcut or an NFC tag can start guidance straight away. Whether a given phone's assistant can use those shortcuts depends on that phone and has not been tested.

In developer mode there is a text box where you can **type a command** instead of speaking it. It follows exactly the same path as speech, so the team can check the wording without a microphone.

## Print the markers

The app reads `DICT_4X4_50` markers, IDs 0 to 10. The tool needs Python 3 with OpenCV:

```bash
pip install opencv-python numpy
python3 tools/make_markers.py --size-cm 12
```

This writes `tools/markers/marker_00.png` to `marker_10.png`. Each has a wide white border and "NOVA marker n" printed underneath so the person sticking it up can tell them apart.

- Print at **100 percent / actual size** (no "fit to page"). Print on matt paper if you can; glossy paper causes glare.
- **Measure the printed black square** and write the size down.
- Stick each marker where the table below says. The places come from `markers.json`, so if the team moves a marker, change the file, not the code.

| ID | Place (node) | Where it stands |
| --- | --- | --- |
| 0 | Entrance | Entrance row, left strip between the stair lanes |
| 1 | Entrance | Entrance row, right strip |
| 2 | Stairs landing | Long step (landing) row, left strip |
| 3 | Stairs landing | Long step (landing) row, right strip |
| 4 | Top of the main stairs | Door wall row at the top of the stairs, left strip |
| 5 | Top of the main stairs | Door wall row, right strip |
| 6 | Walkway corner near the stairs | Walkway pillar near the stairs-D corner |
| 7 | Start of the D block path | Walkway pillar at the F-D corner |
| 8 | D11 | D path pillar P1, beside D11 |
| 9 | D12 | D path pillar P2, beside D12 |
| 10 | D13 | D path pillar P3, beside D13 |

(Left and right in the first six rows are as seen by a person climbing the stairs. Markers in the same row stand for the same place.)

### Field test: what to record

- With developer mode on, point the phone at each printed marker. The ID in the overlay must match the sticker. For each marker, record the **farthest distance at which it is accepted**. This tells the team what marker size and height to use.
- Listen to how the phone says "D11", "D12", "D13" and the step counts. If it sounds wrong, add a `spoken` field to the node in `map.json` (for example `"spoken": "D eleven"`).
- Go through every screen with TalkBack on, and again at 200 percent font size.
- Run guidance for 15 minutes with the camera on. Note the battery drop and how warm the phone gets.
- Test on the newest and the oldest phone the team owns.

## How the project is organised

Two Gradle modules:

- `core/` is plain Kotlin with **no Android code**, so it runs and is tested on a laptop. It holds the map model and loader (`data/`), bearings and turn words (`geometry/`), route finding and legs (`route/`), all spoken sentences (`speech/Phrases.kt`), marker confirmation (`sighting/`), and the guidance state machine (`nav/NavigationMachine.kt`). Every tunable number is in `Config.kt`.
- `app/` is the Android part: `camera/` (CameraX, the OpenCV detector), `speech/` (text-to-speech), `log/`, `settings/`, `ui/` (Jetpack Compose screens), `NovaViewModel` and `MainActivity`. `app` depends on `core`, never the other way round. OpenCV is used only in `ArucoMarkerDetector`.

Place-specific facts live in two data files, **not** in code: `app/src/main/assets/map.json` (places and walks) and `markers.json` (which marker stands at which place). A copy of each is in `core/src/test/resources/` for the tests. **If you change one, change both copies.** The app checks the files when it starts and refuses to guide if they are inconsistent.

### Changing things safely

- A marker ID, distance, room or door side: edit the JSON files only.
- A spoken sentence: edit `Phrases.kt` (all text is in that one place, ready for a Tamil version later).
- A timing or threshold (reminder times, frames to confirm a marker, analysis size): edit `Config.kt`.
- After any change run `./gradlew test`.

## Versions used

Gradle 9.9.1, Android Gradle Plugin 9.5.1, Kotlin 2.4.21, kotlinx.serialization 1.13.0, kotlinx.coroutines 1.11.0, Compose BOM 2026.09.00, Activity Compose 1.13.0, Lifecycle 2.11.0, CameraX 1.6.2, OpenCV 5.1.0, AndroidX Core 1.19.1. `compileSdk` and `targetSdk` are 37, `minSdk` is 26. Package name `com.genvision.nova`.

## Known gaps (see TEST_REPORT.md)

- Reading a printed marker with a real camera, marker reading range, battery and heat, a real phone, and TalkBack with a real user have **not** been tested.
- On the emulator, TalkBack's first focus on a screen does not always land on the screen title. This needs checking on a real phone.
- The debug APK is about 148 MB because of OpenCV.
- Voice commands and the "Hey Google" shortcuts have not been tried with real speech or a real assistant: the emulator had no offline speech pack, so listening ended with "Voice commands are not available on this phone".
