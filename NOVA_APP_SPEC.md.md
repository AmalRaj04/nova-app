# NOVA Android App – Build Specification (for Claude Code)

Nov 14, 2026 · @Amal Raj A

## Purpose and ground rules

This brief tells Claude Code what to build: an offline Android app that guides a visually impaired student by voice along the first-floor D-block route, using printed ArUco markers that the phone camera reads. Everything the MVP needs is here. Where a fact is unknown, the brief gives a default to build with.

**Read in this order:** Scope, Source facts, Open questions, Conventions, MVP map data, Functional requirements, Navigation state machine, Spoken instructions, Screens, Architecture, Configuration, Testing, Build order, Out of scope.

**Ground rules**

1. Build only what the MVP needs. Items under "Out of scope" are not to be built, even if they look easy.
2. Keep every place-specific fact in data files (`map.json`, `markers.json`), never in code. Changing a marker ID, a distance or a room must not need a code change.
3. Do not invent measurements. Numbers here come from the team's plan, or are marked "estimate".
4. Nothing has been tested inside the building. Do not write comments, screen text or docs that claim the app is tested or safe. NOVA gives wayfinding only and does not replace a white cane or guide dog.
5. The app works fully offline. Do not add the INTERNET permission, analytics or any cloud service.
6. The user is blind. Every screen and every state change must work with TalkBack and with speech alone.
7. When something here is unclear, use the default in the Open questions table, record the choice in a file called `DECISIONS.md`, and keep going.
8. Write unit tests for routing, instruction building and marker acceptance as you go. Those parts must be correct even without a phone.
9. Run the build and the tests before saying something works. State plainly what you could not run, such as anything that needs a real camera.

**Who will maintain the code.** First-year engineering students. Use clear names, short functions, comments that explain why, and a `README.md` with exact build and run steps.

## Scope: what the MVP is

The MVP lets a student standing at the building entrance choose D11, D12 or D13, then walk up the main stairs and along the walkway to that room, hearing one spoken instruction at each printed marker. If a marker is missed or a wrong turn is taken, the app recovers by speech.

**In the MVP**

- One Android app in Kotlin, running on a phone only. No ESP32 and no other hardware.
- Three destinations: D11, D12, D13.
- Start at the entrance, or at any marker that comes earlier on the same route. Guidance works in the forward direction only (entrance towards the D block).
- 11 printed ArUco markers, IDs 0 to 10, listed in "MVP map data".
- Live camera, marker detection, and a route found from data files.
- Spoken guidance in English, with a large Repeat button.
- Two recovery behaviours: a reminder when no marker has been seen for a while, and a new route when a marker that is off the planned route is seen.
- Fully offline, screen kept on while guiding.
- A debug mode that can simulate marker scans without a camera, and a local event log for field tests.
- Screens that work with TalkBack.

**Stretch goals** (only after every step in "Build order" is accepted)

- Vibration patterns for left, right and arrival.
- Voice input for choosing the destination (the button list always stays).

**Not in the MVP:** see "Out of scope and known limitations" at the end.

## Source facts and how sure each one is

These facts come from the team's progress update (5 November 2026), the graph file `nova_floor1.json`, and the marker floor plan. Nothing has been tested inside the building. "Measured" means the team measured it. "Plan" means read from the drawing, so treat it as approximate.

| Fact | Value | Source | How sure |
| --- | --- | --- | --- |
| Block length | 30.9 m | Progress update, plan | Measured |
| Block pathway width | 2.35 m | Progress update, plan | Measured |
| Inner hexagon side | 8.5 m | Progress update, plan | Measured |
| Outer hexagon side | 11.75 m | Progress update, plan | Measured |
| Corner, inner to outer hexagon | About 4 m on the plan, 3.5 m in the update, about 3.25 m at true scale | Both documents | Conflicting estimate; little effect on guidance |
| Entrance | 9.1 m wide, three doors of 1.8 m | Plan | Measured |
| Door to first step | 1.35 m | Plan, graph | Measured |
| Main stairs | 16 steps, landing 1.3 m, then 10 steps | Plan, graph | Measured |
| Walkway pillar position | 2.7 m from the outer corner, 1.5 m from the inner hexagon | Plan | Measured |
| D room order | D11, D12, D13, from the hexagon outward | Plan, graph | Plan |
| Door side of D rooms | Right, as seen walking outward from the hexagon | Graph | Plan; to confirm on site |
| Room spacing on D path | 10.3 m (30.9 m divided by 3) | Graph | Derived |
| Number of markers | 11, for the D-block route only | Plan | Plan |
| Marker positions | Approximate on the drawing | Plan | Approximate |
| Stairs step depth | 0.30 m | Graph | Estimate |
| Walking step length | Not measured | None | Unknown; see Open questions |
| Walk distances in the graph | Straight lines between node positions; `steps` is null for walks | Graph | Estimate |

**Two inconsistencies in the source files** (handled in "MVP map data"):

- `nova_floor1.json` starts the D path at the middle of the D side of the hexagon, but the plan shows the D pathway starting at the F-D corner of the walkway, where a marker pillar stands.
- The two stair-landing nodes in the graph are labelled floor 0, although they sit between the entrance and the first floor. Treat `floor` as a label only; never use it in routing.

## Open questions and the default to build with

The team and mentor have not settled these yet. Build with the default, keep the choice in data or in the config file so it can change without code edits, and list each one in `DECISIONS.md`.

