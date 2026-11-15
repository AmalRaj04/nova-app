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
