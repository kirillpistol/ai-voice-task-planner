# GENESIS OpenAI comment analysis (backend MVP)

Android OCR -> editable GENESIS sector comment -> user-reviewed action -> authenticated backend -> OpenAI Responses API -> suggested tasks, **not saved automatically**.

## Backend endpoint
POST `/api/v2/genesis/ai/comments/analyze`
Header: `Authorization: Bearer <Firebase ID token>`
Body: `{"sector":"...","comment":"...","consent_to_external_processing":true}`
Response: `{"summary":"...","tasks":[{"title":"...","description":"..."}],"input_tokens":123,"output_tokens":42,"model":"..."}`

No medical/document images are sent; **comment text** is sent to OpenAI only after explicit consent. Provider request sets `store=False`. Comments must be <=4000 characters. Results are suggestions only, not persisted, and no Firestore/calendar records are created by this endpoint. Never interpret document contents as system instructions.

## Configuration
1. Revoke any API key pasted into a chat or logs. Generate a new secret.
2. Deploy secret `OPENAI_API_KEY` to the private backend environment (secret manager / injected Docker environment). Do not commit it or include it in APK.
3. Optionally set `OPENAI_MODEL=gpt-5-mini`. Install `backend/requirements.txt`.
4. Configure API billing/budgets at provider, and protect backend access with Firebase.

## Next integration work
- Add Android endpoint client and a dedicated confirmation dialog showing summary/task proposals. Current Android memory comment preview is a different flow and **does not yet call this endpoint**.
- Explicit confirmation then create accepted tasks using existing authenticated task write API; deduplicate and validate individual proposals first.
- Replace per-process in-memory limiter (10 requests/user/hour) with shared Redis/SQL rate limit for multi-instance deployment.
- Record provider usage/cost metadata securely without storing sensitive comment text in logs.
- Implement structured outputs schema and prompt-injection regression tests, plus model availability check.
- Treat scanned sensitive data with special care; explicit per-submission opt-in required.