| # | Open question | Who decides | Default to build with |
| --- | --- | --- | --- |
| 1 | Do the two markers in each stairs row both face people climbing, or one each way? | Team and mentor | Both face people climbing (bearing 0). Forward travel only |
| 2 | Do the two markers in one stairs row stand for one place? | Team | Yes. Both map to the same node and either one triggers guidance |
| 3 | Real walking step length | Team, after measuring several walkers | 0.70 m, set in config. Stairs always use the counted steps in the map |
| 4 | Use the phone's step sensor between markers? | Mentor | Off. Step counts in speech come from map distances, not from sensors |
| 5 | Which way do the two walkway pillar markers face? | Team | Towards a person walking along the walkway (pillar S-D faces east, pillar F-D faces north-east: bearings 90 and 30) |
| 6 | Where does the D path start: middle of the D side (graph) or the F-D corner (plan)? | Team | The F-D corner, where marker 7 stands |
| 7 | Marker size and height | Team, after printing tests | Not used by the app in the MVP. Keep a config value for the smallest marker width in pixels to accept (default 0, meaning no limit) |
| 8 | What if the chosen room is behind the user? | Mentor | Say it is behind and give the simple message in "Spoken instructions". Do not plan backward guidance |
| 9 | How the phone is carried | Team | Portrait, rear camera, held upright on the chest or on a lanyard |
| 10 | Spoken language | Team | English only. Keep all spoken text in one resource file so Tamil can be added later |
| 11 | Oldest Android version to support | Team, from the phones they own | Android 8.0 (API 26) |
| 12 | App package name | Team | `com.genvision.nova` |
| 13 | Marker dictionary | Team, must match what is printed | `DICT_4X4_50`, IDs 0 to 10 used, 11 to 49 reserved |
| 14 | Spoken hints from an orientation and mobility instructor | Team and instructor | None yet. Every walk in the map has an empty `hint` field and the app must work with it empty |

## Conventions: map frame, headings and turns

Use these rules everywhere in the code and the data, so the numbers match the team's drawing.

- **Map frame.** Same as `nova_floor1.json` and the plan: x points right, y points down, units are metres. "Map north" is up on the plan, towards the G block. It is not real north.
- **Bearing.** An angle in degrees, clockwise from map north, from 0 up to but not including 360. For a move from point 1 to point 2, with dx = x2 minus x1 and dy = y2 minus y1:

```latex
\text{bearing} = \operatorname{atan2}(dx,\,-dy) \bmod 360^\circ
```

- **Edge heading.** The bearing from the edge's start node to its end node.
- **Marker facing (`facingDeg`).** The bearing a person is facing while looking straight at that marker. It is stored per marker in `markers.json`.
- **Turn angle.** The edge heading minus the marker facing, wrapped to the range from minus 180 (exclusive) to plus 180. Positive means turn right, negative means turn left.
- **Marker node.** A node that has at least one marker mapped to it.
- **Leg.** The stretch of the route from one marker node to the next marker node. Guidance is spoken once per leg, at the first marker of the leg. A leg may cover several graph edges, for example a walk across the landing followed by 10 stairs.

**Turn words**

| Absolute turn angle | Spoken as |
| --- | --- |
| 0 to 20 degrees | Walk straight |
| Over 20 up to 50 | Bear right, or bear left |
| Over 50 up to 130 | Turn right, or turn left |
| Over 130 up to 170 | Turn sharp right, or turn sharp left |
| Over 170 | Turn around |

**Distances to steps**

- For a stairs edge, use its `steps` value from the map exactly.
- For a walk edge, steps = distance in metres divided by the configured walking step length, rounded to the nearest whole number (halves round up).
- A walk under 0.5 m is not mentioned.
- A leg's walking steps are the sum of its walk edges, rounded once at the end, not edge by edge.

**Door side.** `doorSide` is given as seen walking in the forward direction of the route (outward from the hexagon). The app speaks "on your right" or "on your left" using that value as it is.

## MVP map data

The app reads two files from `app/src/main/assets/`: `map.json` (places and walks) and `markers.json` (which printed marker stands at which place). The route below is the entrance to the D block. It uses `nova_floor1.json` for the stairs and room spacing, and the marker plan for marker positions. The JSON files in this section are the source of truth. If the table and the JSON ever disagree, the JSON wins and the table should be corrected.

**How the MVP route was derived**

- The ring and the B, H, G, F blocks from `nova_floor1.json` are left out. They have no markers yet.
- Markers 6 and 7 sit at two corners of the walkway. Read from the plan, they are at the corners of a hexagon with 10.13 m sides, halfway between the inner and outer hexagons. Their coordinates (5.07, 8.77) and (10.13, 0.0) come from that.
- D11, D12 and D13 use the marker pillar positions P1, P2 and P3 from the plan, not the room centres in `nova_floor1.json`. They are placed on a straight line at bearing 120 degrees from marker 7, 7.9 m, then 10.3 m, then 10.3 m apart. These are estimates from the drawing.
- The map is one-way. Edges only go forward, from the entrance towards the D block.

**The route as a chain**

