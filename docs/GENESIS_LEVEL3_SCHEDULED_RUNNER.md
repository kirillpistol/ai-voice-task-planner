# GENESIS Level 3 — scheduled server worker (pilot)

The one-shot worker `python -m app.genesis_level3_runner --once` fetches only the predefined public demo site and writes its receipt to the same GENESIS SQL database as the main backend. It does not scan other sites, install APKs, execute Android apps, or analyze site content.

## Test on Windows before enabling a schedule

```powershell
cd "C:\Users\kiril\Desktop\ai-voice-task-planner"
git pull
$env:PYTHONPATH=(Resolve-Path ".\backend").Path
$env:GENESIS_DATABASE_URL = "sqlite:///" + ((Join-Path (Get-Location) "genesis_main.sqlite") -replace "\\","/")
$env:GENESIS_PUBLIC_REPORT_DIR = Join-Path (Get-Location) "genesis_public_reports"
$env:GENESIS_SECTOR_SOURCES_JSON = '{"FINANCE":{"source_name":"Public demo","source_url":"https://www.example.com/","resource_name":"Server HTTPS worker","resource_type":"web"}}'
& .\.venv\Scripts\python.exe -m app.genesis_level3_runner --once
& .\.venv\Scripts\python.exe -m pytest backend/tests/test_genesis_level3_runner.py backend/tests/test_genesis_public_sql_bridge.py -q --basetemp=".pytest_temp" -p no:cacheprovider
```

Expected: `verified_http_200`, persisted row in `genesis_source_observations`. The main API and worker must use identical `GENESIS_DATABASE_URL` (or `GENESIS_DATABASE_URL_FILE`), ideally shared PostgreSQL in deployment. Local SQLite is demonstration-only and is not automatically shared with a remote server.

## Scheduling on the server

Use the deployment's existing scheduler (Windows Task Scheduler, systemd timer or cron) to invoke the exact one-shot command, e.g. once daily. Provide `PYTHONPATH`, database secret and report directory via the server's protected environment/service configuration. Do not create a schedule in Android. Do not run the one-shot command concurrently; use a scheduler-level lock.

The worker currently runs only when manually invoked or when the server operator explicitly configures a schedule. It does not claim automatic recurring execution by itself.

The output returned by the authenticated `GET /api/v2/genesis/memory/FINANCE` supplies `source`, `connection_resource`, `source_status` and a short status line in `text`, which the existing Android cards render on sector refresh. Real content recommendations, SQL user-specific reports and VPN/VM/Docker deployment remain separate work.
