# LokiTunes 🎧 — v0.9.0 Midnight Aurora Polish

Offline-first, free, open-source native Android music player (Android 8+, `minSdk 26`).

## What's new in 0.9.0

- **Smoother navigation:** short crossfade transitions between tabs; the Songs and Favorites list remember their scroll positions when switching tabs.
- **Home shortcuts:** quick access to Songs, Folders, Favorites and Library.
- **Mini-player:** discreet playback progress bar, with live position updates from the existing Media3 controller.
- **Now Playing:** gentle animated artwork transition and small scale change for play/pause, without continuous heavy animation.
- **Search:** background filtered/sorted queries with short debounce; multi-word searches can match across title, artist, album, file name and folder.
- **Folder Explorer:** nested folder counts are precomputed per library refresh, rather than recounting for every visible row.

No original music files are moved or deleted. No new permissions are needed. Existing Favorites, custom playlists, offline/online lyrics, EQ settings and signed in-app updates are preserved.

## Update the existing GitHub repository

Extract the PATCH ZIP **into the root of the existing LokiTunes repository**. Check that the updated files land in `app/src/...` (not inside a nested folder). Then:

```bash
git add -A
git commit -m "feat: LokiTunes v0.9.0 polish and performance"
git push origin main
```

Wait for a green **GitHub Actions** build, including unit tests and `assembleDebug`. Only then:

```bash
git tag v0.9.0
git push origin v0.9.0
```

A successful tag workflow publishes `LokiTunes-v0.9.0-release.apk`. Update **inside your installed signed LokiTunes** (Updates → Download → Install). Do not uninstall. **Use your original private signing key** in the existing GitHub secrets; do not regenerate it or upload it to the repository.

### What's preserved

- Android application ID: `com.lokij.lokitunes`
- `versionName = "0.9.0"`, `versionCode = 11`
- Android Media3 background playback, in-app GitHub release updates, favorite/playlist storage, smart offline/online lyrics, Sound Studio
- Original Midnight Aurora colors and offline music library

## Quality checks / limitations

See [TEST_PLAN.md](TEST_PLAN.md). Pure Kotlin smoke tests and syntax/static validations run locally; GitHub Actions must still compile the Android/Compose sources and the real device must be tested. Do not treat source-level validation alone as a successful release.
