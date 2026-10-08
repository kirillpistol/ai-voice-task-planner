# GENESIS — приложение и проект экранов

[Каталог экранов](screenshots/README.md) · [Manifest](screenshots/manifest.json) · [История переименования](screenshots/original-file-mapping.md)

Макеты организованы по назначению, языку, теме и состоянию. [Общий каталог «Экраны PISTOL GENESIS»](screenshots/README.md): 27 русских и 28 английских макетов. [RU](screenshots/ru/README.md) · [EN / UK English](screenshots/en/README.md).

This is a Kotlin Multiplatform project targeting Android, iOS.
| RU | EN |
|:-:|:-:|
| <a href="screenshots/ru/auth/register-light.png"><img src="screenshots/ru/auth/register-light.png" width="150" alt="Регистрация RU"></a> | <a href="screenshots/en/auth/register-dark.png"><img src="screenshots/en/auth/register-dark.png" width="150" alt="Регистрация EN"></a> |
| Регистрация · RU | Регистрация · EN |
| <a href="screenshots/ru/calendar/weekly-dark.png"><img src="screenshots/ru/calendar/weekly-dark.png" width="150" alt="Недельный календарь RU"></a> | <a href="screenshots/en/calendar/weekly-light.png"><img src="screenshots/en/calendar/weekly-light.png" width="150" alt="Недельный календарь EN"></a> |
| Недельный календарь · RU | Недельный календарь · EN |
| <a href="screenshots/ru/tasks/weekly-task-actions-light.png"><img src="screenshots/ru/tasks/weekly-task-actions-light.png" width="150" alt="Действия над задачей RU"></a> | <a href="screenshots/en/tasks/weekly-task-actions-dark.png"><img src="screenshots/en/tasks/weekly-task-actions-dark.png" width="150" alt="Действия над задачей EN"></a> |
| Действия над задачей · RU | Действия над задачей · EN |
| <a href="screenshots/ru/profile/settings-menu-dark.png"><img src="screenshots/ru/profile/settings-menu-dark.png" width="150" alt="Настройки RU"></a> | <a href="screenshots/en/profile/settings-menu-light.png"><img src="screenshots/en/profile/settings-menu-light.png" width="150" alt="Настройки EN"></a> |
| Настройки · RU | Настройки · EN |
| <a href="screenshots/ru/tasks/task-edit-light.png"><img src="screenshots/ru/tasks/task-edit-light.png" width="150" alt="Редактирование задачи RU"></a> | <a href="screenshots/en/tasks/task-edit-dark.png"><img src="screenshots/en/tasks/task-edit-dark.png" width="150" alt="Редактирование задачи EN"></a> |
| Редактирование задачи · RU | Редактирование задачи · EN |
| <a href="screenshots/ru/tasks/task-created-light.png"><img src="screenshots/ru/tasks/task-created-light.png" width="150" alt="Задача добавлена RU"></a> | <a href="screenshots/en/tasks/task-created-dark.png"><img src="screenshots/en/tasks/task-created-dark.png" width="150" alt="Задача добавлена EN"></a> |
| Задача добавлена · RU | Задача добавлена · EN |

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
