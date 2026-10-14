# Core status gateway

Authenticated `GET /api/v2/genesis/core/status` reads the real private L1 nodes.
It reports connected/partial/unavailable/not_configured/busy, node A/B mode and
readiness, and checked_at. There is no arbitrary URL, task/control forwarding,
automatic failover, algorithm registration or execution endpoint. Calendar and
memory endpoints retain their configuration and behavior.

TLS checks CA, DNS name and configured SHA256 DER server pin before HTTP. The
gateway uses only the test circle's data identity; no control or CA private key
is mounted. **That certificate still has the circle's data role**, which is not
a dedicated observer role; status-only enforcement is in gateway code. Separate
identities/roles for future execution are part of the subsequent security design.
No claim of production security certification or quantum cryptography is made.

The status is system-level metadata, not a user's algorithm result. The existing
circle is synthetic. Phone input, memory rules and user tasks are not sent to it.
API clients never receive core certificates, keys, addresses, snapshots or raw
algorithm results. Two concurrent gateway checks per process are allowed; nodes
are probed in parallel with bounded response bodies and socket timeouts.

## Deploy on the existing Ubuntu Docker host

Build the current backend, then use the actual circle workspace:

```bash
docker build -t pistol-genesis-api:core-status backend
python3 backend/deploy/connect-core-api.py \
  --image pistol-genesis-api:core-status \
  --circle /opt/genesis-circle-1791996760
```

The helper verifies the selected network is internal and belongs to that circle.
It copies only CA certificate and data certificate/key into a new root-provisioned
directory, readable by the API's actual UID. It first runs a real mTLS probe from
a temporary restricted container on the private network. Only after that succeeds
does it recreate the API, preserving Firebase/SQL mounts, environments, ports,
command, user, restart policy and non-circle networks. Previous circle networks
are replaced to avoid ambiguous core DNS. A stopped rollback container is kept.
If API or core checks fail, the previous API is restored. No core is restarted.

Certificate expiry makes the gateway unavailable; validation is never disabled.
For a new circle, repeat the helper with its new workspace. Test certificates are
short-lived (two days), not the final key lifecycle design.

Update Android after deploying. GENESIS polls once when opened and offers manual
refresh. It displays A paused after handoff and B ready for the current numeric
test; it always labels service execution as closed. No changes to L2 services or
the safety approval workflow are performed at this stage.
