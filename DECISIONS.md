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
