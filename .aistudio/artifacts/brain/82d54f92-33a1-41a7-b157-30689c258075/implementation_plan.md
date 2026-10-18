# Dot Battle — Native Android Strategy Game

Dot Battle is a full-featured, competitive Dots and Boxes strategy game built with Jetpack Compose and Kotlin, featuring deterministic game rules, 3-tier heuristic AI (Easy, Medium, Minimax-based Hard), multi-AI arena battles with up to 4 players, online multiplayer lobby and rooms, and offline match recovery.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following defaults have been selected to ensure full compliance with the user's brief and `design.md` token specification:
> - **Visual Identity**: Modern Dark with warm neon orange (`#E66239`) accents, high-contrast dark surfaces (`#000000`, `#171717`, `#262626`), clean tokenized borders, and bundled Poppins typography.
> - **Multiplayer Architecture**: Clean modular network layer designed for Firebase Realtime Database with room codes, matchmaking queue, and idempotent move synchronizer, coupled with an interactive offline peer simulation mode so online features remain fully playable even before Firebase keys are provisioned.
> - **Board Sizing**: Medium 6×6 grid default with Small (4×4) and Large (8×8) options.
> - **Persistence**: Room Database and DataStore for persisting audio/haptics settings, tutorial progress, and ongoing offline match recovery.

---

## 1. Overview & Core Concept

- **What It Does**: Dot Battle delivers the classic turn-based "Dots and Boxes" game. Players take turns drawing lines between adjacent grid dots. Closing the 4th wall of any 1×1 box captures it, scores 1 point, and grants an immediate bonus turn. A single move can capture up to 2 boxes simultaneously. The player with the highest box count when the grid is full wins.
- **Game Modes**:
  1. **Play vs AI**: 1 Human vs AI across 3 distinct difficulty tiers (Easy, Medium, Hard with alpha-beta search).
  2. **Multi-AI Battle**: Up to 4 players (1 Human + 1–3 AIs, or AI-only matches with pause/step controls).
  3. **Play Online**: Quick Match, Create Private Room with 6-character room codes, and Join Room with live state sync.
  4. **Interactive How to Play**: Visual, step-by-step interactive rules guide.
  5. **Settings**: Audio toggles, haptic vibration feedback, board theme preferences.

---

## 2. User Experience & Visual Design

### Key User Flows
1. **Home Screen**: Hero branding ("Dot Battle"), quick-launch action cards for Play Online, Play vs AI, Multi-AI Battle, How to Play, and Settings.
2. **Match Setup Flow**: Configurable board size (4×4, 6×6, 8×8), player count (2 to 4), AI difficulty per bot slot, player color selection, and room creation/joining.
3. **Gameplay Canvas**:
   - Clean, touch-expanded hitboxes for horizontal and vertical edges (48dp touch targets preventing mis-taps).
   - Dynamic line drawing animations with active player color highlight.
   - Captured boxes fill smoothly with player tints and initials.
   - Real-time scoreboard with active turn pulsation and combo streak indicators.
   - In-game pause, quit confirmation dialog, and network connection status pill.
4. **Result Screen**: Winner announcement with celebration animation, detailed score breakdown, stats (moves made, boxes captured, max chain), Rematch button, and Return to Home.

### Visual Identity & Design Tokens (design.md)
- **Primary Accent**: `#E66239` (Warm Energetic Orange)
- **Background Base**: `#000000` (Pitch Black Canvas)
- **Surface Raised / Cards**: `#171717` / `#262626`
- **Text Primary**: `#FFFFFF` / `#F5F5F5`
- **Text Secondary / Muted**: `#A3A3A3` / `#737373`
- **Player Palette** (4 High-Contrast Distinct Hues):
  - Player 1: Neon Orange (`#E66239`)
  - Player 2: Electric Cyan (`#00D2FF`)
  - Player 3: Emerald Lime (`#10B981`)
  - Player 4: Radiant Violet (`#A855F7`)
- **Typography**: Poppins (Bold for headings, Medium for game stats, Regular for body).

---

## 3. Key Product Decisions & Trade-Offs

