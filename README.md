This is a Kotlin Multiplatform project targeting Android, iOS.

* `/composeApp` is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - `commonMain` is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    `iosMain` would be the right folder for such calls.

* `/iosApp` contains iOS applications. Even if you’re sharing your UI with Compose Multiplatform, 
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* `/shared` is for the code that will be shared between all targets in the project.
  The most important subfolder is `commonMain`. If preferred, you can add code to the platform-specific folders here too.


Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…
# ai-voice-task-planner
Kotlin-based AI Voice Task Planner with speech recognition (Whisper/ML Kit) and LLM integration — designed for banking &amp; analytics workflows.
<div align="center">
    <img src="screenshots/MainScreenDark.png" alt="Screenshot 1" width="20%">
    <img src="screenshots/RegisterScreenLight.png" alt="Screenshot 2" width="20%">
</div>
<div align="center">
    <img src="screenshots/MainScreenProfileSettingDark.png" alt="Screenshot 3" width="25%">
    <img src="screenshots/MainScreenWeeklyLight.png" alt="Screenshot 4" width="25%">
    <img src="screenshots/MainScreenWeeklyTaskEditLight.png" alt="Screenshot 5" width="25%">
    <img src="screenshots/MainScreenWeeklyRecordingFinishedDark.png" alt="Screenshot 6" width="25%">
</div>
