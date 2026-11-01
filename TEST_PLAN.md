# LokiTunes v0.7.0 — Inspect → Improve → Test → Fix → Retest

## GitHub Actions before tagging

1. Push to `main`, confirm **Run unit tests and compile debug APK** passes (`:app:testDebugUnitTest` and `:app:assembleDebug`).
2. Inspect the first failure if red; do not tag/release until it is green.
3. Check the signed release job after pushing `v0.7.0`. Confirm release filename and `apksigner` verification. Do not change signing secrets.

## Real phone checks (signed v0.6.0 → signed v0.7.0)

1. Before updating, note your existing Favorites and saved lyrics; verify they remain after in-app update **without uninstalling**.
2. Library → Playlists → create "Night Drives". Verify duplicate name with different case is rejected. Rename. Add 3 songs, try adding one twice and verify no duplicate membership.
3. Play a playlist in order; next/previous and background playback should work normally. Remove a song from playlist and verify the actual MP3 still exists in Songs.
4. Delete the playlist after confirmation and verify no audio files or Favorites are deleted.
5. Try opening a playlist after deleting/moving one indexed song outside LokiTunes and refreshing; missing entries should be skipped, not crash playback.
6. In Songs, test all 5 sort modes and restart the app to confirm the selected sort persists. Search by album name and folder path.
7. Place two songs with similar title + artist + duration in separate folders: Library → Duplicates should show a *potential* match. Try files with different artists and very different lengths: should not be grouped.
8. Confirm no delete-audio button exists in Duplicates.
9. Regression: folder list/explorer, Favorites backup/restore, Play Queue, Sleep Timer, Now Playing, lyrics (local + optional online), GitHub updater, offline playback with Wi-Fi off.
10. Check large libraries, repeated quick playlist taps, multiple screen rotations, and back navigation for Compose/runtime issues.

## Local verification performed during packaging

- Pure Kotlin PlaylistRules/LibraryInsights compiled with `kotlinc`, and 10 smoke assertions passed.
- Full Android SDK / Compose build unavailable in the packaging runtime — **not claimed as passed**.