| # | Place | Marker IDs | Facing at scan | Next leg | Heading | Turn at this marker |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | Entrance | 0, 1 | 0 | Walk 1.35 m, then climb 16 steps | 0 | Straight |
| 2 | Stairs landing | 2, 3 | 0 | Walk 1.3 m, then climb 10 steps | 0 | Straight |
| 3 | Top of stairs | 4, 5 | 0 | Walk 5.26 m | 74.5 | Right |
| 4 | Walkway, stairs-D corner | 6 | 90 | Walk 10.13 m | 30 | Left |
| 5 | Walkway, F-D corner (D path start) | 7 | 30 | Walk 7.9 m | 120 | Right |
| 6 | D11 (pillar P1) | 8 | 120 | Walk 10.3 m | 120 | Straight |
| 7 | D12 (pillar P2) | 9 | 120 | Walk 10.3 m | 120 | Straight |
| 8 | D13 (pillar P3) | 10 | 120 | End of route | none | none |

**`map.json`**

```json
{
  "meta": {"version": 1, "route": "Entrance to D block, first floor", "frame": "x right, y down, metres; bearing clockwise from plan up", "directed": true},
  "nodes": [
    {"id": "ENTRANCE_PLAZA", "name": "the entrance", "type": "junction", "floor": 0, "x": 0.0, "y": 20.63, "doorSide": null},
    {"id": "STAIRS_FOOT", "name": "the foot of the main stairs", "type": "stairs", "floor": 0, "x": 0.0, "y": 19.28, "doorSide": null},
    {"id": "STAIRS_LANDING_DOWN", "name": "the stairs landing", "type": "stairs", "floor": 0, "x": 0.0, "y": 14.48, "doorSide": null},
    {"id": "STAIRS_LANDING_UP", "name": "the upper end of the landing", "type": "stairs", "floor": 0, "x": 0.0, "y": 13.18, "doorSide": null},
    {"id": "MAIN_STAIRS_TOP", "name": "the top of the main stairs", "type": "stairs", "floor": 1, "x": 0.0, "y": 10.18, "doorSide": null},
    {"id": "WALK_SD", "name": "the walkway corner near the stairs", "type": "junction", "floor": 1, "x": 5.07, "y": 8.77, "doorSide": null},
    {"id": "WALK_FD", "name": "the start of the D block path", "type": "junction", "floor": 1, "x": 10.13, "y": 0.0, "doorSide": null},
    {"id": "D11", "name": "D11", "type": "room", "floor": 1, "x": 16.97, "y": 3.95, "doorSide": "right"},
    {"id": "D12", "name": "D12", "type": "room", "floor": 1, "x": 25.89, "y": 9.10, "doorSide": "right"},
    {"id": "D13", "name": "D13", "type": "room", "floor": 1, "x": 34.81, "y": 14.25, "doorSide": "right"}
  ],
  "edges": [
    {"from": "ENTRANCE_PLAZA", "to": "STAIRS_FOOT", "weight": 1.35, "kind": "walk", "steps": null, "hint": ""},
    {"from": "STAIRS_FOOT", "to": "STAIRS_LANDING_DOWN", "weight": 4.8, "kind": "stairs", "steps": 16, "hint": ""},
    {"from": "STAIRS_LANDING_DOWN", "to": "STAIRS_LANDING_UP", "weight": 1.3, "kind": "walk", "steps": null, "hint": ""},
    {"from": "STAIRS_LANDING_UP", "to": "MAIN_STAIRS_TOP", "weight": 3.0, "kind": "stairs", "steps": 10, "hint": ""},
    {"from": "MAIN_STAIRS_TOP", "to": "WALK_SD", "weight": 5.26, "kind": "walk", "steps": null, "hint": ""},
    {"from": "WALK_SD", "to": "WALK_FD", "weight": 10.13, "kind": "walk", "steps": null, "hint": ""},
    {"from": "WALK_FD", "to": "D11", "weight": 7.9, "kind": "walk", "steps": null, "hint": ""},
    {"from": "D11", "to": "D12", "weight": 10.3, "kind": "walk", "steps": null, "hint": ""},
    {"from": "D12", "to": "D13", "weight": 10.3, "kind": "walk", "steps": null, "hint": ""}
  ],
  "destinations": ["D11", "D12", "D13"]
}
```

**`markers.json`**

In "left strip" and "right strip", left and right are as seen by a person climbing the stairs.

```json
{
  "meta": {"version": 1, "dictionary": "DICT_4X4_50"},
  "markers": [
    {"id": 0, "nodeId": "ENTRANCE_PLAZA", "facingDeg": 0, "where": "Entrance row, left strip between the stair lanes"},
    {"id": 1, "nodeId": "ENTRANCE_PLAZA", "facingDeg": 0, "where": "Entrance row, right strip"},
    {"id": 2, "nodeId": "STAIRS_LANDING_DOWN", "facingDeg": 0, "where": "Long step (landing) row, left strip"},
    {"id": 3, "nodeId": "STAIRS_LANDING_DOWN", "facingDeg": 0, "where": "Long step (landing) row, right strip"},
    {"id": 4, "nodeId": "MAIN_STAIRS_TOP", "facingDeg": 0, "where": "Door wall row at the top of the stairs, left strip"},
    {"id": 5, "nodeId": "MAIN_STAIRS_TOP", "facingDeg": 0, "where": "Door wall row, right strip"},
    {"id": 6, "nodeId": "WALK_SD", "facingDeg": 90, "where": "Walkway pillar near the stairs-D corner"},
    {"id": 7, "nodeId": "WALK_FD", "facingDeg": 30, "where": "Walkway pillar at the F-D corner, start of the D path"},
    {"id": 8, "nodeId": "D11", "facingDeg": 120, "where": "D path pillar P1, beside D11"},
    {"id": 9, "nodeId": "D12", "facingDeg": 120, "where": "D path pillar P2, beside D12"},
    {"id": 10, "nodeId": "D13", "facingDeg": 120, "where": "D path pillar P3, beside D13"}
  ]
}
```

