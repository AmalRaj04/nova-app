# NOVA test report

Date of the runs: 15 November 2026. Everything below was run on a Mac with an Android **emulator** (API 36, no audio). **Nothing has been run on a real phone, in the building, or with a real TalkBack user.** NOVA has not been tested in the building and must not be described as tested or safe for independent use.

## Automated tests

| Command | What it covers | Result |
| --- | --- | --- |
| `./gradlew test` | `:core`: 73 tests. Map loading and each data check, bearings and turn angles, turn words and their boundaries, routing, golden spoken text, rounding and summing of steps, hints, the sighting filter, and every row 1 to 22 of the state table with a fake clock (reminders at 12, 27, 42 s and help at 57 s, going back, off route, behind you, errors and Retry, background and foreground) | 73 passed |
| `./gradlew test` | `:app` plain-Kotlin tests: 29 tests. Settings limits, event log format and rotation, frame throttle, rate meter, stride-aware luminance copy, marker size and overlay maths, speech routing in every state both ways, colour contrast at least 7 to 1 | 29 passed |
| `./gradlew connectedDebugAndroidTest` | 6 OpenCV tests on an emulator: reads marker IDs 0 to 10 from generated images, a rotated marker, a blank image finds nothing, works again after release, minimum size limit, frames of different sizes | 6 passed (run at the end of Step 7) |
| `./gradlew lintDebug` | Android lint | Warnings only, no errors (portrait lock, backup attribute) |
| `./gradlew assembleDebug`, `assembleRelease` | Both build. Release APK is unsigned, about 144 MB | Built |
| `tools/make_markers.py` | All 11 images read back correctly by Python's OpenCV | Passed |

## Manual checks on the emulator (developer mode, no real markers)

| Check | Result |
| --- | --- |
| 1. D13, simulated markers 0, 2, 4, 6, 7, 8, 10 | Passed. Each spoken line in the log equals the golden text and the walk ends with the arrival message. |
| 2. D12, marker 0 then 6 | Not repeated on the device (covered by the state machine tests). |
| 3. Going back to an earlier marker | Not repeated cleanly on the device (covered by the state machine tests). |
| 4. D11 then marker 10 (behind you) | Not repeated cleanly on the device (covered by the state machine tests). |
| 5. Reminders at 12, 27, 42 s, help at 57 s | The first reminder was seen about 12 s after the start prompt, and the help message appeared in the log. The full timeline is covered by the state machine tests with a fake clock. |
| 6. Repeat, Where am I, Stop | Seen working in the log. |
| Camera | Starts with guidance, live preview and analysis rate in the developer panel, camera comes back after the app is sent to the background and reopened. |
| OpenCV on live frames | Ran on emulator camera frames without crashing. The virtual scene has no ArUco marker, so "Markers in view: none". |
| TalkBack (the emulator's built-in) | With TalkBack on, the Home prompt and "Guidance stopped." went through the accessibility system and guidance speech used the app's own speech (confirmed in the log, `via=accessibility`). |
| 200 percent font | Home, Guidance and Settings show no clipped text. |

## 15-minute run

**Not a valid 15-minute run.** I started a scripted run on the emulator at 16:29 (developer mode off, camera on, app sent to the background for 20 seconds and back at minute 7, sampled every minute). It was disturbed by hand use of the same emulator: from about 90 seconds in, the app's log shows destinations chosen and simulated markers pressed that the script did not send, and at 16:42 the app was started again. So the run cannot be counted as 15 unattended minutes.

What the samples do show, for what they are worth on an emulator:

- No `FATAL EXCEPTION` appeared in logcat during the 16 minutes, and the app did not crash on camera analysis with OpenCV running.
- App memory stayed between about 67 MB and 119 MB (total PSS) with no steady climb.
- The log file stayed small and the camera started and stopped as expected.
- The emulator's battery stayed at 100 percent (it is not a real battery), so **battery drop and phone heat were not measured**.

A clean 15-minute run, with battery and heat, still has to be done on a real phone.

## What could NOT be run, and why

- **Reading a printed marker with a real camera**, and the farthest reading distance for each marker (needs printed markers and a phone).
- **Marker size and height choice** (needs the distance data above).
- **Anything in the building**: whether the stairs-strip and pillar markers can be read from the walking lane, the real step length, the correct door side, the corner distances.
- **A real phone**: real camera speed (the emulator ran at about 1 to 6 frames per second, which says nothing about a phone), battery drop, how warm the phone gets, oldest and newest phone checks.
- **Real speech**: the emulator had audio off, so how text-to-speech sounds was not heard (for example how it says "D11"). If it sounds wrong, use the `spoken` field described in the README.
- **TalkBack with a real user**, and whether TalkBack and the app's own speech overlap in practice. **Known problem:** on the emulator, TalkBack's first focus on a screen did not always land on the screen title. This needs a check on a real phone and may need another approach.
- **Camera error and Retry on a device**, **camera permission denied on a device**, and a missing English voice. The state machine rows for these are tested, but the device paths were only compiled.
- **Arrival and Error screens at 200 percent font.**
- **Stretch goals** (vibration, voice input): not built.
