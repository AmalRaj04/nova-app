# Decisions

Defaults chosen for the open questions in NOVA_APP_SPEC.md. Each can change in data or config without code edits.

| # | Question | Default used |
| --- | --- | --- |
| 1 | Stairs marker facing | Both face people climbing (bearing 0) |
| 2 | Two markers per stairs row | Same node; either triggers guidance |
| 3 | Walking step length | 0.70 m (`Config.WALKING_STEP_LENGTH_M`) |
| 4 | Phone step sensor | Off |
| 5 | Pillar marker facing | Markers 6 and 7 face 90 and 30 degrees |
| 6 | D path start | F-D corner (marker 7) |
| 7 | Marker size | Not used; `minMarkerSidePx` = 0 |
| 8 | Room behind user | Speak "behind you" message |
| 9 | Phone carrying | Portrait, rear camera, upright |
| 10 | Language | English only; text kept in one place |
| 11 | Oldest Android | API 26 |
| 12 | Package name | `com.genvision.nova` |
| 13 | Marker dictionary | `DICT_4X4_50`, IDs 0 to 10 |
| 14 | Instructor hints | None; `hint` fields empty |

## Build decisions

- Step 0: Gradle 9.9.1, AGP 9.5.1, Kotlin 2.4.21 (latest stable found on 2026-11-15). The machine has only JDK 23, so `:core` targets JVM 17 bytecode without a toolchain.
- Step 1: `MapLoader` returns all data-check errors at once (not just the first) so the team can fix a file in one pass. Optional node field `spoken` is already in the model for speech-only names.
- Step 2: A turn of exactly 180 degrees counts as +180 (turn around). `TurnClassifier` returns an enum; the spoken words are added in Step 3 with `Phrases`.
- Step 3: The spec gives wording only for walk-then-stairs. Where it is silent I chose: stairs as the first movement of a leg say "Climb {s} steps up." (no "Then"); a walk after stairs says "Then walk about {n} steps."; 1 step is spoken as "1 step". A turn is worked out once per leg, at the start marker; later edges in the same leg get no turn word.
- Step 3: A leg ends at the next node that has a marker, so unmarked nodes (stairs foot, upper landing) are folded into the leg before them.
- Step 4: Choices where the state table is silent:
  - A sighting at the node already being guided from (after the cooldown) is logged and gets no speech; timers still reset.
  - Rows 12 and 13 speak two sentences as two `Speak` actions: the first interrupts, the second queues. Repeat speaks both together.
  - Row 13 with no forward path says the "behind you" message, keeps the old route and stays in GUIDING, as the table says.
  - Arrival also emits `StopCamera` (FR-11, "stop guiding"); row 19 stops it again, which is harmless. Row 19 also speaks the ready prompt so the return to Home is spoken (accessibility rule 9).
  - Reminders do not fire while the app is in the background; returning restarts the reminder timer.
  - Added a `BackPressed` event for the Error screen's Back button (goes to HOME).
  - The machine does not emit timer actions: it keeps its own timer using `Clock`, and the caller sends `Tick` about once a second.
  - Speak actions carry `interrupt` and `protected` flags so the speaker can keep arrival uninterrupted (FR-16).
- Step 5: Android shell built with Compose. Settings use SharedPreferences (allowed by the brief). `compileSdk` and `targetSdk` are 37, because the current stable Compose and AndroidX libraries require it; Gradle downloaded SDK platform 37. `gradle.properties` raises Gradle memory to 3 GB because the Compose build ran out of memory at the default.
- Step 5: Screen orientation is locked to portrait for the whole app (the brief asks for portrait while guiding).
- Step 5: The camera actions (`StartCamera` and others) only set a `cameraActive` flag in the ViewModel for now; the real camera arrives in Step 6. The developer panel has no camera preview yet and says so.
- Step 5: The ready prompt is spoken through the app's own text-to-speech. The rule that avoids talking over TalkBack (accessibility requirement 7) is left for Step 8.

## Step 5 test note

Run: `./gradlew test assembleDebug` (all pass: 71 `:core` tests, 7 `:app` tests for settings and the log). Ran the app on an Android emulator (API 36, no audio).