**Data checks the app must run when it loads the files.** If any check fails, show and speak a clear error and do not start guiding.

- Every edge's `from` and `to` is an existing node.
- Every marker's `nodeId` is an existing node, and marker IDs are unique.
- Every destination is an existing node of type `room`.
- Every edge has `weight` above 0, and every `stairs` edge has a `steps` value.
- Every destination can be reached from the node of marker 0.

## Functional requirements

Each requirement has an ID and an acceptance criterion. A requirement is finished only when its criterion is met by a test or by a described manual check. Constants in brackets, such as \[noMarkerReminderMs\], are in the Configuration section.

| ID | Requirement | Acceptance criterion |
| --- | --- | --- |
| FR-01 | On start, load `map.json` and `markers.json` from assets and run the data checks. | Valid files: the Home screen opens and speaks "NOVA is ready. Where do you want to go?". Invalid files: a clear error is shown and spoken, and guidance cannot start. |
| FR-02 | Ask for camera permission at runtime when guidance first needs it. | If denied, the app explains why by speech and text and offers a button to open settings. It does not crash. |
| FR-03 | Home screen lists one large button for each destination in `map.json`. | Buttons are in file order, each has a spoken label, and selecting one starts a guidance session. |
| FR-04 | Run live camera analysis while guiding. | The detector receives frames at least \[analysisIntervalMs\] apart, and old frames are dropped, never queued. |
| FR-05 | Detect ArUco markers of the dictionary named in `markers.json`. | A printed marker is reported with its ID and its corner points. IDs not in `markers.json` are logged and ignored. |
| FR-06 | Accept a marker only after repeated sightings. | A marker ID counts as a sighting only when seen in \[confirmFrames\] analysed frames within \[confirmWindowMs\]. A single-frame detection does nothing. |
| FR-07 | Ignore repeats of the same place. | After a sighting at a node, further sightings at that node are ignored for \[sameNodeCooldownMs\], unless a different node was sighted in between. |
| FR-08 | On a sighting, set the current place and facing from `markers.json`. | The current node and facing are updated, and the sighting is logged. |
| FR-09 | Plan a route from the current node to the destination with Dijkstra on the one-way graph, using `weight`. | For every destination from marker 0, the planned node list matches the chain in "MVP map data". If no forward path exists, the "behind you" message is spoken. |
| FR-10 | Build and speak one instruction per leg at the first sighting of a leg (see "Spoken instructions"). | The spoken text for each leg of the demo route equals the expected text in that section, with the default step length. |
| FR-11 | Handle arrival. | On a sighting at the destination, speak the arrival message with the door side, stop guiding, and show a large "Choose another destination" button. |
| FR-12 | A large Repeat button speaks the last instruction again. | Pressing it repeats the exact last spoken instruction. It does nothing before the first instruction. |
| FR-13 | A large "Where am I?" button speaks the last confirmed place and how long ago it was seen. | Example: "Last marker: the stairs landing, 12 seconds ago." Before any sighting: "No marker seen yet." |
| FR-14 | No-marker reminder. | While guiding, if no sighting happens for \[noMarkerReminderMs\] after the last instruction, speak the reminder. Repeat every \[reminderRepeatMs\], at most \[maxReminders\] times, then speak the help message once. Any sighting resets the count. |
| FR-15 | Off-route handling. | A sighting at a node that is not ahead on the planned route triggers a new plan from that node and the matching message (see the state table). |
| FR-16 | Speech output through Android TextToSpeech, English. | A new instruction interrupts the previous one, except that an arrival message is never interrupted. If English speech is unavailable, show an error. |
| FR-17 | Keep the screen on and the app in portrait while guiding. | The screen does not time out during a session. Leaving the app stops the camera, and returning resumes it. |
| FR-18 | Developer mode, off by default. | When on: camera preview with detected marker IDs, one button per marker ID that simulates a sighting, the current state, and the log. A simulated sighting enters the same code path as a real one from FR-08 onwards. |
| FR-19 | Local event log. | A text file in app storage records each event with a timestamp: app start, destination chosen, sighting (marker ID and node), spoken text, reminder, off-route, arrival, error. No camera images are ever stored. Developer mode can export the file with the system share sheet. |
| FR-20 | Settings screen. | Step length, speech rate and developer mode can be changed, are saved, and take effect on the next instruction. |
| FR-21 | No silent failures. | A camera error, speech error or data error is shown and spoken, with a Retry or Back action. |

## Navigation state machine

Implement the app's guidance logic as one pure state machine, with no Android classes inside it, so it can be unit tested on a laptop. It takes events in and gives out actions such as "speak this text". "Sighting at N" means an accepted marker sighting (FR-06 and FR-07) at node N. "Ahead" means later in the planned route than the current node. "Last instruction" is the last leg instruction or off-route message, not a reminder.

