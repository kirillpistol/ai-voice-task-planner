# GENESIS — versioned data and adaptive application workflows

## Separation of concerns
- **Level 1 / GENESIS Core:** identity, service registry, capability discovery, job routing; no app-specific selectors, banking rules, or database schemas.
- **Level 2 / sector service:** template catalog, source-data import, comparison logic, adapter versions, AI-assisted adaptation proposals and review. Change sector independently.
- **Level 3 / worker:** executes an approved action contract against a registered test application, emits structured observations. Security runtime is a separate integration layer, not assumed deployed.

## Implemented in this branch
`backend/app/genesis_dataset_store.py` provides a reference SQLite store (not production Postgres):
- revisioned templates with named columns;
- UTF-8 CSV import, deterministic source SHA-256, unique record IDs;
- observations tied back to each imported record;
- left-join comparison including pending/unprocessed records;
- UI selector fingerprints, revision history and adaptation proposals.

`backend/app/genesis_android_contract.py` defines bounded UI steps and validates
test scope. `backend/app/genesis_android_adb_worker.py` contains a local
ADB-based test executor. Neither module is wired to a remote secured worker.
Bank account access and real-person phone enumeration are not implemented.

## Adaptation lifecycle (design, not a claim of deployed ML)
1. Sector adapter observes an application-version/UI-tree change.
2. Normalize non-sensitive screen structure and compute a fingerprint.
3. Compare with stored selector revisions and recent controlled task outcomes.
4. An AI adapter *may* recommend an alternative selector/workflow version.
5. Validate recommendations against isolated permissioned regression tests.
6. Promote approved revision; retain rollback path and full audit history.

Adapting to entirely different task semantics requires a new declared capability
contract, not silently interpreting an unrelated UI as equivalent.

## Example using locally controlled dataset
```python
from app.genesis_dataset_store import DatasetStore
db = DatasetStore("genesis_test.sqlite")
template = db.register_template("LAB", "cases", ["record_id", "scenario"])
batch = db.import_csv(template["template_id"],
                      "record_id,scenario\\ndemo-001,positive\\ndemo-002,negative\\n".replace("\\n", "\n"))
db.record_observation(batch["dataset_id"], "demo-001", "owned.test.app", "1",
                      {"outcome": "positive"})
print(db.compare(batch["dataset_id"]))
```

The template can be exported as JSON from `export_template()`.
Data migration, remote template downloads, trained models, rollout gates,
PostgreSQL and worker attestation remain future integrations.


## Замкнутый пилотный цикл (Task Intake, отдельный локальный API)

`POST /v1/tasks/generate` или `/v1/tasks/import` → `POST /v1/tasks/{dataset_id}/process` → запись каждой проверки в SQLite `observations` → JSON в `GENESIS_REPORT_DIR` → `GET /v1/tasks/{dataset_id}/report`.

Проверка пока касается только валидности загрузки, а не третьих приложений. Реальный поиск, работа с APK, обучение и маршрутизация VPN/VM/Docker/VPN не включены. Не путать `validated` с подтверждением данных внешними источниками.

В PowerShell после запуска локального сервиса с `GENESIS_TASK_TOKEN` и `GENESIS_TASK_DB`:

```powershell
$h = @{"X-Genesis-Token"="local-genesis-test-2026"}
$root = "http://127.0.0.1:8081"
$created = Invoke-RestMethod "$root/v1/tasks/generate" -Method Post -Headers $h -ContentType "application/json" -Body '{"count":3}'
$id = $created.dataset_id
$processed = Invoke-RestMethod "$root/v1/tasks/$id/process" -Method Post -Headers $h -ContentType "application/json" -Body '{"confirm":true}'
$report = Invoke-RestMethod "$root/v1/tasks/$id/report" -Headers $h
$processed | Format-List
$report.records | Format-Table record_id,status
```

Server only; no added Android screen. Authentication is for local testing and must be replaced with deployed user-specific authorization before exposing the endpoint over a network.
