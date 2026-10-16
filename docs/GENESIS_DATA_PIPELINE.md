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
