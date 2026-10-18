# GENESIS Level 3 — первый реальный публичный HTTPS-сигнал

В этом этапе добавлен только узкий проверочный worker: `app.genesis_public_check.check_public_demo("FINANCE")`. Он выполняет HTTPS GET к фиксированному общедоступному тестовому `https://www.example.com/`, фиксирует HTTP 200, длину ответа, SHA-256 и время. Не извлекает личные данные и не делает выводов по содержимому.

Затем `genesis_source_registry.sector_source_status` читает сохранённую квитанцию в `GENESIS_PUBLIC_REPORT_DIR` и отдаёт метаданные в существующие карточки GENESIS через `/api/v2/genesis/memory/FINANCE`. Для этого `GENESIS_SECTOR_SOURCES_JSON` должен содержать **точно такой же URL**. Сервер авторизации и основной Android-клиент остаются прежними.

## Локальная проверка в PowerShell

```powershell
cd "C:\Users\kiril\Desktop\ai-voice-task-planner"
git pull
$env:PYTHONPATH = (Resolve-Path ".\backend").Path
$env:GENESIS_PUBLIC_REPORT_DIR = (Join-Path (Get-Location) "genesis_public_reports")
$env:GENESIS_SECTOR_SOURCES_JSON = '{"FINANCE":{"source_name":"Example public site","source_url":"https://www.example.com/","resource_name":"Server HTTPS checker","resource_type":"web"}}'
& .\.venv\Scripts\python.exe -c "from app.genesis_public_check import check_public_demo; print(check_public_demo('FINANCE'))"
& .\.venv\Scripts\python.exe -c "from app.genesis_source_registry import sector_source_status; print(sector_source_status('FINANCE'))"
& .\.venv\Scripts\python.exe -m pytest backend/tests/test_genesis_source_registry.py backend/tests/test_genesis_public_receipt.py -q
```

Важно: чтобы карточки на Android показали эти значения, конфигурация `GENESIS_SECTOR_SOURCES_JSON` и путь к отчётам должны быть установлены **в окружении основного backend-сервера**, а не только в консоли тестирования. Хранилище отчётов должно быть доступно backend-процессу. Текущее обновление не содержит таймера, очереди, автоматического запуска рабочего процесса, HTML-парсера или анализа содержимого сайтов.

**Ограничения:** этот пилот использует только фиксированный домен `www.example.com`. Не является общим поисковиком, не загружает APK, не выполняет действия в банковских приложениях и не включает VPN/VM/Docker-контур. Для production нужны сетевые egress-ограничения, планировщик, контроль обновлений и авторизованное хранилище результатов по пользователям.
