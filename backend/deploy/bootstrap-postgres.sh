#!/usr/bin/env bash
# Idempotent setup for a private SQL instance on the GENESIS VPS.
# Does NOT touch the current API container or Firestore.
set -euo pipefail
umask 077

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="${1:-$SCRIPT_DIR/postgres.compose.yml}"
export GENESIS_SECRET_DIR="${GENESIS_SECRET_DIR:-/opt/genesis-sql-secrets}"

if [[ "$(id -u)" != "0" ]]; then
  echo "Run as root: sudo bash ${BASH_SOURCE[0]}" >&2
  exit 1
fi
command -v docker >/dev/null || { echo "Docker not installed" >&2; exit 1; }
command -v python3 >/dev/null || { echo "Python 3 not installed" >&2; exit 1; }
docker compose version >/dev/null || { echo "Docker Compose v2 not installed" >&2; exit 1; }
[[ -f "$COMPOSE_FILE" ]] || { echo "Missing $COMPOSE_FILE" >&2; exit 1; }

# Do not create replacement credentials for an already initialized data volume.
if docker volume inspect genesis-postgres-data >/dev/null 2>&1; then
  for secret in admin_password app_password; do
    if [[ ! -s "$GENESIS_SECRET_DIR/$secret" ]]; then
      echo "Existing SQL volume but $secret is missing. Restore its original secret before retrying." >&2
      exit 1
    fi
  done
fi

install -d -m 0700 -o root -g root "$GENESIS_SECRET_DIR"
for secret in admin_password app_password; do
  if [[ ! -s "$GENESIS_SECRET_DIR/$secret" ]]; then
    python3 -c 'import secrets; print(secrets.token_hex(32))' > "$GENESIS_SECRET_DIR/$secret"
  fi
  chmod 0600 "$GENESIS_SECRET_DIR/$secret"
done
# postgres:16-alpine drops root privileges before reading POSTGRES_PASSWORD_FILE.
# 0644 is needed inside the container; host directory remains root-only (0700).
chmod 0644 "$GENESIS_SECRET_DIR/admin_password"

if docker ps -a --format '{{.Names}}' | grep -qx genesis-postgres; then
  if [[ "$(docker inspect genesis-postgres --format '{{ index .Config.Labels "com.docker.compose.project" }}')" != "pistol-genesis-sql" ]]; then
    echo "Container genesis-postgres exists but is not managed by this compose project. Stopping." >&2
    exit 1
  fi
fi

docker compose -f "$COMPOSE_FILE" up -d --wait postgres

# Passwords are hex-only; no shell/SQL escaping ambiguity.
app_pass="$(cat "$GENESIS_SECRET_DIR/app_password")"
[[ "$app_pass" =~ ^[0-9a-f]{64}$ ]] || { echo "Unexpected app password format" >&2; exit 1; }

# The database and unprivileged application role are created only if missing.
if [[ "$(docker exec genesis-postgres psql -U postgres -d postgres -Atqc "SELECT 1 FROM pg_roles WHERE rolname='genesis_app'")" != "1" ]]; then
  docker exec genesis-postgres psql -U postgres -d postgres -v ON_ERROR_STOP=1 \
    -c "CREATE ROLE genesis_app LOGIN PASSWORD '$app_pass'"
fi
if [[ "$(docker exec genesis-postgres psql -U postgres -d postgres -Atqc "SELECT 1 FROM pg_database WHERE datname='genesis'")" != "1" ]]; then
  docker exec genesis-postgres psql -U postgres -d postgres -v ON_ERROR_STOP=1 \
    -c 'CREATE DATABASE genesis OWNER genesis_app'
fi

# Verify the application role and its private password match before connecting API.
docker exec -e PGPASSWORD="$app_pass" genesis-postgres \
  psql -h 127.0.0.1 -U genesis_app -d genesis -Atqc "SELECT 1" >/dev/null ||
  { echo "GENESIS SQL application authentication failed; do not redeploy API." >&2; exit 1; }

# Never print this URL or commit it. The web API reads it as a mounted file.
printf 'postgresql+psycopg://genesis_app:%s@genesis-postgres:5432/genesis\n' "$app_pass" \
  > "$GENESIS_SECRET_DIR/database_url"
# API is a non-root container user. 0644 applies only to a file in a 0700 directory.
chmod 0644 "$GENESIS_SECRET_DIR/database_url"

docker exec genesis-postgres psql -U postgres -d genesis -Atqc "SELECT current_database()"
echo "GENESIS SQL prepared. No database ports exposed; current API untouched."
echo "Secret mount for API: $GENESIS_SECRET_DIR/database_url -> /run/secrets/genesis_database_url:ro"
echo "API environment: GENESIS_DATABASE_URL_FILE=/run/secrets/genesis_database_url"
echo "Connect API to Docker network genesis-sql only after inspecting its runtime configuration."