| # | State | Event | Condition | Actions | Next state |
| --- | --- | --- | --- | --- | --- |
| 1 | LOADING | Data loaded | Checks passed | Speak the ready prompt | HOME |
| 2 | LOADING | Data loaded | Checks failed | Show and speak the error | ERROR |
| 3 | HOME | Destination chosen | Camera permission granted | Start camera, speak "Going to {destination}. Point the phone forward at a marker." | WAITING |
| 4 | HOME | Destination chosen | Permission not granted | Ask for permission | PERMISSION |
| 5 | PERMISSION | Permission granted | None | Same actions as row 3 | WAITING |
| 6 | PERMISSION | Permission denied | None | Explain, offer a settings button | HOME |
| 7 | WAITING | Sighting at N | N is the destination | Speak the arrival message | ARRIVED |
| 8 | WAITING | Sighting at N | A forward path from N to the destination exists | Plan the route, speak the instruction for the leg that starts at N | GUIDING |
| 9 | WAITING | Sighting at N | No forward path exists | Speak the "behind you" message | WAITING |
| 10 | GUIDING | Sighting at N | N is the destination | Speak the arrival message | ARRIVED |
| 11 | GUIDING | Sighting at N | N is ahead on the planned route | Speak the instruction for the leg that starts at N | GUIDING |
| 12 | GUIDING | Sighting at N | N is earlier on the route than the current node | Speak "You are back at {N name}.", plan again from N, speak the instruction for the leg that starts at N | GUIDING |
| 13 | GUIDING | Sighting at N | N is not on the planned route | Speak "This is not on your route. New route.", plan again from N, then speak the instruction. If no forward path exists, speak the "behind you" message instead | GUIDING |
| 14 | WAITING or GUIDING | Time since last sighting or instruction reaches \[noMarkerReminderMs\], then every \[reminderRepeatMs\] | Reminders spoken is below \[maxReminders\] | Speak the no-marker reminder | Same |
| 15 | WAITING or GUIDING | Reminder time reached | Reminders spoken equals \[maxReminders\] | Speak the help message once | Same |
| 16 | GUIDING | Repeat pressed | None | Speak the last instruction | Same |
| 17 | WAITING or GUIDING | Where am I pressed | None | Speak the last confirmed place and its age | Same |
| 18 | WAITING or GUIDING | Stop pressed | None | Stop camera, speak "Guidance stopped." | HOME |
| 19 | ARRIVED | Choose another destination pressed | None | Stop camera | HOME |
| 20 | Any | Camera, speech or data error | None | Show and speak the error with a Retry action | ERROR |
| 21 | ERROR | Retry pressed | None | Run the failed step again | The state before the error, or HOME |
| 22 | WAITING or GUIDING | App sent to background | None | Pause camera, keep the state | Same, camera resumes in foreground |

**Rules that apply to every row**

- Reminder counts and timers reset on every accepted sighting and on every spoken instruction.
- Any spoken text is also written to the event log.
- The planned route is a list of node IDs. Instructions are built from the route and the map, never typed into the code.

## Spoken instructions

Speech is the main output, so the wording is part of the specification. Keep every spoken sentence in one place (a string resource file or one `Phrases` object) and build instructions from the templates below. Short sentences, one action each, turn before distance.

**Building a leg instruction**

1. Start with the place sentence: "You are at {name}." or, when the node type is `room`, "You are beside {name}."
2. Add the movement sentences for the leg, in the order of the edges:
   - Straight, then walking: "Walk straight about {n} steps."
   - A turn, then walking: "{Turn word}." then "Walk about {n} steps."
   - Stairs after a walk: "Then climb {s} steps up." The stairs step count is exact, so it has no "about".
3. If the leg's edges have non-empty `hint` text, add each hint as its own final sentence.
4. Numbers are spoken as digits. The turn word and step rules are in "Conventions".

In the MVP, stairs always go up. If a route ever needs stairs going down, add a `stairsDirection` field to the edge instead of guessing from the floor labels.

**Fixed phrases**

| Situation | Spoken text |
| --- | --- |
| Home screen opens | NOVA is ready. Where do you want to go? |
| Destination chosen | Going to {destination}. Point the phone forward at a marker. |
| Arrival | You have arrived. {destination} is on your {side}. |
| Destination is behind | {destination} is behind you. This version cannot guide backwards. Ask someone nearby to help, or return to the entrance. |
| Off the planned route | This is not on your route. New route. |
| Back at an earlier place | You are back at {name}. |
| No marker for a while | I have not seen a marker. Point the phone forward and move slowly. |
| Help after repeated reminders | Still no marker. Press Repeat, or ask someone nearby for help. |
| Where am I | Last marker: {name}, {n} seconds ago. |
| Where am I, nothing seen yet | No marker seen yet. |
| Stop pressed | Guidance stopped. |

**Expected output for the demo route** (default step length 0.70 m). Use these as the golden strings in unit tests.

| Marker | Place | Destination | Expected spoken text |
| --- | --- | --- | --- |
| 0 or 1 | Entrance | D13 | You are at the entrance. Walk straight about 2 steps. Then climb 16 steps up. |
| 2 or 3 | Stairs landing | D13 | You are at the stairs landing. Walk straight about 2 steps. Then climb 10 steps up. |
| 4 or 5 | Top of stairs | D13 | You are at the top of the main stairs. Turn right. Walk about 8 steps. |
| 6 | Walkway corner near the stairs | D13 | You are at the walkway corner near the stairs. Turn left. Walk about 14 steps. |
| 7 | Start of the D path | D13 | You are at the start of the D block path. Turn right. Walk about 11 steps. |
| 8 | D11 | D12 or D13 | You are beside D11. Walk straight about 15 steps. |
| 9 | D12 | D13 | You are beside D12. Walk straight about 15 steps. |
| 8 | D11 | D11 | You have arrived. D11 is on your right. |
| 9 | D12 | D12 | You have arrived. D12 is on your right. |
| 10 | D13 | D13 | You have arrived. D13 is on your right. |

