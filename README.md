
<img width="488" height="1002" alt="image" src="https://github.com/user-attachments/assets/e84e0e08-ecc1-4c04-ab54-7ca3ab23bbf0" />


# Solitaire

A solitaire collection for Android — Klondike, Spider, FreeCell, TriPeaks, and Pyramid — built with Jetpack Compose.

**Klondike variant:** Draw 3, unlimited redeals, substack moves, reversible foundations. Full rules in [GAMERULES.md](GAMERULES.md).

## Features

- Tap and drag-and-drop card movement
- Undo (10-deep history)
- Auto-complete when the game is winnable
- Dead-end detection with game-over prompt
- Win celebration overlay
- Animated stock ↔ waste flips and move-snap animations
- Session persistence across app launches

## Tech stack

- Kotlin + Jetpack Compose
- Coil 3 with SVG decoder for card rendering
- AndroidX DataStore for persistence
- Kotlinx Serialization for save state

## Requirements

- Android Studio Ladybug or newer
- Android SDK 36
- Min SDK 33 (Android 13)
- JDK 11

## Build

```bash
./gradlew :app:assembleDebug
```

Install to a connected device:

```bash
./gradlew :app:installDebug
```

## Tests

```bash
./gradlew :app:testDebugUnitTest
```

## Project layout

```
app/src/main/java/se/bernhauser/solitaire/
├── game/           Shared card model (Card, Deck, Piles)
│   ├── klondike/   Klondike state, rules, and ViewModel
│   ├── spider/     Spider state, rules, and ViewModel
│   ├── freecell/   FreeCell state, rules, and ViewModel
│   ├── tripeaks/   TriPeaks layout, state, rules, and ViewModel
│   └── pyramid/    Pyramid layout, state, rules, and ViewModel
├── persistence/    Save/load (per-game sessions, SessionCodec)
├── repository/     SolitaireRepository — bridges ViewModels and storage
├── configuration/  User configuration storage
└── ui/
    ├── board/      Shared board engine: columns, drag & drop, animations
    ├── cards/      Card rendering
    ├── klondike/   Klondike screen and board
    ├── spider/     Spider screen and board
    ├── freecell/   FreeCell screen and board
    ├── tripeaks/   TriPeaks screen and board
    ├── pyramid/    Pyramid screen and board
    ├── landing/    Game picker
    ├── settings/   Settings dialog
    ├── theme/      Material 3 theming
    └── win/        Win and game-over overlays
```

# Changing Card Visuals

Place your new card files in:

`app/src/main/assets/cards/`

The app currently loads SVGs from there using a strict naming convention — <rank><suit>.svg:

- Ranks: A 2 3 4 5 6 7 8 9 T J Q K
- Suits: C (Clubs), D (Diamonds), H (Hearts), S (Spades)
- Card backs: 1B.svg (Red), 2B.svg (Blue)

So the 52 + 2 backs would be e.g. AS.svg, TH.svg, KD.svg, 1B.svg, 2B.svg, …

## License

Source code: MIT — see [LICENSE](LICENSE).

Card SVG assets in `app/src/main/assets/cards/` are by Adrian Kennard (RevK), released under [CC0 / Public Domain](https://creativecommons.org/publicdomain/zero/1.0/). Source: https://www.me.uk/cards/
