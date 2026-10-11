# PISTOL GENESIS — SQL workspace

## Purpose and guarantees

The Calendar is the only place where source tasks are created or edited. Calendar records remain in Firestore. GENESIS stores separate user-scoped read-only SQL copies to support future **personal** data modelling. These copies can be wiped and re-imported without changing Calendar. Recommendations are stored separately and are retained on quarterly resets and on the user's **Clear GENESIS database** action.

## Deployment (not yet performed)

1. Provision a private PostgreSQL server/database and least-privilege service credentials. Do not expose its TCP port to the public internet.
2. Add VPS/container secrets (never commit credentials):
   - `GENESIS_DATABASE_URL=postgresql+psycopg://USER:PASSWORD@HOST:5432/DB`
   - `GENESIS_PRIVACY_URL=https://YOUR-ACTUALLY-PUBLISHED-PRIVACY-DOCUMENT`
   - `GENESIS_POLICY_VERSION=2026-10` (set to the version of the published document)
3. Rebuild/redeploy the `backend/` image. The first start bootstraps SQL tables; production schema changes should use formal migrations.
4. If SQL is absent, the Calendar continues to work but GENESIS endpoints return 503 and mobile import remains unavailable.
5. Run `python -m app.quarterly_cleanup` in the configured API container at 00:05 UTC on 1 January, 1 April, 1 July and 1 October. The command is idempotent and deletes copies imported **before** the current quarter.
6. Establish backup retention and an actual deletion process. SQL DELETE does not erase prior backups immediately. Restrict SQL access per authenticated UID and configure DB roles, encryption and ideally PostgreSQL RLS before production launch.

## Routes

- `GET /api/v2/genesis/config` — policy URL/version and whether import can be enabled
- `GET /api/v2/genesis/tasks` — copied tasks for authenticated user
- `POST /api/v2/genesis/tasks/import` — requires explicit personal-analysis consent and current published policy version; copies genuine tasks from the authenticated UID's Firestore calendar. No client text or arbitrary task IDs are accepted.
- `DELETE /api/v2/genesis/tasks` — erases user's SQL task copies, not Calendar or recommendations
- `DELETE /api/v2/genesis/tasks/{source_task_id}` — erases one copied task
- `GET /api/v2/genesis/recommendations` — stored results for this user, not synthetic output

User ID is exclusively derived from a verified Firebase Bearer token. No client-supplied user ID is trusted.

## Consent and AI modelling

This consent authorises **personal analysis only**; it does **not** authorise training a general-purpose AI model. A future separate opt-in is necessary before training shared models. The AI generation engine is NOT connected in this release; the recommendations table is ready but not populated automatically.

A quarterly SQL purge retains recommendations, calendar records and existing consent records. To remove an entire account or recommendations requires a different, explicit account-erasure workflow.

## Notes

- Tasks from Firestore must be re-imported explicitly after wiping the model; no silent background restore.
- The Android Tasks tab is a read-only view of imported SQL task copies; no task editing/creation shortcut.
- Import snapshots are deduplicated by (user_id, source_task_id), keeping one copy of each source task per user.
- Use real website documents; never substitute an unverified website URL in production.
