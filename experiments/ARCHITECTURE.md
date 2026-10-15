# GENESIS execution lab — implementation status

This document describes the prepared test scaffold, not a deployed architecture.

| Layer | Responsibility | Actual status |
| --- | --- | --- |
| L1 | Kernel routing and architecture/state handoff | Existing synthetic circle; not wired to this worker |
| L2 | Approved jobs, algorithms, service orchestration | Local fixed-fixture validation and SQLite queue |
| VM agent | Select device, execute approved adapter, record outcome | ADB read-only probe; synthetic worker; UI adapter pending |
| Android | Dedicated controlled test interface | Fixture Lab source; separate APK build workflow |
| L3 | Prepare and deliver reports | Local JSON export; authenticated delivery pending |

## Local execution contract

Only version 1 offline-simulation manifests with bundled case IDs are accepted.
No shell command, password, real phone number or arbitrary application selector
is accepted as a job field. The generator produces synthetic job IDs, not phone
numbers. Fixture Lab is package dem.dev.genesis.fixturelab, separate from the
production mobile app. It requires no network or audio permissions.

State transitions: pending -> running -> completed/failed; pending -> cancelled.
An ID is unique in the queue. Claims and local report publication prevent blind
replay. A crash can leave a running claim, or a report with an unfinished queue
record. Operators must reconcile these; no exactly-once guarantee is claimed.
Failures retain state and artifacts for review. No automatic cleanup is enabled.

## Deployment boundary

Intended execution boundary is a Windows VM containing Android emulator and
agent. Current user environment has LDPlayer on the host; Windows guest boot and
ADB discovery remain unresolved. No VM isolation claim can be made from the
Python probe. No agent, UI automation or report delivery has been deployed.

The Finance profile (inbound VPN -> VM -> temporary Docker -> outbound VPN)
remains declarative. VPN provisioning, VM provisioning, container lifecycle and
report-before-cleanup enforcement are separate implementation work.

## Next integration contracts

1. Restrict UI adapter to the Fixture Lab package and its fixed resource IDs.
2. Measure emulator boot, device readiness, UI steps and report delivery separately;
   current worker duration measures only the synthetic comparison job.
3. Design authenticated worker enrollment, per-agent job authorization, bounded
   payloads, signed/versioned capabilities and cancellation semantics before
   adding server transport. No public ADB/Appium listener is required.
4. Persist accepted reports before permitting temporary container cleanup.
5. Connect approved L2 service sets and L3 algorithms only after their contracts
   and security boundaries are reviewed. This scaffold does not train a model.

Real bank beta applications are production environments unless explicitly backed
by a bank sandbox. The lab does not automate recipient enumeration or collection
of real names from transfer screens, and does not log in or submit payments.
