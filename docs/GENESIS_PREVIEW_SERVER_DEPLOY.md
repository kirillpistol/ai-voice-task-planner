# GENESIS: publish the server preview that the Android app calls

The Android client uses `https://api.pistolgenesis.ru/api/v2/genesis`. Updating the Android APK does **not** update the server. The user flow "Import -> comment -> Prepare rule" depends on both API and SQL.

## A. Verify on Windows (no production secrets)

```powershell
cd "C:\Users\kiril\Desktop\ai-voice-task-planner"
git switch feature/execution-routing-isolation-20261015
git pull --ff-only
$env:PYTHONPATH=(Resolve-Path ".\backend").Path
.\.venv\Scripts\python.exe -m pytest backend/tests/test_genesis_preview_suggestions.py backend/tests/test_genesis_memory.py backend/tests/test_genesis_public_api_bridge.py -q --basetemp=".pytest_temp" -p no:cacheprovider
```

If tests fail, **do not deploy**. The GitHub workflow `GENESIS backend tests` also runs on this branch.

## B. Inspect REG.RU VPS (read-only)

SSH to the Ubuntu host from your own terminal. Do not send private keys or passwords in chat. Run:

```bash
docker ps --format 'table {{.Names}}\t{{.Image}}\t{{.Ports}}'
docker inspect genesis-api --format 'Networks={{json .NetworkSettings.Networks}} Ports={{json .HostConfig.PortBindings}} Mounts={{json .Mounts}}'
cd /opt/genesis && git status --short && git branch --show-current
curl -fsS https://api.pistolgenesis.ru/healthz
curl -fsS https://api.pistolgenesis.ru/api/v2/genesis/config
curl -fsS https://api.pistolgenesis.ru/api/v2/genesis/memory/config
```

**Do not** paste `docker inspect .Config.Env`, SQL URLs, bearer tokens, Firebase configuration, or environment files. Do not run `git reset --hard` or replace the running container until mounts, secret files, network(s), port mapping, restart policy, actual source checkout and deployment method have been reviewed. In particular the API needs Firestore credentials and its SQL network/secret intact. See `backend/deploy/QUICKSTART.md`.

## C. Release (requires host access and validated preflight)

The existing `backend/deploy/update-memory-api.py` provides a rollback-capable replacement, but it requires a publicly published memory policy URL and version. The host operator must first build a backend image from the tested branch, verify the new image's `/api/v2/genesis/memory/preview` route and its SQL connectivity without touching the running API, and only then invoke the deployment script with real policy information. Never use a placeholder policy.

After deploying, confirm:
- `/healthz` and `/readyz` are healthy;
- SQL, policy, Firebase verification and task import still work;
- a **valid, local Firebase ID token** gets a 200 from the authenticated preview endpoint;
- `text` contains `Подсказка GENESIS:` for an account with imported tasks; and
- an account with no imported tasks receives the honest empty-import hint.

No token or personal task data should be pasted into chat. If the server still returns its old preview shape, it is not running this branch; do not rebuild Android to fix a stale server.

The current hint algorithm is a rules-based schedule/status summary, **not** a deployed generative AI model. Saving a rule remains governed by explicit storage consent.
