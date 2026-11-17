# NOVA Expo (phone test version)

A test version of NOVA you can open in **Expo Go**. It checks the voice guidance, the destination choice with TalkBack, and the camera permission. It does **not** read markers.

## Run

```bash
cd nova-expo
npm install
npx expo start
```

Scan the QR code with Expo Go (Android) or the Camera app (iPhone). The phone and the Mac must be on the same Wi-Fi.

## How to test

1. Open the app. It says "NOVA is ready. Where do you want to go?"
2. Turn TalkBack on (Android) and pick `Go to D13`. The app asks for the camera, then starts guidance.
3. Leave **Team mode** on. Press `Marker 0`, `Marker 2`, `Marker 4`, `Marker 6`, `Marker 7`, `Marker 8`, `Marker 10` in turn. Each press acts as if the camera had just read that marker, and the phone speaks the next instruction.
4. Try `Repeat`, `Where am I`, `Stop`, a wrong marker (off route), and waiting without pressing anything (reminders at 12, 27 and 42 seconds, then a help message at 57).

## What is different from the Android app

| Android app | Expo version |
|---|---|
| Reads ArUco markers with OpenCV | No marker reading. Team mode buttons stand in for the camera |
| Fully offline, no network permission | Needs Wi-Fi to load from your Mac while using Expo Go |
| Voice commands, Volume Up | Not included |
| Sighting filter (2 frames, 10 s cooldown) | Not needed. A button press is one accepted sighting |

The guidance logic in `src/core` is a TypeScript port of the Kotlin `:core` module. `npm test` checks it against the same golden sentences. `src/data/*.json` are copies of the Android `map.json` and `markers.json`. Change both together.

This is a test tool. It has not been tested as a safe way to guide a person, and it does not replace a white cane or a guide dog.
