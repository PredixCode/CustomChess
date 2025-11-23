# CustomChess

## Overview

**CustomChess** is a small Playground for chess variants.

It shares one core chess engine and offers two ways to play:

- An **Android app** (Jetpack Compose UI).
- A **desktop app** (JavaFX UI).

You can start standard games, tweak how many moves each side gets, change the board size, enable special pieces, or let **Chess960** randomize the back ranks.

---

## Features

- **Standard Chess**
  - Start from the normal initial position.
  - Play like a regular chess app.

- **Presets**
  - Ready‑made setups such as:
    - Standard
    - Double move (both sides move twice per turn)
    - Bureaucrat variants (with a special extra piece)
  - Each preset fills in reasonable defaults that you can still edit.

- **Custom Start Positions**
  - Enter any valid **FEN** string to start from a specific position.

- **Board Size**
  - Change **width** and **height** of the board.

- **Chess960 (Fischer Random)**
  - Toggle Chess960 to randomize the starting back ranks.
  - Works with custom board sizes.

- **Move‑per‑Turn Variants**
  - Set how many moves White and Black get on each turn (e.g. 2 moves vs 1 move).

- **Bureaucrat Piece**
  - Optional experimental piece with its own rule.
  - Can be enabled or disabled per game.

- **Shared Core Engine**
  - Android and desktop use the same logic for boards, rules, and starting positions, so behaviour is consistent across platforms.

---

## Project Structure

At a high level, the project is split into three parts:

- **Core**
  - Shared chess engine and variant logic.
  - Handles board, pieces, rules, starting position and provides UI adapters.

- **Android App**
  - Written in Kotlin using Jetpack Compose.
  - Provides a mobile UI with:
    - Card‑based configuration screen (presets, rules, custom pieces).
    - Game screen with the board and pieces.

- **Desktop App**
  - Written in Java using JavaFX.
  - Provides a configuration window and game view similar to the Android version.

The Android and desktop layers mainly handle UI; almost all game rules live in the core.

---

## Prerequisites

You don’t have to use all targets; pick what you need.

### General

- **Gradle:** Local gradle installation to generate gradle-wrapper with setup_gradle.bat

### For the Core / Desktop App

- **JDK:** 21
- **JavaFX:** Either:
  - A JDK bundle with JavaFX, or
  - JavaFX added as dependencies via your build tool (Gradle/Maven).

### For the Android App

- **Android Studio** (latest stable).

---

## Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/PredixCode/CustomChess.git
cd CustomChess
```

### 1.1 Setup gradle wrapper

1. Install gradle (if not already installed)
2. Run setup_gradle.bat

### 2. Run the Android App

1. Open the project in **Android Studio**.
2. Select the Android module as your run configuration.
3. Choose an emulator or connected device.
4. Press **Run**.

### 3. Run the Desktop App

1. run start_desktop.bat

---

## Usage

The Android and desktop apps work in the same way.

### 1. Choose a Preset

- Use the **Preset** dropdown to pick a scenario:
  - e.g. *Standard*, *Double move x2*, *Bureaucrat*, etc.
- The fields below are pre‑filled based on the preset.

### 2. (Optional) Adjust the Start Position

- **FEN field:**
  - Leave it as is to use the preset’s default position.
  - Clear it to reset to the preset’s FEN.
  - Enter your own FEN to start from a custom position.

### 3. Configure Rules

- **Dimensions**
  - Set **Height** and **Width** for the board.
  - Values too small are automatically raised to a sensible minimum.
  - 0=auto, extracts width/height from FEN.

- **Moves per Turn**
  - Set how many moves White gets.
  - Set how many moves Black gets.

- **Chess960 Toggle**
  - Turn this on to randomize the starting back ranks (Fischer Random style).

- **Bureaucrat Toggle**
  - Turn this on to include the Bureaucrat piece and its rule.

### 4. Start or Resume a Game

- Click **Start new game**:
  - A new game is created using your current settings.
- If a game is already running:
  - Click **Resume** to return to it.
- Use **Exit** to close the app (Android: finishes the activity; desktop: closes the window/application).

---

## Ideas & Future Work
- More built‑in presets for popular variants.
- Additional experimental pieces and rule sets.
