# Reuse existing programmable Android fixture and test generators

This workstream **reuses** the existing repository, not a duplicate clicker application.
The current source of truth for these components is the GitHub branch
`feature/phone-generator-api`. Changes must be reviewed/cherry-picked selectively
into the VM branch, not blindly merged.

## Existing verified repository components
- `backend/app/phone_generator.py` — optional FastAPI test phone formatter.
  The documented endpoint is `GET /api/v1/tools/generate-phone`.
  A formatted random number is **not** a guaranteed synthetic/unassigned number.
- `backend/tests/test_phone_generator.py` — endpoint-related tests.
- `experiments/android_fixture_app/` — standalone test Android package
  `dem.dev.genesis.fixturelab`, fixed UI selectors and three outcomes.
- `experiments/finance_fixture_check/` — bounded synthetic job generator,
  task runner, simulator, worker CLI and JSON reports.
- `experiments/finance_fixture_check/tests/` — existing runnable unit tests.

## Contract for the two VM execution test

1. Host runs the API and orchestrator. VM A and VM B host only approved L2 tasks
   and test application/emulator sessions; a task receives no host Docker socket.
2. Task schema: job id, synthetic fixture ids, requested case and permitted
   Android package, expected outcome, deadline. Never derive a target from a
   generated real-world phone number.
3. Worker A and worker B use independent workspaces and bounded queues. First
   exercise simulated task execution; only then boot the Android emulator within
   the guest if its CPU/RAM requirements fit. Guest KVM acceleration is not
   assumed simply because host `/dev/kvm` exists.
4. The fixture UI automation is allowlisted to
   `dem.dev.genesis.fixturelab`. Unsupported package names fail closed.
5. Results: match / mismatch / not_found / error, elapsed time and sanitized
   trace, with no names, actual phone numbers, PIN, payment action or account
   lookup.
6. Run a failed-worker experiment: stop VM A after job acceptance; use an
   idempotency key and retry via VM B. No duplicated completion.
7. Only after the fixture passes: implement the authenticated mTLS route
   API → L3 → L1 → VM worker; neither current fixture worker nor core status
   endpoint provides that production bridge today.

## Commands to run on an isolated staging checkout

```sh
python -m unittest discover -s experiments/finance_fixture_check/tests -v
python -m experiments.finance_fixture_check
python -m experiments.finance_fixture_check.generate --output /tmp/genesis-synthetic-job.json
```

Do not expose phone generator endpoint publicly or feed its random phone values
to an external application. For deterministic fixture tests use fixture IDs.

## Rollout status
- [x] Existing components identified on feature/phone-generator-api
- [x] Integration plan attached to VM workstream
- [ ] Version synchronization and regression tests
- [ ] VM A and VM B provisioned and Docker installed
- [ ] Android emulator/device actually running in guest
- [ ] Authenticated VM task routing
- [ ] Chaos/load testing
