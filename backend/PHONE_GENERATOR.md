# Phone Generator API

A standalone, optional test-data endpoint within the existing PISTOL GENESIS FastAPI backend.

`GET /api/v1/tools/generate-phone?prefix=999&count=3`

Response shape:

```json
{"count":3,"phones":["+79990000001","+79990000002","+79990000003"]}
```

The example numbers illustrate formatting only. Actual responses are randomized.

- `prefix`: 3 digits, starting with 9; default `999`.
- `count`: from 1 to 1000; default 1.
- Numbers are unique **within one response** only.
- A prefix matching the syntax is **not** proof of an assigned telecom range.
- Generated numbers may belong to real people. Do not contact them; use controlled test fixtures when you need guaranteed fictional numbers.
- There is no subscriber lookup, active-number validation, SMS sending, or number ownership verification.

The route is mounted from `app/main.py` and uses the backend's existing dependencies. In a deployed environment, restrict access and add authentication/rate limiting before exposing bulk generation publicly.

Run tests from the `backend` directory after installing requirements and pytest:

```sh
python -m pytest tests/test_phone_generator.py
```
