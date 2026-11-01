# Contributing to LokiTunes

Thanks for considering helping with this free, open-source Android music player!

## Before opening a contribution

1. Check [existing issues](https://github.com/LokiJ07/LokiTunes/issues) to avoid duplicates.
2. For a bug, include **Android version, LokiTunes version, device model, expected behavior, actual behavior and reproducible steps**.
3. For features, explain what problem they solve for an offline-first music player.
4. Keep third-party lyrics and media licensing in mind. Do not submit copyrighted music files or unlicensed lyric databases.

## Development setup

The source is Kotlin + Jetpack Compose, with Media3 for playback and GitHub Actions for CI. Open the project with a compatible Android SDK/Gradle environment or push a branch to run the existing CI workflow.

- Match the current project settings in `app/build.gradle.kts`.
- Keep changes targeted; include tests for rules/parsers where practical.
- Run `:app:testDebugUnitTest` and `:app:assembleDebug` before asking for review.
- Treat **device testing** as necessary for playback, Bluetooth, permissions and audio effects.

## Safety and releases

- **Never commit signing keys**, keystores, secrets or user audio files.
- Do not modify the production `applicationId` without discussion; it would break ordinary updates.
- Do not move existing published version tags or replace signed release assets silently.
- The project owner manages official releases and the long-lived signing key.

## Pull requests

Describe what changed, why, test results, and any relevant screenshots. A submitted PR is not a guarantee of acceptance.

The repository is licensed under [MIT](LICENSE). By contributing, you agree to submit the work under the project's license or otherwise clearly disclose restrictions before merging.
