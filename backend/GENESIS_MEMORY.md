# Personal rule memory v1

This is authenticated, account- and sector-scoped SQL memory, not LLM training or
execution of the L1/L2/L3 pipeline. No external model, URL retrieval or executable
algorithm is connected. The user selects the meaning of the comment explicitly.

## Cycle

1. Select one of FINANCE, MEDICINE, MANUFACTURING, MILITARY, GOVERNMENT, LOGISTICS.
2. Select goal, constraint, preference or correction; submit up to 2000 characters.
3. POST `/api/v2/genesis/memory/preview` returns existing confirmed rules plus an
   unsaved proposal. Preview does not write the comment to SQL.
4. POST `/memory/confirm` stores the rule with current memory-policy consent.
   Identical kind/text retries in the same account and sector are idempotent.
5. GET `/memory/{sector}` or the next preview reconstructs context using confirmed
   rules. It does not claim to implement the instruction or produce domain advice.
6. DELETE `/memory/{sector}/{rule_id}` removes a rule from subsequent contexts.

Up to 50 rules per sector. PostgreSQL confirmation uses a per-account transaction
lock to serialize concurrent confirmation/consent checks. Storage consent is
separate from Calendar-copy consent and is checked against the current version.
Policy changes require new consent. Reading/deletion remain available afterwards.
User statements are instructions, not established facts or verified sources.
No pending drafts are permanently stored by this backend. Server administrators
can access infrastructure; this is not end-to-end encryption or a quantum protocol.

## Deployment (Ubuntu server, not Windows PowerShell)

Keep the existing production source and container configuration. Clone into a new
directory, publish `backend/static/memory-analysis-v1.html` using the existing
Caddy `/nft/` static route, build the backend image, and run the update helper.
It verifies the published policy, keeps Firebase/SQL mounts, environment, ports,
networks, restart settings and image command, and retains a stopped rollback
container with restart disabled. SQL tables are added on startup; old tables and
rows are not erased. If new readiness checks fail, the helper restores the API.

```bash
(
  set -eu
  task_release="/opt/genesis-memory-release-$(date +%s)"
  git clone --depth 1 https://github.com/kirillpistol/ai-voice-task-planner.git "$task_release"
  cd "$task_release"
  install -m 644 backend/static/memory-analysis-v1.html /opt/genesis-nft/memory-analysis-v1.html
  docker build -t pistol-genesis-api:memory backend
  python3 backend/deploy/update-memory-api.py \
    --image pistol-genesis-api:memory \
    --policy-url https://api.pistolgenesis.ru/nft/memory-analysis-v1.html
  curl -fsS --max-time 20 https://api.pistolgenesis.ru/api/v2/genesis/memory/config
)
```

This enables only personal memory. L1/L2 execution needs its own approved security
configuration and future implementation; `execution_allowed` is always false.
After deploying, update Android and select a sector or press its refresh icon.
