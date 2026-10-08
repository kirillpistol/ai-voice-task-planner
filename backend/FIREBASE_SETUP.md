# PISTOL GENESIS — Firebase setup before Cloud Run billing

Firebase project: **pistol-genesis**. Android application ID: `dem.dev.genesis`.
Apple bundle ID: `dem.dev.genesis`.

## 1. Authentication

1. Open [Firebase Authentication](https://console.firebase.google.com/project/pistol-genesis/authentication/providers).
2. Select **Get started** if prompted.
3. Enable **Email/Password** only if the first Android test will use email/password accounts.
4. Do **not** enable anonymous login merely to bypass missing account setup.
5. The existing Android login still uses the **old REST /api/v1 endpoint** and must be
   migrated or adapted before Firebase ID token verification can work.

## 2. Firestore

1. Open [Firestore](https://console.firebase.google.com/project/pistol-genesis/firestore).
2. Create database in **Native mode** if not already created.
3. Choose a region close to your Cloud Run region before creating the database
   (the location is not casually changeable afterward).
4. Set restrictive rules; do not select permissive test rules for production.

The v2 backend uses Admin SDK with the Cloud Run runtime service account, not
the mobile Firestore SDK. This means **client security rules are NOT the backend's
permission boundary**. Every endpoint must authenticate the Firebase ID token
and scope database reads/writes to its validated UID. Configure the service account
with minimum necessary IAM permissions.

## 3. GitHub checks, no billing required

Open the repository's **Actions** tab, select **GENESIS backend checks** and
inspect the latest run. The workflow builds the Docker image and runs tests.
A green CI run does **not** replace deploying and integration-testing Firestore.

## 4. Once Cloud Run billing is available

- Enable Cloud Run / Cloud Build / Artifact Registry APIs in project pistol-genesis.
- Provision runtime service account and Firestore database.
- Deploy the backend source from the root repository directory.
- Check `GET /healthz` for a running process.
- Check `GET /readyz` for Firestore connectivity.
- Obtain the generated HTTPS endpoint and verify Firebase bearer-token auth.
- Integrate Android Firebase Authentication and v2 task DTOs before replacing the
  legacy Kotlin API URL. Do not ship a client that calls unimplemented /api/v1 routes.
- Run Debug APK build, install it, and test registration, login and task CRUD.

## Billing safety

Google Cloud **budgets are alerts**, not spending caps. Check services' pricing,
set alerts, and disable unneeded services. Do not deploy Cloud SQL unless it is needed;
the current v2 backend is designed to use Firestore.

## Secrets

Never push Firebase Admin service-account JSON, passwords or private SSH keys.
Use Cloud Run's workload identity via attached runtime service account.
