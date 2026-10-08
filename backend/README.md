# PISTOL GENESIS — Cloud Run backend

Initial deployable **bootstrap**, not yet the final mobile backend. The existing Kotlin
application calls `/api/v1/auth/register`, `/api/v1/auth/login`,
`/api/v1/tasks`, `/api/v1/tasks/delete`, `/api/v1/tasks/edit` and other
routes. Their request and response schemas have **not been verified**. Do not switch
the Android `BackendEndpoints.kt` to this service until those contracts are implemented.

## Google Cloud project

Use project `pistol-genesis`. Billing and these APIs are required:
Cloud Run, Cloud Build, Artifact Registry, Firestore. Create the Firestore database
in Native mode in a suitable region. Provision a dedicated runtime service account
with the narrow permissions needed to access Firestore. Do not put a service-account
JSON key in the repository; Cloud Run uses its attached service account automatically.

## Container

From this directory:

```sh
docker build -t genesis-api:dev .
docker run --rm -p 8080:8080 -e PORT=8080 genesis-api:dev
curl http://localhost:8080/healthz
```

`/healthz` works without cloud credentials. `/readyz` verifies Firestore access
and returns HTTP 503 until correctly configured.

After enabling billing and IAM, deploy with Cloud Run source build:

```sh
gcloud config set project pistol-genesis
gcloud run deploy genesis-api --source ./backend --region europe-west1 \
  --service-account=YOUR_RUNTIME_SERVICE_ACCOUNT
```

Run this from the repository root. Review any Cloud Run prompt about public
access. A mobile API needs an intentionally designed authentication mechanism
before it can safely be opened to the internet.

**Important:** Never send raw passwords, private keys, or long-lived admin tokens
in chat or commit them to GitHub. Keep secrets in Secret Manager.

## Next work

1. Confirm Kotlin request/response DTOs for auth, users, devices and tasks.
2. Implement compatible endpoints and validate authorization for each user.
3. Add automated contract tests and Firebase/Firestore integration tests.
4. Replace the Android endpoint with the actual HTTPS Cloud Run URL.
5. Test APK on a phone.

## New authenticated v2 API

The new routes are separate from the **legacy** Android v1 contract:

- `GET /api/v2/session` validates a Firebase Authentication ID token.
- `POST /api/v2/tasks` creates a task.
- `GET /api/v2/tasks?limit=50` lists the caller's tasks.
- `PUT /api/v2/tasks/{task_id}` replaces a task.
- `DELETE /api/v2/tasks/{task_id}` deletes a task.

All v2 routes require `Authorization: Bearer <Firebase ID token>`. Firebase
Authentication must be enabled in the Firebase Console and the Android app must
sign users in to obtain ID tokens; **the existing app does not yet do this**.
Tasks are stored under Firestore `users/{uid}/tasks/{taskId}`. Each API
request only accesses the verified token holder's UID.

Before phone testing: validate Firestore IAM, use a dedicated Cloud Run service
account, and implement compatible Android DTOs and token refresh handling.
For local tests install dependencies plus pytest:

```sh
python -m pip install -r backend/requirements.txt pytest
cd backend
python -m pytest -q
```

Tests have not yet been run in a fully provisioned deployment environment.
The `/api/v1` routes are still intentionally not implemented.
