# GENESIS Level 3: server SQL loop

The verified proof-of-concept check is now linked to the existing GENESIS SQL workspace.

## Run a single server check

Execute on the backend host (not in Android):

```powershell
cd "C:\Users\kiril\Desktop\ai-voice-task-planner"
git pull
$env:PYTHONPATH = (Resolve-Path ".\backend").Path
$env:GENESIS_DATABASE_URL = "sqlite:///" + ((Join-Path (Get-Location) "genesis_main.sqlite") -replace "\\","/")
$env:GENESIS_PUBLIC_REPORT_DIR = (Join-Path (Get-Location) "genesis_public_reports")
$env:GENESIS_SECTOR_SOURCES_JSON = '{"FINANCE":{"source_name":"Example public site","source_url":"https://www.example.com/","resource_name":"Server HTTPS worker","resource_type":"web"}}'
& .\.venv\Scripts\python.exe -c "from app.genesis_store import make_session_factory; from app.genesis_public_check import check_and_store_demo; from app.genesis_store import configured_database_url; print(check_and_store_demo(make_session_factory(configured_database_url())))"
```

The SQL database URL used by the worker **must match** that of the main backend process; a separate SQLite database will not show results in Android. With deployed PostgreSQL use the same private DB credentials/configuration used by the main backend, following existing secret-file rules.

The mobile app already reads `/api/v2/genesis/memory/FINANCE` to display `source` and `connection_resource`. Restart the main backend after updates. Set the same `GENESIS_SECTOR_SOURCES_JSON` in the backend environment and refresh the FINANCE sector to see the SQL observation.

The server worker records HTTP-200 status, timestamp, bytes and SHA256. It does not parse content, execute apps or claim that VPN/VM/Docker is deployed. Runs are explicitly triggered, not scheduled. No new Android interface was created.

## Tests

```powershell
$env:PYTHONPATH = (Resolve-Path ".\backend").Path
& .\.venv\Scripts\python.exe -m pytest backend/tests/test_genesis_public_sql_bridge.py backend/tests/test_genesis_public_receipt.py -q --basetemp=".pytest_temp" -p no:cacheprovider
```
