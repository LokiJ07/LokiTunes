# LokiTunes Changelog

## v0.3.2 — Signed in-app update compatibility test
- Bump Android versionName to **0.3.2** and versionCode to **5** (previous: 0.3.1 / code 4).
- Show version **and build number** in the existing Updates dialog to confirm a successful install-over update.
- Add semantic-version unit tests for 0.3.1 → 0.3.2.
- Preserve application ID `com.lokij.lokitunes`, signing setup, playback, Favorites, offline scanning, queue and Midnight Aurora UI.
- This test does **not** bypass Android signing requirements: install the signed 0.3.1 GitHub Release first.

## v0.3.1 — First public in-app updater test
- Increment Android versionCode to 4 and versionName to 0.3.1.
- Display installed version in the Updates dialog for easy verification.
- Extend unit test coverage for version comparison.
- Preserve playback, Favorites, queue, sleep timer, UI theme, package ID, and stable release signing workflow.

## v0.3.0 — Signed releases + in-app updates
- GitHub Releases update checker; verified APK download; Android installer handoff.
- GitHub Actions signed release publishing when version tags are pushed.

## v0.2.0 — Play queue + Sleep timer
- Editable queue, sleep timer, Favorites sorting and local JSON backup/restore.

## v0.1.0 — Initial offline music player
- Automatic audio library scanning, playback, folder browser, Favorites and Midnight Aurora UI.