**Check how the phone says room names.** Text-to-speech may read "D11" in an odd way. During the first phone test, listen to it. If it sounds wrong, add an optional `spoken` field to nodes (for example "D eleven") and use it for speech only, keeping `name` for the screen.

## Screens and accessibility

Use Jetpack Compose with a single activity. The student cannot see the screen, so these screens are mostly for TalkBack and for sighted helpers, and every state change is also spoken.

| Screen | Contents | Behaviour |
| --- | --- | --- |
| Home | App title, one large button per destination (D11, D12, D13), a Settings button | Opens with the ready prompt. Choosing a destination starts guidance (FR-03). |
| Guidance | Large text with the destination and the last instruction, then three full-width buttons: Repeat (the largest), Where am I, Stop | Camera runs in the background with no preview. Shows no camera picture unless developer mode is on. Instruction text is large (at least 28 sp). |
| Arrival | Large text "You have arrived" and one button, Choose another destination | Replaces the Guidance screen on arrival. |
| Settings | Walking step length (buttons for plus and minus 0.05 m), speech rate, developer mode switch | Values are saved. The developer switch is labelled "For the team". |
| Developer panel (only when developer mode is on) | Camera preview with boxes and IDs around detected markers, one button per marker ID 0 to 10 that simulates a sighting, the current state and node, the last 50 log lines, an Export log button | Simulated sightings behave as in FR-18. |
| Error | Message, Retry button, Back button | Message is also spoken (FR-21). |

**Accessibility requirements**

1. Every button and text has a clear label that TalkBack reads, and a correct role. Do not use icon-only buttons.
2. Main buttons are at least 72 dp tall and full width. Nothing important is smaller than 48 dp.
3. Use one fixed high-contrast theme: light text on a dark background, a contrast ratio of at least 7 to 1 for main text. Do not use dynamic colour. Never use colour alone to carry meaning.
4. Text follows the system font size. The layout must stay usable at 200 percent font size.
5. Nothing is time-limited, nothing needs a swipe or a long press, and nothing disappears by itself.
6. Focus order is logical, and the first focus on each screen is the screen title.
7. **TalkBack and the app's own speech must not talk over each other.** When touch exploration is on, the Home prompt is announced through the accessibility system. The app's own text-to-speech is used for guidance on the Guidance screen. Check this with the `AccessibilityManager` and test both ways.
8. Keep the screen on during a session (FR-17).
9. Every state change in the state table produces speech, not only a screen update.

## Architecture and technology

Keep the logic that decides where the student is and what to say in a plain Kotlin module with no Android code in it. Only the camera, speech, storage and screens live in the Android module. This keeps the important parts testable on a laptop and keeps the OpenCV dependency in one small class.

| Part | Choice | Note |
| --- | --- | --- |
| Language | Kotlin |  |
| Modules | `:core` (pure Kotlin/JVM) and `:app` (Android) | `:app` depends on `:core`, never the other way round |
| UI | Jetpack Compose, single activity, Material 3 | Fixed high-contrast theme |
| Pattern | MVVM with `StateFlow`, plain constructor injection | No dependency injection framework |
| Camera | CameraX: `ImageAnalysis` always, `Preview` only in developer mode | Back camera |
| Marker detection | OpenCV for Android, ArUco detector | The only OpenCV user is `ArucoMarkerDetector` |
| JSON | kotlinx.serialization | Reads the two asset files |
| Concurrency | Coroutines and Flow. Camera analysis on one background executor |  |
| Speech | Android `TextToSpeech` behind a `Speaker` interface | A fake `Speaker` is used in tests |
| Settings | Jetpack DataStore (or SharedPreferences) |  |
| Log | Text file in `filesDir/logs/` | Rotate at about 1 MB |
| Tests | JUnit and kotlin.test in `:core`; a few instrumented tests in `:app` |  |
| Build | Gradle Kotlin DSL with a version catalog | `minSdk` 26, `targetSdk` and `compileSdk` the current stable at build time |

Use the current stable version of every library at build time. Do not copy versions from old tutorials. List the chosen versions in `README.md`. Declare only the `CAMERA` permission, plus `VIBRATE` if the stretch goal is built. Do not declare `INTERNET`.

**Package layout**

```text
core/   (no Android imports)
  model/     MapData, Node, Edge, Marker
  data/      MapLoader (parse and validate)
  geometry/  Bearing, TurnClassifier
  route/     RouteFinder (Dijkstra), Leg, LegBuilder
  speech/    Phrases, InstructionBuilder
  sighting/  SightingFilter (confirm frames, cooldown)
  nav/       NavigationMachine (state, events, actions), Clock
app/
  camera/    CameraAnalyzer, MarkerDetector (interface),
             ArucoMarkerDetector, FakeMarkerDetector
  speech/    AndroidSpeaker
  log/       EventLog
  settings/  SettingsStore
  ui/        NovaTheme, HomeScreen, GuidanceScreen, ArrivalScreen,
             SettingsScreen, DevPanel, ErrorScreen
  NovaViewModel, MainActivity
```

**Data flow**