- Manual check 1 (D13, markers 0, 2, 4, 6, 7, 8, 10): passed. The log shows each spoken text equal to the golden table and ends with the arrival message.
- Reminder timing, Repeat, Where am I and Stop: seen working in the log (first reminder 12.2 s after the start prompt). Checks 2 to 5 as a full sequence were not cleanly repeated on the emulator, because the emulator was slow and my scripted taps missed some buttons. Those behaviours are covered by the `:core` state machine tests.
- Not run: real speech audio (the emulator had audio off; the app did start text-to-speech without an error), a real phone, TalkBack, a real camera.
- Fixed during testing: text was black on black until the theme got a background surface; the title sat under the status bar until system-bar padding was added.
- Step 6: CameraX 1.6.2 (`camera-core`, `camera2`, `lifecycle`, `view`). The camera is bound to the activity's lifecycle, so CameraX itself stops it when the app goes to the background and restarts it on return. The machine's pause and resume actions still stop reminders during that time.
- Step 6: `Preview` is bound only in developer mode, using the compatible (TextureView) mode because the default SurfaceView drew outside its box in the developer panel.
- Step 6: Frame throttling, the rate meter and the stride-aware luminance copy are small plain classes in `FrameMath.kt` with unit tests. Detection still uses `FakeMarkerDetector`, which finds nothing, until Step 7.
- Step 6: The `camera_start` line may appear twice on entering guidance in developer mode, because the camera is restarted once the preview surface exists. This is harmless.

## Step 6 test note

Run: `./gradlew test assembleDebug` (all pass: 71 `:core` tests, 13 `:app` tests including throttle, rate meter and stride copy). Ran the app on an Android emulator with its virtual camera.

- Seen working: the camera starts when guidance starts, the developer panel shows the live preview and an analysis rate (2.7 to 5.6 frames per second on the slow emulator, against a limit of about 6.7), and after pressing Home and reopening the app the preview and rate come back.
- Not run: a real phone camera, camera error and Retry on a device, camera permission denied on a device, the 1280 by 720 resolution on real hardware, battery and heat.
- Fixed during testing: the preview drew over the developer panel text until it used the compatible mode.
- Step 7: OpenCV 5.1.0 from Maven Central (`org.opencv:opencv`), which has the ArUco classes in `org.opencv.objdetect` (`ArucoDetector`, `Objdetect.getPredefinedDictionary`). `OpenCVLoader.initLocal()` loads the native library. Default detector parameters are used.
- Step 7: Only `ArucoMarkerDetector` uses OpenCV in the app. The dictionary name comes from `markers.json`; only the 4x4 dictionaries are accepted and any other name fails loudly.
- Step 7: OpenCV is large. Packaging is limited to `arm64-v8a`, `armeabi-v7a` and `x86_64`, and the debug APK is still about 148 MB. A release build compresses and will be smaller; measure it in Step 9.
- Step 7: With no markers, OpenCV can return an empty `ids` matrix that is not zero rows; the detector counts the corner list instead.
- Step 7: The developer overlay draws green boxes and "ID n" over the preview. Image corners are turned by the frame rotation and fitted to the view in `MarkerGeometry.toView` (unit tested).
- Step 7: `tools/make_markers.py` writes `tools/markers/marker_00.png` to `marker_10.png` (white border, "NOVA marker n" printed below, 300 dpi, size set with `--size-cm`, default 10 cm). The size is only a default for printing tests; the app does not use it.

## Step 7 test note

Run: `./gradlew test assembleDebug` (71 `:core` tests, 21 `:app` unit tests including marker size and overlay maths), and `./gradlew connectedDebugAndroidTest` on an Android emulator: 6 of 6 pass. They generate marker images with OpenCV and check that IDs 0 to 10 are each read back, a rotated marker is read, a blank image finds nothing, the detector works again after release, the minimum size limit rejects a small marker, and frames of different sizes work. `tools/make_markers.py` output was also read back by Python's OpenCV for all 11 IDs.

- Seen working: the app runs with OpenCV analysing live emulator camera frames (the panel shows "Markers in view: none" because the virtual scene has no marker), with no crash. Analysis ran at about 1.3 frames per second on the slow emulator; that is not a measure of a real phone.
- Not run: reading a printed marker live with a real camera (needed for the "a printed marker is read live" acceptance), the farthest reading distance for each marker, a real phone, speed on real hardware. Step 7 is therefore not fully accepted until the team prints markers and checks them with the developer panel.
- Fixed during testing: the detector crashed on an image with no markers until the empty `ids` case was handled.
- Step 8: Speech routing (accessibility requirement 7). While guiding (WAITING or GUIDING) the app's own text-to-speech is always used. On every other screen, when TalkBack (touch exploration) is on, messages go to the accessibility system (`announceForAccessibility`) so TalkBack and the app do not talk over each other; with TalkBack off, the app's own speech is used everywhere. This includes the arrival message: with TalkBack on it is announced through the accessibility system (so it is not protected from interruption in that case), because the Arrival screen is read by TalkBack. The log line for such messages carries `via=accessibility`. The rule is the pure function `SpeechRouting.route`, unit tested for every state both ways.
- Step 8: Rule 9 (every state change is spoken): asking for camera permission now speaks "NOVA needs to use the camera to read markers. Please allow it when asked." and Retry speaks "Trying again." Opening and closing the Settings screen is not in the state table and is not spoken by the app; TalkBack reads that screen.
- Step 8: Colours are plain numbers in `Palette`, and a unit test checks that white on black, black on yellow, white on dark grey and the overlay green on black are all at least 7 to 1.
- Step 8: Each screen sets a pane title (its title text) and a heading, and the developer overlay drawing is hidden from TalkBack (its IDs are also in text above it).

