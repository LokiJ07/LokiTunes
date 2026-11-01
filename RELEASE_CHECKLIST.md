# LokiTunes stable-release checklist

1. **Never regenerate the signing key.** Back up the original `lokitunes-release.jks` offline and keep GitHub Actions signing secrets unchanged. Confirm certificate SHA-256 fingerprint in the release job is the same as v0.9.0.
2. Inspect `git diff` before committing; ensure no `.jks`, `.p12`, password, base64 key or private personal file is tracked (`git status`, `git ls-files`).
3. Push commit to main; verify both `testDebugUnitTest` and `assembleDebug` are green.
4. Complete required on-device checks from `TEST_PLAN.md`, especially update-over-install, Favorites/playlists, offline scan and background playback.
5. Create a **tag only after QA passes** (`v1.0.0`). Do not reuse/move a published release tag.
6. Ensure official signed APK is published by the tag workflow, and install **only from GitHub Releases**.
7. Confirm latest release is visible to users and automatic updater detects it from installed v0.9.0.
8. If a critical problem appears, publish a **new** signed version with a higher `versionCode`, e.g. v1.0.1 / build 13, instead of replacing signed APK content behind an existing tag.

## Build status clarification

Local lightweight tests and static file validation can help find issues but are **not** substitutes for an Android SDK compile, signing verification or device tests.
