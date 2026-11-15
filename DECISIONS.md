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