## Step 8 accessibility notes

Run: `./gradlew test assembleDebug` (71 `:core` and 29 `:app` unit tests pass). Checked on an Android emulator (API 36) with its built-in TalkBack and with the system font at 200%.

| Requirement | Result |
| --- | --- |
| 1. Labels and roles, no icon-only buttons | Every control has a text label; the switch is a labelled switch. Not read aloud by a real TalkBack user. |
| 2. Buttons at least 72 dp, full width | Main buttons set to at least 72 dp (Repeat 160 dp). Developer marker buttons are 56 dp. Seen on screen only. |
| 3. Contrast at least 7 to 1 | Passed in a unit test for all colour pairs in use. |
| 4. Font follows system, usable at 200 percent | Checked at 200 percent: Home, Guidance and Settings screens show no clipped text; Repeat, Where am I and Stop all fit on screen on Guidance, and Settings scrolls. The Arrival and Error screens were not looked at at 200 percent. |
| 5. Nothing time-limited, no swipe or long press needed | Nothing is timed. The Settings and developer pages scroll when text is large. |
| 6. Logical focus order, first focus is the title | NOT met on the emulator. TalkBack's first focus sometimes landed on the text under the title instead (seen on Home after a cold start and on Guidance), even with pane titles set. Needs a check on a real phone with a TalkBack user; a different approach may be needed. |
| 7. TalkBack and own speech do not overlap | Checked both ways in the log: TalkBack off, everything uses own speech; TalkBack on, the Home prompt and "Guidance stopped." go through the accessibility system, while "Going to D13...", reminders and instructions use own speech. Audio was off, so actual overlap was not heard. |
| 8. Screen stays on while guiding | Done in Step 5 (window flag); not measured over time. |
| 9. Every state change is spoken | Added speech for the permission request and Retry; all other rows already spoke. |

Not run: a real phone, a real TalkBack user, how speech sounds, 200 percent on Arrival and Error.

- Step 9: While an error screen is showing, further errors are ignored (otherwise a failing camera would repeat the same spoken error for every frame), and a camera error that arrives when guidance is not running is ignored. The camera session reports an analysis failure once.
- Step 9: Lint reports warnings only. The portrait lock is kept on purpose (the brief asks for portrait); Android 16 and later may ignore it on large screens, which does not matter for phones.
- Step 9: The 15-minute emulator run was disturbed by hand use of the emulator and is recorded in TEST_REPORT.md as not a valid unattended run. Battery and heat were not measured. The README and TEST_REPORT.md are written; the README steps were written from the commands that were run, but not followed from scratch by a new person.

## Step 10: stretch goal, voice (built at the user's request)

- Voice commands are OFF by default and the buttons always stay. Both the voice button and the Volume up key start listening only when the person switches **Voice commands** on in Settings and the phone can recognise speech on the device.
- Offline: only Android's on-device recogniser is used (`createOnDeviceSpeechRecognizer`, Android 13+ with the offline pack). There is no fallback to an online recogniser, because the brief forbids network use. Below Android 13, or without the pack, voice is simply not offered.
- Permissions: the brief allowed only CAMERA (plus VIBRATE for the other stretch goal). Voice input needs **RECORD_AUDIO**, declared in the manifest, requested only the first time the person uses voice. Audio is never stored. The INTERNET permission is still not declared.
- The Volume up key is the talk key while voice is on (it can be found without looking). The cost is that Volume up no longer raises the volume; Volume down and the phone's volume panel still work. The Settings text says so. The team should decide with the instructor whether this trade is right.
- The command words are matched by `VoiceCommandParser` in `:core` (tested). Room names come from `map.json`, so a new room works without code changes. "Stop" wins over a room name; two rooms heard together are not guessed.
- Voice has the same effect as the buttons and goes through the same state machine. Where a command does not fit the state, NOVA says so ("Say stop first, then choose a place." or "Guidance has not started...") so that no command is silently ignored.
- "Hey Google": opening NOVA by name needs no code. The `nova://destination/<room>` link and one dynamic app shortcut per room (built from `map.json` at run time, so no room names are in code or XML) let an assistant or shortcut start guidance. This is not registered with Google's App Actions (that needs a Play Console setup), so exactly which spoken phrases work depends on the phone.
- The activity is now `singleTask`, so a link or shortcut reuses the running app. A link for a room that is not in `map.json` is ignored, and a link that arrives while the person is already guiding is ignored.
- Vibration patterns (the other stretch goal) were not built.
