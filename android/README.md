# Numbercraft for Android

Numbercraft is a child-friendly math puzzle app for Android, designed for roughly ages 7-12. It includes ten different puzzle games, large touch controls, encouraging feedback, and endless levels for each game.

## Open in Android Studio

Open this `android` folder as a project in Android Studio. Install JDK 17, Gradle 9.6.0, and Android SDK Platform 37, then sync and run the `app` configuration on a device or emulator. To create local Gradle wrapper scripts, run `gradle wrapper --gradle-version 9.6.0` from this folder.

## How to play

Choose among Target Builder, Number Sequence, Times Tables, Number Bonds, Fraction Finder, Shape Sequence, Odd One Out, Place Value, Balance Beam, and Number Riddle. Each game gets its own progress and infinite stream of puzzles.

Target Builder starts with ten curated puzzles, then generates new ones endlessly. The other games also generate new questions as levels increase. Progress, score, stars, and the current equation are saved on the device.

## Privacy

The app works offline. It requests no network permission and stores progress only in Android app preferences on the device. It has no ads, account sign-in, or analytics SDK.

## Build setup

- Minimum Android version: Android 7.0 (API 24)
- Compile SDK: 37
- Target SDK: 36
- Java runtime: 17
- Android Gradle Plugin: 9.5.1
- Jetpack Compose BOM: 2026.09.00

