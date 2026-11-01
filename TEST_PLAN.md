# LokiTunes v0.9.0 Test Plan

## Build / release (GitHub)

- `:app:testDebugUnitTest` passes, including new `LibraryBrowseTest`.
- `:app:assembleDebug` passes; no experimental API annotation errors.
- Signed `v0.9.0` release builds and passes `apksigner verify`.
- Signed GitHub release APK is named `LokiTunes-v0.9.0-release.apk`.

## Device checks (on existing signed app, without uninstall)

1. Check song scan, playback, Bluetooth, lock screen and background controls as before.
2. Browse all tabs rapidly; each fades gently and selection always matches content.
3. Scroll Songs deeply → Favorites → back to Songs; prior list position stays. Repeat for Favorites.
4. Search for a title fragment, a multi-word title/artist combination, folder and file name. Clear search.
5. Scan a library with many tracks; scroll the folder explorer and compare displayed nested counts.
6. Open the mini-player; watch progress advance, pause, seek, and skip. Avoid showing a progress bar when duration is unknown.
7. Toggle play/pause; ensure subtle artwork motion works and no stutter. Rapidly skip tracks with/without album art.
8. Check Sound Studio equalizer, bass boost, playback speed, sleep timer, lyrics, favorites, playlists, exports and in-app updates still work.
9. Upgrade from signed v0.8.0 to signed v0.9.0 without uninstall. Favorites, playlists and settings must remain.
10. Check very narrow/small screens: no clipped important controls; quick-access shortcuts must scroll horizontally.

## Automated / static checks

- `LibraryBrowseTest.kt` covers search matching, empty query, folder counting, backslash normalization.
- Zip integrity, manifest XML, GitHub YAML and version/application ID checks.
- Full Android compile needs GitHub Actions/SDK; no APK compile was claimed locally.
