This is a Kotlin Multiplatform project targeting Android, iOS.
| | |
|:-:|:-:|
| ![Main screen dark](screenshots/MainScreenWeeklyDark.png) | ![Registration light](screenshots/RegisterScreenLight.png) |
| *Main screen (dark)* | *Registration screen (light)* |
| ![Profile settings dark](screenshots/MainScreenProfileSettingsDark.png) | ![Weekly view light](screenshots/MainScreenWeeklyLight.png) |
| *Profile settings (dark)* | *Weekly view (light)* |
| ![Edit task light](screenshots/MainScreenWeeklyTaskEditLight.png) | ![Recording finished dark](screenshots/MainScreenWeeklyRecordingFinishedDark.png) |
| *Edit task (light)* | *Recording finished (dark)* |
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
