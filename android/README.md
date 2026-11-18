# Numbercraft for Android

Numbercraft is a child-friendly arithmetic puzzle game for Android. It is designed for roughly ages 7-12 and uses large touch controls, encouraging feedback, and progressively harder target puzzles.

## Open in Android Studio

Open this `android` folder as a project in Android Studio. Install JDK 17, Gradle 9.6.0, and Android SDK Platform 37, then sync and run the `app` configuration on a device or emulator. To create local Gradle wrapper scripts, run `gradle wrapper --gradle-version 9.6.0` from this folder.

## How to play

Use each of the four number tiles once and place three operations between them to reach the target. Multiplication and division happen before addition and subtraction. Hints explain that rule; they do not reveal the answer. Each generated level has a valid solution.

The first puzzle is `2`, `3`, `5`, and `7`, with a target of `24`. Later puzzles use larger numbers. Progress, score, stars, and the current equation are saved on the device.

## Privacy

The app works offline. It requests no network permission and stores progress only in Android app preferences on the device. It has no ads, account sign-in, or analytics SDK.

## Build setup

- Minimum Android version: Android 7.0 (API 24)
- Compile SDK: 37
- Target SDK: 36
- Java runtime: 17
- Android Gradle Plugin: 9.5.1
- Jetpack Compose BOM: 2026.09.00

