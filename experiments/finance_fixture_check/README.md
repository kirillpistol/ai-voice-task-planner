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

Application/package and version, device/emulator transport, stable screen
selectors from the authorized environment, fixture manifest references, and
expected test outcomes. Login remains manual in the first integration; passwords
must not be supplied in chat or source. The orchestration contract is reusable,
while each application's selectors and actions require a specific adapter.
