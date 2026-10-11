# REG.RU VPS: GENESIS private PostgreSQL

This deploy adds a **new** private PostgreSQL container without touching the
existing `genesis-api`, Firebase credentials, DNS or Caddy. Works on Ubuntu with
Docker Engine and Docker Compose v2. Designed for a 2 GB RAM VPS.

## 1. Inspect existing production (safe/read-only)

```bash
docker ps --format 'table {{.Names}}\t{{.Image}}\t{{.Ports}}'
docker compose version
docker inspect genesis-api --format 'Networks={{json .NetworkSettings.Networks}} Ports={{json .HostConfig.PortBindings}} Mounts={{json .Mounts}} Restart={{.HostConfig.RestartPolicy.Name}}'
cd /opt/genesis && git status --short
```

**Never paste** `docker inspect ... .Config.Env`, Firebase JSON, database URLs
or passwords into chat. The current repository may use a rewritten Git history,
so do not use `git pull` or `git reset --hard` until checking local changes.

## 2. Add PostgreSQL

Clone a clean staging checkout without modifying the existing `/opt/genesis` deployment:

```bash
cd /opt
git clone --depth 1 https://github.com/kirillpistol/ai-voice-task-planner.git genesis-sql-release
sudo bash /opt/genesis-sql-release/backend/deploy/bootstrap-postgres.sh
docker inspect genesis-postgres --format '{{json .NetworkSettings.Ports}}'
docker exec genesis-postgres pg_isready -U postgres -d genesis
```

This writes random passwords under `/opt/genesis-sql-secrets` (directory
0700), creates Docker volume `genesis-postgres-data`, and does not publish
port 5432. The API database role `genesis_app` is not a PostgreSQL superuser.

## 3. Connect your EXISTING API (only after identifying its mounts and port)

The API must retain existing Firebase credentials and Caddy port binding.
For the new API instance, add:

- Docker network `genesis-sql` alongside its existing network(s).
- Bind mount `/opt/genesis-sql-secrets/database_url` to
  `/run/secrets/genesis_database_url:ro`.
- Environment `GENESIS_DATABASE_URL_FILE=/run/secrets/genesis_database_url`.
- `GENESIS_PRIVACY_URL` pointing to a **real, published HTTPS page**.
- `GENESIS_POLICY_VERSION` matching that page.
- Backend image rebuilt from current `backend/` code.

**Do not discard old `google-services.json`, Firebase mount, API publishing or
Caddy TLS settings when recreating the container.** A running container's
environment cannot be changed with `docker network connect` alone.

## 4. Validate

```bash
curl -fsS https://api.pistolgenesis.ru/healthz
curl -fsS https://api.pistolgenesis.ru/readyz
curl -fsS https://api.pistolgenesis.ru/api/v2/genesis/config
docker exec genesis-postgres psql -U postgres -d genesis -c '\dt'
```

API `/config` returns `import_available: true` only if SQL is connected
and the published policy URL/version are set. Import also requires an
authenticated Firebase user and explicit personal-analysis consent.

### Quarterly cleanup

After API is confirmed, schedule
`docker exec genesis-api python -m app.quarterly_cleanup`
on 1 January, 1 April, 1 July and 1 October; the script removes previous-quarter
SQL task copies and retains recommendations and all calendar tasks. Make sure
it executes in an API container with the SQL secret mount and Docker network.
Use UTC if the VPS timezone differs from UTC.

### Backup/privacy

SQL DELETE removes live rows, but not instantly from historic backups. Decide on
an encrypted backup and expiry policy before storing real users' personal data.
Avoid backing up the copied tasks beyond their agreed retention period. All
privacy and user-agreement documentation must be on a real published website;
`backend/GENESIS_SQL.md` is technical operator documentation, not a consent page.
