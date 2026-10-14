# GENESIS: first three-level VPS test circle

This deployment is a synthetic integration test, not model training or a
production security certification. It does not connect the Android/API user
memory to the core or enable application execution. Existing API, SQL, Caddy,
NFT assets and personal data are not changed.

## Components

- Two L1 workers, A and B, in separate containers.
- Reviewed L2 `sector-a.numeric` installed inside each worker, with exact package
  identity and input contract. L2 is trusted in-process code, not a sandbox.
- Two independent read-only L3 part servers, containing 10/20 and 30/40.
- An operator checker with separate control/data test identities.

All three repositories are fetched at exact commits in `versions.json`.
Runtime containers use a private internal Docker network with no published
ports. Each worker/source mounts only its own keys/configuration. Separate
server identities have DNS SANs and certificate pins; core clients have distinct
control/data roles, and only the approved data identity can access the source
parts and strict binding. Containers run under UID 10001 with read-only files,
no Linux capabilities, no new privileges and memory/PID/CPU limits.

CA and leaf certificates are generated locally for this test; the CA private
key is destroyed after issuing them. Certificates expire after two days. No
test credentials belong in Git or chat. Rebuild a new circle for fresh identities.
The checker is an operator tool holding control and data identities for this
synthetic environment; it must not become the public application backend.

## Verified circle

1. Check mTLS health and availability of both sources.
2. Reject an unapproved certificate, incorrect server pin, forbidden role and
   legacy task bypass of strict bindings.
3. Operator selects the approved L2/L3 pair on worker A.
4. L3 reads and verifies the first part, then sends two records to L1/L2.
5. Transfer snapshot/binding/cursor from worker A to fresh worker B.
6. L3 resumes the second part on B; result is count 4, mean 25, variance 500/3.
7. Verify a completed stream produces no extra records and a replay is rejected.

The result returns along L2 -> L1 -> L3 to the checker. Worker A remains paused;
B retains the test state in RAM. A process loss loses that state. This checks
handoff, not durable recovery or transfer to a different physical VPS. Existing
L1/L2/L3 limitations still apply: no general exactly-once task queue, untrusted
algorithm sandbox, quantum protocol or autonomous training.

## Ubuntu server (root, Docker Compose v2)

From a checkout containing this directory:

```bash
bash infra/genesis-circle/bootstrap.sh
```

The script creates a new `/opt/genesis-circle-TIMESTAMP` workspace, builds a
dedicated image, creates identities with networking disabled, starts four
private services and runs the checker. If startup/check fails it stops only
this new test circle. It prints `status: passed`, report JSON and workspace.
No production container is replaced. The image has OpenSSL for test identity
issuance; runtime processing uses Python standard-library implementations.

## Inspect, repeat or stop

Enter the exact workspace printed by bootstrap, then:

```bash
docker compose --env-file .env -f compose.yml ps
docker compose --env-file .env -f compose.yml logs --tail 30 core-a core-b
```

To repeat the synthetic test, deliberately reset only its volatile workers:

```bash
docker compose --env-file .env -f compose.yml restart core-a core-b
docker compose --env-file .env -f compose.yml --profile check run --rm --no-deps checker
```

To stop this isolated circle:

```bash
docker compose --env-file .env -f compose.yml down
```

State and keys remain under the root-only workspace for operator inspection;
delete them only after confirming this synthetic environment is no longer needed.
Do not run bootstrap inside an existing workspace; it refuses overwrites.

## Tests

The dedicated GitHub workflow fetches pinned sources, runs real loopback mTLS
tests and existing strict-bound e2e checks, then builds and launches the actual
Docker circle and tears it down. Local native test:

```bash
PYTHONPATH=path/to/core:path/to/level2:path/to/level3 python -m unittest discover -s infra/genesis-circle/tests -v
```

Next application integration requires approved per-user execution boundaries,
a text/task algorithm in L2, and a scoped gateway. This test does not grant
arbitrary app clients control-plane certificates or expose the core publicly.