1. CameraX delivers a frame to `CameraAnalyzer`, which drops frames to keep at least \[analysisIntervalMs\] between detections.
2. `MarkerDetector` returns the marker IDs found in the frame, with corner points.
3. `SightingFilter` applies FR-06 and FR-07 and emits an accepted sighting (a marker ID).
4. `NavigationMachine` receives the sighting, looks up the node and facing, plans or continues the route, and returns a list of actions: speak text, update the screen state, start or reset a timer, write a log line.
5. `NovaViewModel` carries out those actions with `Speaker`, the UI state flow and `EventLog`.

Simulated sightings from developer mode (FR-18) enter at step 4. A `Clock` interface gives time to every timer, so tests can use a fake clock.

**Camera and detection details**

- Use `ImageAnalysis` with the keep-only-latest strategy and a target of about 1280 by 720. Make the size a config value so the team can trade distance against speed.
- Use the luminance (Y) plane directly as the grey image. Respect the row stride when copying it into an OpenCV matrix, because the stride can be larger than the width.
- ArUco detection works at any rotation, so frame rotation is not needed for detection. It is needed only for the developer overlay.
- Always close the image in a `finally` block. Reuse matrices between frames and release them when analysis stops.
- Start the camera only in the WAITING and GUIDING states, and stop it on leaving them or when the app goes to the background.
- Start with OpenCV's default detector parameters. Change them only because of field-test results, and record the change in `DECISIONS.md`.
- OpenCV's Android setup and Java class names change between versions. Follow the official instructions for the version you pick, and check the ArUco class names against that version. If you cannot get OpenCV running in your environment, finish everything else against `FakeMarkerDetector`, say clearly what is missing, and leave the detector as the one remaining task.
- `TextToSpeech` starts asynchronously. Queue speech until it reports ready. Check that English is available and show an error if not.

## Configuration constants

Put these in one `Config` file in `:core`, with the user-changeable ones also stored in settings. They are starting values chosen without field data. The team will tune them after the first walk-through, so no other code may hard-code them.

| Name | Default | Meaning |
| --- | --- | --- |
| `analysisIntervalMs` | 150 | Smallest time between two marker detections |
| `analysisWidth` and `analysisHeight` | 1280 and 720 | Camera analysis size in pixels |
| `confirmFrames` | 2 | Analysed frames that must show the same marker to accept it |
| `confirmWindowMs` | 1000 | Time window in which those frames must fall |
| `sameNodeCooldownMs` | 10000 | Ignore repeat sightings at one node for this long |
| `minMarkerSidePx` | 0 | Smallest marker width, in pixels, to accept. 0 means no limit |
| `walkingStepLengthM` | 0.70 | Metres per walking step. Setting range 0.50 to 0.90 |
| `minWalkMentionM` | 0.5 | Walks shorter than this are not spoken |
| `straightMaxDeg` | 20 | Largest turn angle spoken as straight |
| `bearMaxDeg` | 50 | Largest turn angle spoken as bear |
| `turnMaxDeg` | 130 | Largest turn angle spoken as turn |
| `sharpMaxDeg` | 170 | Largest turn angle spoken as sharp turn. Above it, turn around |
| `noMarkerReminderMs` | 12000 | Silence before the first no-marker reminder |
| `reminderRepeatMs` | 15000 | Time between reminders |
| `maxReminders` | 3 | Reminders before the help message |
| `speechRate` | 1.0 | Text-to-speech rate. Setting range 0.7 to 1.5 |
| `developerMode` | off | Shows the developer panel |
| `logMaxBytes` | 1000000 | Size at which the log file rotates |

## Testing

The whole `:core` module must be covered by automated tests that run with `./gradlew test` and need no phone. Android parts get manual checks, because nothing here has been tried on a real phone or in the building.

**Automated tests in `:core`**

| Area | Tests that must exist |
| --- | --- |
| Geometry | Bearing of north, east, south and west vectors gives 0, 90, 180, 270. The three demo headings come out as 74.5, 30 and 120 within 0.1. Turn angles wrap correctly (facing 350 to heading 10 is plus 20; facing 10 to heading 350 is minus 20). |
| Turn words | Each boundary (20, 50, 130, 170) gives the word it should, with the upper limit included in the lower band. |
| Map loading | The two files in this brief load. Each data check fails with a clear message when its rule is broken: unknown node, duplicate marker ID, stairs edge with no `steps`, unreachable destination. |
| Routing | From the node of marker 0, the routes to D11, D12 and D13 match the chain in "MVP map data". Going from D13 to D11 finds no route. |
| Instruction text | Every row of the golden table in "Spoken instructions" is reproduced exactly. Rounding halves up. Walks are summed before rounding. A non-empty hint is added last. Changing the step length to 0.60 changes the step counts. |
| Sighting filter | One frame does not accept. Two frames inside the window accept. Two frames outside it do not. The cooldown ignores repeats. A different node in between resets the cooldown. |
| Navigation machine | Every row 1 to 22 of the state table has at least one test using a fake clock. Reminders come at 12, 27 and 42 seconds, the help message at 57 seconds, and nothing after that. Rows 12 and 13 (back at an earlier place, off route) and row 9 (behind you) are covered. |

**Manual checks without a camera** (developer mode)

1. Choose D13. Press simulated markers 0, 2, 4, 6, 7, 8, 10 in order. The spoken text must equal the golden table, ending with the arrival message.
2. Choose D12. Press 0, then 6 (skipping markers). The app must speak the instruction for marker 6 without complaint, since 6 is ahead on the route.
3. Choose D13. Press 0, 2, 4, then 2 again. The app must say "You are back at the stairs landing." and continue from there.
4. Choose D11. Press 10. The app must say D11 is behind you.
5. Choose D13, press 0 and wait. Reminders must come at 12, 27 and 42 seconds, then the help message at 57 seconds.
6. Press Repeat, Where am I and Stop at each stage and check the speech.

