# GENESIS — приложение и проект экранов

[Каталог исходных экранов](screenshots/README.md) · [Manifest](screenshots/manifest.json) · [История переименования](screenshots/original-file-mapping.md)

Исходные макеты организованы по назначению, теме и состоянию. Переработанные русская и английская версии будут добавлены отдельно после получения.

This is a Kotlin Multiplatform project targeting Android, iOS.
| | |
|:-:|:-:|
| ![Registration light](screenshots/original/auth/register-light.png) | ![Main screen dark](screenshots/original/calendar/weekly-dark.png) |
| *Registration screen (light)* | *Main screen (dark)* |
| ![Weekly view light](screenshots/original/tasks/weekly-task-actions-light.png) | ![Profile settings dark](screenshots/original/profile/settings-menu-dark.png)|
| *Weekly task actions (light)* | *Profile settings (dark)* |
| ![Edit task light](screenshots/original/tasks/task-edit-light.png) | ![Recording finished dark](screenshots/original/tasks/task-created-dark.png) |
| *Edit task (light)* | *Task successfully added (dark)* |
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
