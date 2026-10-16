# GENESIS — источник проверки и ресурс в существующем интерфейсе

Экран Genesis уже содержит read-only поля «Источник проверки», «Ресурс» и «Безопасность». Не нужно добавлять на телефон кнопки выбора браузера или APK.

Сервер `GET /api/v2/genesis/memory/{sector}` теперь заполняет `source` и `connection_resource` из конфигурации `GENESIS_SECTOR_SOURCES_JSON`. Это метаданные выбранного сектора, а НЕ доказательство внешнего поиска или исполнения Level 3.

Пример локальной конфигурации в PowerShell (демонстрационный домен; реальную ссылку подставляет оператор):
```powershell
$env:GENESIS_SECTOR_SOURCES_JSON = '{"FINANCE":{"source_name":"Открытый справочник","source_url":"https://example.org/public","resource_name":"Публичный веб-адаптер","resource_type":"web"}}'
```

Допустимые типы ресурса: `web`, `api`, `android_app`, `service`. Адрес источника должен быть HTTPS. Конфигурация относится к backend-процессу: при размещении на сервере переменную задают в серверном окружении и перезапускают backend, после чего в существующем приложении обновляют сектор. Вводить переменные на телефоне не требуется.

Поля `source_status` с флагами `source_configured`, `resource_configured`, `verification_completed`, `worker_executed` позволяют отличить настройку от фактического результата. Последние два поля сейчас всегда `false`.

Файлы отчётов, реальный парсер, проверка сайта, автоматический анализ и изолированный исполнитель Level 3 к этому изменению не относятся и пока не подключены. Отображение `VPN - VM - DOC - VPN` в Android остаётся отдельно от профиля безопасности и не означает, что туннели или VM запущены.

Проверка:
```powershell
$env:PYTHONPATH=(Resolve-Path ".\backend").Path
& .\.venv\Scripts\python.exe -m pytest backend/tests/test_genesis_source_registry.py -v
```
