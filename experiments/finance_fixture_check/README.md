# Controlled Android financial test: first contract

Target direction: Android and a dedicated test account. This commit implements
the offline runner/adapter contract, comparison algorithm and L3 JSON report.
It does not implement or operate the real banking application, perform number
enumeration, collect recipient directories, receive SMS or send transfers.

## Boundaries

- L2: approved fixture manifest, comparison logic, adapter/session interfaces.
- Execution adapter: manually authenticated dedicated Android test session.
  Only fixed agreed fixture IDs are accepted; actual test data mapping stays
  local to the authorized test environment. Credentials are not part of reports.
- L3: counts and per-case verdicts, report delivery and subsequent temporary
  workspace cleanup. This first version exports a local JSON file, not an app
  download endpoint or a Docker/VM deployment.
- Four results: match, mismatch, not_found, error. No money/amount/confirmation
  action is present in the contract.

The shipped simulator uses synthetic labels and fixture IDs, not real phone
numbers. It cannot select a live adapter. A real Android adapter will be a
separate implementation after agreeing the exact environment, bounded fixture
list and UI selectors. It must stop before amount entry/payment submission.

## Run from repository root (Python 3.10+)

```bash
python -m experiments.finance_fixture_check
python -m experiments.finance_fixture_check --report finance-test-report.json
python -m unittest discover -s experiments/finance_fixture_check/tests -v
```

Report creation is explicit and exclusive; existing files are never replaced.
Synthetic runner state is local and contains no credentials. The manifest is
validated in full before opening a session, and cleanup runs in finally.

## Android adapter inputs still needed

### Persistent local worker

Generate a fresh synthetic job (exclusive file creation; no real phone numbers):

```powershell
python -m experiments.finance_fixture_check.generate --output synthetic-job.json
```

Submit this generated file instead of example-job.json when a new job ID is needed.
See ../ARCHITECTURE.md for the deployment boundaries and pending integrations.

Run inside the intended VM from the repository root:

```powershell
python -m experiments.finance_fixture_check.worker --workspace fixture-workspace submit --job experiments/finance_fixture_check/example-job.json
python -m experiments.finance_fixture_check.worker --workspace fixture-workspace run-once
python -m experiments.finance_fixture_check.worker --workspace fixture-workspace status
```

SQLite preserves job states across restarts. Each invocation processes at most
one job. Two workers cannot claim the same pending job. Duplicate IDs are
rejected. `cancel --job-id synthetic-demo-001` cancels only pending work.
Interrupted running jobs remain running for operator review; there is no
automatic retry or exactly-once guarantee across a crash. If publication succeeds
but the state update fails, reconcile the retained report manually.
Reports live under `fixture-workspace/reports`; duration_ms measures the offline
job only, not emulator startup or Android UI latency. Workspaces are local trusted
directories, not shared untrusted inboxes. This worker has no HTTP listener,
server polling, credentials, microphone access, or real application automation.

### Local job execution

```powershell
python -m experiments.finance_fixture_check.job --job experiments/finance_fixture_check/example-job.json --output fixture-reports
```

The local job protocol accepts only the bundled synthetic fixture IDs. It has no
shell-command field or live adapter. A job ID is claimed once; retries with that
ID are rejected, including after interrupted execution. Review failed claims
manually. Complete reports are published without overwrite using a same-directory
hard link (requires a filesystem supporting hard links, such as NTFS).
This is local execution only: server polling, authentication and report upload
are not implemented. The ADB probe remains independent of this simulation.

### VM-local connectivity probe

Run the agent only inside the designated Windows VM, alongside LDPlayer.
The probe does not verify VM isolation; deployment must enforce this boundary.
Enable the emulator's local ADB access and obtain its serial with `adb devices`.
Then run from the repository root inside that VM:

```powershell
python -m experiments.finance_fixture_check.android_probe --adb "C:\path\to\adb.exe" --serial "emulator-5554"
```

Replace both example arguments with the actual executable path and device serial.
This checks only device authorization, boot completion and Android version.
It does not open apps, log in, read UI content, or connect ADB over the internet.
No device is selected automatically. Each command has a 15-second timeout.
This is a connectivity foundation, not a completed UI agent or L1/L2 integration.

Application/package and version, device/emulator transport, stable screen
selectors from the authorized environment, fixture manifest references, and
expected test outcomes. Login remains manual in the first integration; passwords
must not be supplied in chat or source. The orchestration contract is reusable,
while each application's selectors and actions require a specific adapter.