**Manual checks on a real phone**

- Point the phone at each printed marker in developer mode. The ID overlay must match. Record, for each marker, the farthest distance at which it is accepted. This is the data the team needs to choose marker size and height.
- Listen to how text-to-speech says "D11", "D12", "D13" and step counts, and fix pronunciation with the `spoken` field if needed.
- Go through every screen with TalkBack on, and again at 200 percent font size.
- Run guidance for 15 minutes with the camera on. Note battery drop and how warm the phone gets.
- Test on the newest and the oldest phone the team owns.

**Log format.** One line per event: an ISO timestamp, the event name, then `key=value` pairs. The team will read these logs after field tests to find missed markers, long gaps and repeated reminders.

```text
2026-11-20T10:15:32.120 app_start version=1
2026-11-20T10:15:40.502 destination_chosen id=D13
2026-11-20T10:15:43.870 sighting marker=0 node=ENTRANCE_PLAZA
2026-11-20T10:15:43.900 speak text="You are at the entrance. Walk straight about 2 steps. Then climb 16 steps up."
2026-11-20T10:15:55.910 reminder count=1
2026-11-20T10:16:21.015 off_route marker=6 node=WALK_SD
2026-11-20T10:17:30.660 arrival node=D13
```

## Build order

Build in this order. Finish and commit each step before the next. After each step, run the tests, update `DECISIONS.md`, and write a short note of what was run and what could not be run. If a step is blocked, stop and report it instead of working around it.

| Step | Build | Accepted when |
| --- | --- | --- |
| 0 | Project skeleton: the two Gradle modules, `README.md`, `DECISIONS.md`. Put `map.json` and `markers.json` from this brief in `app/src/main/assets/` and in the `:core` test resources. | `./gradlew test` and `./gradlew assembleDebug` both succeed. |
| 1 | Core model and `MapLoader` with all data checks. | Loading tests pass, including each failing check. |
| 2 | Geometry and turn words. | Geometry and turn-word tests pass. |
| 3 | `RouteFinder`, `LegBuilder`, `Phrases` and `InstructionBuilder`. | The routing and golden instruction tests pass. |
| 4 | `SightingFilter`, `Clock` and `NavigationMachine`. | Every row of the state table has a passing test. |
| 5 | Android shell: theme, all screens, `NovaViewModel`, `AndroidSpeaker`, `SettingsStore`, `EventLog`, `FakeMarkerDetector`, and the developer panel with simulate buttons. | Manual checks 1 to 6 (no camera) pass on a phone or on an emulator with a working speech engine. The log file shows the expected lines. |
| 6 | Camera permission and CameraX analysis with the throttle, no OpenCV yet. | The developer panel shows the analysis rate. Leaving and returning to the app stops and restarts the camera cleanly. |
| 7 | OpenCV ArUco detector, the developer overlay, and `tools/make_markers.py` that writes marker images 0 to 10 with a white border and the ID printed below. | An instrumented test detects a generated marker image and returns the right ID. A printed marker is read live. |
| 8 | Accessibility pass against the requirements. | TalkBack, 200 percent font size and the overlap rule (requirement 7) are checked and noted. |
| 9 | Hardening: error handling, lifecycle, 15-minute run, `README.md` with exact build, run, install and marker-printing steps. | All tests pass. The 15-minute run is recorded. The README works when followed by someone new. |
| 10 | Stretch goals, only if steps 0 to 9 are accepted: vibration patterns, then voice destination input. | The old behaviour still passes its tests. Both are off or fall back safely. |

**Deliverables at the end**

- The source code in two modules, building cleanly.
- `README.md`, `DECISIONS.md` and `tools/make_markers.py`.
- A short test report listing what was run, what passed, and what could not be run (anything needing a real camera, the real building or a TalkBack user).

## Out of scope and known limitations

**Do not build these in the MVP**

- Blocks B, H, G and F, and the hexagon ring. They have no markers yet. Keep `nova_floor1.json` untouched as the reference for later.
- Guidance in the backward direction (from the D block back to the entrance).
- Any floor other than the entrance, stairs and first floor.
- Reading room plates (OCR), any language other than English, and any extra hardware such as an ESP32 or Bluetooth device.
- Obstacle detection, GPS, Wi-Fi or Bluetooth positioning, and live step counting from the phone's sensors.
- Accounts, cloud services, analytics, and any network access.
- A map editor or admin tool.
- Storing, sending or uploading camera images.

**Limitations the README and the app's about text must state honestly**

- The app has not been tested in the building. Marker positions come from an approximate drawing, distances are estimates, and the walking step length is not yet measured.
- The app only speaks at markers. Between two markers it says nothing except reminders, so a student can drift off course without being told. Marker density is the main protection.
- Whether the stairs-strip markers and the pillar markers can be read from the walking lane is not yet known.
- Marker reading can fail in glare, low light or at a sharp angle, and can occasionally read the wrong ID. The confirmation rule reduces this but does not remove it.
- A phone swinging in a chest holder can blur the camera image.
- Step counts are rough, because people's steps differ.
- NOVA does not warn about people, open doors, glass, steps or other obstacles. It does not replace a white cane or a guide dog, and every test must have a sighted person beside the tester.
- Guidance is one-way and English only.
