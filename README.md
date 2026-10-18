# Dot Battle — Native Android Game

A competitive, polished Dots and Boxes strategy game built for Android with Jetpack Compose, Material Design 3, deterministic game engine, 3-tier heuristic AI (Easy, Medium, Hard), multi-player arena battles with up to 4 players, and real-time online multiplayer.

---

## Features

1. **Deterministic Game Engine**
   - Pure Kotlin implementation independent of Android UI for complete testability.
   - 3 board grid sizes: Small (4×4), Medium (6×6), and Large (8×8).
   - Independent horizontal and vertical edge tracking.
   - Dual-box completion support awarding 2 points in a single move.
   - Extra-turn mechanics and win/draw evaluation.

2. **Single-Player vs AI**
   - **Easy**: Casual play with randomized choices and occasional captures.
   - **Medium**: Tactical opportunist that greedily captures available boxes and avoids creating 3rd sides.
   - **Hard**: Endgame minimax with alpha-beta pruning, chain sacrifice optimization, and double-cross avoidance within a 250ms time budget.

3. **Multi-AI Arena Battles**
   - 2, 3, or 4 players on a single board with unique colors and labels.
   - Human vs multiple AI opponents or pure AI-only simulation.
   - Pause, resume, and step controls for AI matches.

4. **Online Multiplayer**
   - **Quick Match**: Global queue matchmaking.
   - **Private Rooms**: 6-character room codes for friends to join.
   - State synchronization with idempotent sequence tracking.

5. **Visual Polish & Audio/Haptics**
   - Modern Dark theme with energetic orange (`#E66239`) accents and electric cyan/lime/purple player palette.
   - Touch-expanded hitboxes (26dp radius) for effortless mobile edge tapping.
   - Dynamic line drawing animations and box capture pulses.
   - Integrated sound feedback and vibration haptics.
   - Local persistence with Room Database and DataStore.

---

## Build & Test Instructions

### Running Tests
Execute the local JVM test suite including engine rules, scoring, box completions, and AI heuristics:
```bash
gradle :app:testDebugUnitTest
```

### Building the Debug APK
```bash
gradle :app:assembleDebug
```
The resulting APK is generated at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## Firebase Realtime Database Setup

To link a custom Firebase project for cloud online multiplayer:
1. Create a project in the [Firebase Console](https://console.firebase.google.com).
2. Enable **Anonymous Authentication** under *Authentication > Sign-in method*.
3. Enable **Realtime Database** under *Build > Realtime Database*.
4. Paste the security rules from `firebase-rules.json` into the Database Rules tab.
5. Download `google-services.json` and place it in the `/app` folder.
