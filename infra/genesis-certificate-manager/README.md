# GENESIS Certificate Manager — implementation track

> SECURITY-CRITICAL. Not deployed. No automatic certificate rotation is enabled by this branch.

## Baseline verified on 2026-10-25
- Synthetic CORE A/B, SOURCE A/B tested successfully in an isolated recovery Circle.
- Recovery mTLS probe passed using `connect-core-api.py --probe-only`.
- Existing API remains attached to the old Circle, whose test CA and client/server certificates expired 2026-10-16.
- The synthetic `prepare.py` issues **2-day test certificates**; `bootstrap.sh` creates a fresh test Circle, it is **not** a production rotation mechanism.
- The CORE status API is **read-only**. `execution_allowed=false`; no user documents may be sent to this Circle.
- Production API must not be automatically redirected to a 2-day synthetic environment.

## Delivery milestones (in order)
1. **Inventory & trust boundaries:** document active issuer, CA, SANs, roles, leaf certificates, SHA-256 pins, volume mounts, rollout dependencies, current expiration and ownership; inspect API rollback helper. Do not expose keys or raw environment variables.
2. **Production certificate authority and policy:** select an appropriate internal PKI with tightly restricted signing operations and durable issuer lifecycle, role-specific client/server certificates, appropriate validity/revocation policy, narrowly scoped trust roots. Do not copy the test CA issuer design into production.
3. **Certificate lifecycle controller:** monitor expiry on a timer, stage future secrets in restricted versioned directories, validate chain, SAN, EKU, roles and pins, perform mTLS dry-run on a dedicated isolated candidate network, atomically activate only after approval and probe, maintain protected rollback metadata. Concurrent operations lock; partial failure must fail closed.
4. **Coordinated rollout:** account for each CORE and SOURCE process certificate reload/restart behavior; retain Firebase/SQL mounts, port bindings, restart policy, read-only root filesystems and internal-only networks; ensure no service is silently replaced by a synthetic worker.
5. **Automation:** systemd timer/service or equivalent with least-privilege execution, expiry warnings, alerts on failed renewal, auditable metadata without private keys or user content, tested manual break-glass and rollback.
6. **Acceptance tests:** happy-path renewal, expired/wrong-root certificates, pin mismatch, missing CA, denied roles, unreachable node, incomplete staging, concurrent runs, crash between switch and commit, rollback, service restart and 24h post-renewal health. No production enabling until safe.
7. **Handoff:** only after security release is validated, implement authenticated L3 policy → L1 routing → L2 isolated workers → approved AI gateway in separate changes.

## Implementation rules
- Preserve the existing Android/UI and all currently deployed containers until the rollout is audited.
- Never log, commit or paste private keys, passwords, tokens, full secret JSON or private document data.
- Do not disable TLS verification, relax certificate pinning or expose private network ports.
- `--probe-only` can create temporary artifacts; verify its side effects before automation.
- On the current 2-vCPU/1.9-GiB VPS, avoid parallel full image builds during production traffic.
- Test with synthetic data, no payment-card data, phone lists, or personal documents.
- Explicit exit criteria: two consecutive unattended test renewals; both nodes healthy through authenticated mTLS; rollback exercised; alerts verified; operational runbook reviewed.

## Current status
- [x] Dedicated GitHub development branch
- [x] Safety and acceptance checklist
- [ ] PKI design and review
- [ ] Controller implementation
- [ ] Automated tests
- [ ] Staging deployment
- [ ] Production deployment

Owner notes: Coordinate all changes with the current `infra/genesis-circle`, `backend/app/genesis_core_status.py`, and `backend/deploy/connect-core-api.py` implementation. Do not interpret a successful synthetic-circle check as production readiness.
