# GENESIS Fixture Lab

Standalone native Android UI for the first emulator automation test. Open this
directory as a separate project in Android Studio. Requires JDK 17, Android SDK
35 and a Gradle version compatible with Android Gradle Plugin 8.7.3 (Gradle 8.9).
The wrapper is not bundled; configure Gradle locally or generate it with an
existing compatible Gradle installation. Do not reuse the main application's
Gradle version without checking compatibility.

Build the app's debug APK, then install it in the selected emulator. Package:
`dem.dev.genesis.fixturelab`. This app does not replace PISTOL GENESIS.
No runtime dependencies or manifest permissions; no microphone or network use.

## Fixed automation contract

Use resource IDs under `dem.dev.genesis.fixturelab:id/`:

| Select button | Check button | Result (`fixture_result`) |
| --- | --- | --- |
| fixture_match | check_fixture | match |
| fixture_mismatch | check_fixture | mismatch |
| fixture_missing | check_fixture | not_found |

`reset_fixture` clears selection and sets result to `idle`. Check is disabled
until selection. Choosing another case invalidates the prior result. After an
Activity recreation, selection is retained but result must be checked again.

First real UI test: select case, click check, wait for expected result, reset,
verify idle. Restrict any future adapter to this exact package. No real bank UI
adapter, login or financial operations are included. APK build and device test
have not yet been performed in this environment.
