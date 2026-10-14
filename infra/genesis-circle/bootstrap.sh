#!/usr/bin/env bash
# Install an isolated synthetic test circle; never change the existing application.
set -euo pipefail
umask 077
if [[ $(id -u) -ne 0 ]]; then
  echo "Run this script as root on the Ubuntu Docker host." >&2
  exit 1
fi
script_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
workspace="${1:-/opt/genesis-circle-$(date +%s)}"
if [[ "$workspace" != /* || -e "$workspace" ]]; then
  echo "Use a new absolute workspace path; existing directories are not overwritten." >&2
  exit 1
fi
docker info >/dev/null
docker compose version >/dev/null
command -v git >/dev/null
install -d -m 700 "$workspace/build/sources" "$workspace/state"
cp "$script_root/compose.yml" "$workspace/compose.yml"
cp "$script_root/Dockerfile" "$script_root/prepare.py" "$script_root/check.py" "$workspace/build/"
cp "$script_root/dockerignore" "$workspace/build/.dockerignore"
cp "$script_root/versions.json" "$workspace/versions.json"
while IFS=$'\t' read -r component repository commit; do
  target="$workspace/build/sources/$component"
  git init -q "$target"
  git -C "$target" remote add origin "https://github.com/$repository.git"
  git -C "$target" fetch -q --depth 1 origin "$commit"
  git -C "$target" checkout -q --detach FETCH_HEAD
  [[ $(git -C "$target" rev-parse HEAD) == "$commit" ]]
done < <(python3 - "$workspace/versions.json" <<'PY'
import json,sys
with open(sys.argv[1]) as file:
    for component, item in json.load(file).items():
        print(component, item['repository'], item['commit'], sep='\t')
PY
)
project="genesis-circle-$(date +%s)-$$"
image="pistol-genesis-circle:$project"
printf 'COMPOSE_PROJECT_NAME=%s\nGENESIS_CIRCLE_IMAGE=%s\n' "$project" "$image" > "$workspace/.env"
compose=(docker compose --env-file "$workspace/.env" --project-name "$project" --project-directory "$workspace" -f "$workspace/compose.yml")
"${compose[@]}" config --quiet
"${compose[@]}" build core-a
docker run --rm --network none --read-only --cap-drop ALL --cap-add CHOWN \
  --security-opt no-new-privileges --tmpfs /tmp:rw,nosuid,nodev,size=16m \
  --memory 128m --pids-limit 64 --user 0:0 \
  --mount "type=bind,source=$workspace/state,target=/state" \
  "$image" python /circle/prepare.py /state
started=0
cleanup_failure() {
  code=$?
  if [[ $code -ne 0 && $started -eq 1 ]]; then
    "${compose[@]}" down || true
    echo "Test circle stopped after failure; production API was not changed." >&2
  fi
  exit "$code"
}
trap cleanup_failure EXIT
started=1
"${compose[@]}" up -d core-a core-b source-a source-b
"${compose[@]}" --profile check run --rm --no-deps checker > "$workspace/report.json"
cat "$workspace/report.json"
"${compose[@]}" ps
echo "Circle workspace: $workspace"
echo "Certificates expire in two days. This is a synthetic test, not a production security deployment."
