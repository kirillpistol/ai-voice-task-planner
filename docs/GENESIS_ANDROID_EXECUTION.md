# GENESIS Android execution — test contract v1

## Scope and boundary

GENESIS Core stays independent of any banking or other sector's UI logic.
A Level-2 sector service owns its workflows, authorization and capability
registrations. A Level-3 worker, when deployed, runs approved UI actions
against an approved test package. No device worker is currently deployed.

**This commit implements a validated task contract, not a working ADB worker
or a claim that VPN/VM/Docker isolation exists.** It keeps the existing
execution-routing API and eight tests intact.

## Data flow

1. An authenticated principal asks a sector service for an approved test.
2. Sector service verifies business authorization, the exact application
   package and permitted test data.
3. Sector service builds an `AndroidJob` with at most 40 explicit steps and
   passes contract validation.
4. Future worker control plane verifies signed, short-lived job grants,
   attested worker identity, package fingerprint and isolation profile.
5. Device agent executes a single allowlisted job, captures status and
   synthetic test assertions, and returns bounded audit events.
6. Worker clears temporary task state and releases exclusive device lock.

The current contract deliberately returns `dispatch_status:not_configured`.
No actual device execution is connected yet.

## Required worker controls before production use

- mTLS plus short-lived signed task grants with tenant/scope bindings.
- Device allocation and exclusive locking, health checks and timeouts.
- Exact package **and signing-certificate** allowlist; default deny.
- Single-job concurrency initially; explicit step and duration ceilings.
- Redacted, structured audit log with job ID, timestamps, step outcomes.
- No PIN, password, OTP, account data, customer identity, screen capture
  of financial data or real phone-number enumeration in audit trails.
- Screen coordinates and unrestricted shell commands are **not** in contract.
- Separate network policy and VM/container attestation for Level 3;
  Android Emulator by itself is not a hardened isolation boundary.
- Cancellation, crash recovery, idempotency, cleanup and retention policy.

## Run tests locally

```powershell
cd "C:\Users\kiril\Desktop\ai-voice-task-planner"
git pull
& .\.venv\Scripts\python.exe -m pytest backend/tests/test_genesis_android_contract.py -q
```

## Next integration milestone

Implement an independently deployed test-agent service using ADB only for
owned, explicitly allowlisted test applications. The agent should perform
`launch -> type demo-001 -> assert result -> clear -> return metrics`,
with Android UI assertions rather than mocked outcomes. Do not attach the
agent to the GENESIS Core process or advertise isolation as deployed.
