#!/usr/bin/env bash
# Safe, read-only prerequisites for GENESIS execution VM. Do not mutate running infrastructure.
set -euo pipefail
fail=0
say() { printf '%-27s %s\n' "$1" "$2"; }
ram_kib=$(awk '/MemTotal:/ {print $2}' /proc/meminfo)
cores=$(nproc)
avail_kib=$(awk '/MemAvailable:/ {print $2}' /proc/meminfo)
free_gib=$(df -BG --output=avail / | tail -1 | tr -dc '0-9')
say "CPU" "${cores} vCPU"
say "Memory" "$((ram_kib / 1048576)) GiB total; $((avail_kib / 1024)) MiB available"
say "Disk free" "${free_gib} GiB"
for cmd in qemu-system-x86_64 qemu-img virsh virt-install cloud-localds; do
  if command -v "$cmd" >/dev/null; then say "$cmd" ready; else say "$cmd" MISSING; fail=1; fi
done
if [[ -r /dev/kvm && -w /dev/kvm ]]; then say KVM accessible; else say KVM UNAVAILABLE; fail=1; fi
if (( ram_kib < 12 * 1048576 )); then say "Resource policy" "BLOCKED: >=12 GiB host RAM required"; fail=1; fi
if (( cores < 6 )); then say "Resource policy" "BLOCKED: >=6 vCPU host required"; fail=1; fi
if (( free_gib < 50 )); then say "Disk policy" "BLOCKED: >=50 GiB free required"; fail=1; fi
if command -v docker >/dev/null; then
  for svc in genesis-api genesis-postgres genesis-med-standalone; do
    if docker inspect --format '{{.State.Running}}' "$svc" 2>/dev/null | grep -qx true; then
      say "$svc" running
    else
      say "$svc" "not running (review before provisioning)"
    fi
  done
fi
if (( fail )); then
  echo "GENESIS VM host preflight: BLOCKED. Do not install/start a VM here yet."
  exit 2
fi
echo "GENESIS VM host preflight: prerequisites met. This is not a VM boot validation."