- **Deterministic Game Engine**: Pure Kotlin domain module (`DotBattleEngine`) decoupled from Compose UI and Android framework. Represents horizontal lines `(row, col)` and vertical lines `(row, col)`, detects 1 or 2 box closures atomically, assigns extra turns, and maintains immutable `GameState` snapshots. Enables full local JVM Robolectric unit testing without mocks.
- **3-Tier AI Strategy**:
  - *Easy*: Random valid moves, occasionally takes available boxes.
  - *Medium*: Greedy capturer; avoids giving away 3rd sides of boxes whenever safe moves exist.
  - *Hard*: Evaluates box chains, double-cross strategy, safe sacrifices, and bounded depth-first minimax with alpha-beta pruning to find optimal edge cuts within a strict 300ms time budget.
- **Robust Online Layer**:
  - Authoritative game state machine validating move sequence numbers, player turns, and legality.
  - Room management protocol with join validation, disconnect timers, and state resync.
  - Built-in test harness & simulated opponent fallback when Firebase keys are not configured in local environment, so the app remains 100% crash-free and testable.
- **Offline Match Recovery**: Ongoing local matches are serialized and saved via Room/DataStore upon pause or app backgrounding, allowing seamless resumption.

---

## 4. Technical Architecture & Data Strategy

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                       │
│  HomeScreen │ MatchScreen │ BoardCanvas │ SetupDialogs      │
│  HowToPlayScreen │ SettingsScreen │ ResultModal             │
└──────────────────────────────┬──────────────────────────────┘
                               │ Observes StateFlow
┌──────────────────────────────▼──────────────────────────────┐
│                    ViewModel Layer                          │
│  GameViewModel │ OnlineLobbyViewModel │ SettingsViewModel    │
└──────────────┬──────────────────────────────┬───────────────┘
               │ Dispatches Moves             │ Syncs Moves
┌──────────────▼──────────────┐ ┌─────────────▼───────────────┐
│     Domain Game Engine      │ │    Online / Network Layer   │
│  - DotBattleEngine          │ │  - RealtimeSyncManager      │
│  - BoardState & Evaluator   │ │  - RoomManager              │
│  - AIPlayer (Easy/Med/Hard) │ │  - FirebaseRTDB Repository  │
└──────────────┬──────────────┘ └─────────────┬───────────────┘
               │ Persists                     │ Reconnects
┌──────────────▼──────────────────────────────▼───────────────┐
│                  Data & Persistence Layer                   │
│  - Room Database (Local Matches, History)                   │
│  - DataStore Preferences (Audio, Haptics, Profile)          │
└─────────────────────────────────────────────────────────────┘
```

### Key Data Entities
- `Edge`: `Orientation (HORIZONTAL, VERTICAL)`, `row: Int`, `col: Int`.
- `Box`: `row: Int`, `col: Int`, `claimedBy: PlayerId?`.
- `Player`: `id: Int`, `name: String`, `color: Color`, `isAi: Boolean`, `difficulty: AiDifficulty?`.
- `GameState`: `boardSize: Int`, `edges: Map<Edge, PlayerId>`, `boxes: List<Box>`, `players: List<Player>`, `currentPlayerIndex: Int`, `scores: Map<PlayerId, Int>`, `status: MatchStatus`, `moveHistory: List<Move>`.

---

## Verification Plan

### Automated Tests
1. **Engine Unit Tests (`DotBattleEngineTest`)**:
   - Board initialization for 4×4, 6×6, 8×8.
   - Legal move acceptance and duplicate move rejection.
   - Single-box and double-box completion with extra turn grant.
   - Turn advancement when no box is completed.
   - Full-board completion, tie-breaking, and win declaration.
2. **AI Strategy Tests (`AiPlayerTest`)**:
   - Verification that AI never makes illegal or out-of-bounds moves.
   - Medium and Hard AI capture available boxes.
   - Hard AI chain avoidance and heuristic stability.
3. **Robolectric UI & ViewModel Tests (`GameViewModelTest`)**:
   - ViewModel state transition upon user move.
   - Match reset and rematch flows.
4. **Build & Compilation**:
   - `compile_applet` clean compilation check.
   - Generation and verification of installable debug APK.
