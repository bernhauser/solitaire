# Solitaire

A Klondike Solitaire game for Android, built with Jetpack Compose.

**Variant:** Draw 3, unlimited redeals, substack moves, reversible foundations. Full rules in [GAMERULES.md](GAMERULES.md).

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
├── game/           Pure game model (Card, Deck, Rules, GameState, Move, GameViewModel)
├── persistence/    Save/load (SavedSession, SavedSessionCodec)
├── repository/     SolitaireRepository — bridges ViewModel and storage
├── configuration/  User configuration storage
└── ui/
    ├── board/      Board, columns, drag overlay, animations
    ├── settings/   Settings dialog
    ├── theme/      Material 3 theming
    └── win/        Win and game-over overlays
```

## License

Source code: MIT — see [LICENSE](LICENSE).

Card SVG assets in `app/src/main/assets/cards/` are by Adrian Kennard (RevK), released under [CC0 / Public Domain](https://creativecommons.org/publicdomain/zero/1.0/). Source: https://www.me.uk/cards/
