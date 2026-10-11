# PISTOL GENESIS

Мобильное приложение для планирования задач и развития модульной платформы GENESIS.

## Приложение

- **Календарь** — месячный и недельный режимы, выбор даты, голосовой ввод задач.
- **Задачи** — просмотр, изменение статуса и редактирование задач.
- **Сервисы** — интерфейс для подключения отдельных продуктов, в том числе NFT и документов.
- **Genesis** — интерфейс будущих отчётов и рекомендаций на основе подключённых источников данных.
- Русский и английский интерфейсы, светлая и тёмная темы.

Авторизация реализуется через Firebase Authentication. Android-клиент работает с FastAPI API v2; разделы внешних сервисов и аналитики пока не подключены полностью.

## Структура проекта

| Каталог | Назначение |
|---|---|
| `composeApp/` | Android / Kotlin Multiplatform UI |
| `shared/` | Общие модели, алгоритмы и контракты |
| `iosApp/` | iOS-проект |
| `backend/` | FastAPI и серверные зависимости |
| `screenshots/` | Макеты экранов и каталог дизайна |
| `tools/` | Вспомогательные инструменты |
| `.github/workflows/` | Автоматическая сборка Android |
| `backend/deploy/` | Развёртывание приватного PostgreSQL на VPS |

[Каталог экранов](screenshots/README.md) · [Манифест экранов](screenshots/manifest.json) · [Развёртывание PostgreSQL](backend/deploy/QUICKSTART.md)

## Сборка Android

Требуется настроенный Android SDK и Java 17.

```powershell
git clone https://github.com/kirillpistol/ai-voice-task-planner.git
cd ai-voice-task-planner
.\gradlew.bat :composeApp:assembleDebug
```

APK: `composeApp/build/outputs/apk/debug/composeApp-debug.apk`.

Для работы авторизации необходима корректная конфигурация Firebase и разрешения Android. Функциональность сервисов и GENESIS будет расширяться отдельными модулями.
