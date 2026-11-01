# LokiTunes 🎧 — v0.7.0 Smart Music Library

**Offline-first Android music player** with Midnight Aurora UI. Free, no ads, open source. Android 8+ (`minSdk 26`).

**Package remains `com.lokij.lokitunes`; versionName = `0.7.0`; versionCode = `9`.** Continue using the **same GitHub release signing keystore/secrets** you used for the previous signed releases. The app's Favorites, Lyrics cache, queue controls, and GitHub updater remain in place.

## New in v0.7.0

### Custom playlists — locally saved

- Open **Library → Playlists → New playlist** to create a custom playlist.
- Tap a playlist, choose **Add songs**, and search the existing music library. You can add multiple songs; already-added songs are checked.
- Or open any song's **⋮ menu → Add to playlist**, and select an existing playlist.
- Open a playlist and tap **Play** to play its available songs in saved order.
- Rename playlists with the pencil icon; delete a playlist using the trash icon with confirmation. To remove one song only, open its **⋮ menu → Remove from playlist** inside the playlist.
- Playlists are stored in private app preferences, not online. No accounts or internet are needed. Playlist actions **never delete, move, or modify** actual audio files.
- A playlist may show “unavailable song(s)” if the underlying files were moved, deleted, or reindexed by Android. Missing songs are skipped during playback rather than crashing. Playlists refer to MediaStore song IDs and may not survive file moves or a fresh installation without a future dedicated backup/restore feature.

### Better sorting and searching

- Open **Songs → Sort** and choose: Title A–Z, Recently added, Artist A–Z, Album A–Z, or Longest first.
- The chosen sort preference is retained after app restart and normal signed updates.
- Search checks song title, artist, album and audio folder path. Existing folder list/explorer modes are preserved.

### Potential duplicate finder (read-only)

- Open **Library → Duplicates**.
- Groups are based on case-insensitive title + artist and song duration within two seconds. The feature **does not compare file contents or prove exact duplication**.
- Unknown-title/unknown-artist recordings are excluded from the heuristic to avoid excessive false positives.
- Results allow playback, favoriting, or adding to playlists. **There is no delete-file action.**

## GitHub build and update (no Android Studio on your PC)

1. Apply the patch files over your existing v0.6.0 project. Do not create a nested extra folder.
2. Push the changes to `main`:

```bash
git add -A
git commit -m "feat: LokiTunes v0.7.0 Smart Music Library"
git push origin main
```

3. Open **GitHub → Actions** and confirm `:app:testDebugUnitTest` and `:app:assembleDebug` succeed. A green main build is necessary but is not yet confirmation of real-device behavior.
4. Only after the build succeeds, publish the **same commit** with:

```bash
git tag v0.7.0
git push origin v0.7.0
```

5. Verify the **signed** `LokiTunes-v0.7.0-release.apk` is published under GitHub Releases, then in the already-installed **signed** LokiTunes v0.6.0 use **Updates → Download → Install**. Never use the Actions debug APK as an update to the signed app.

**Important:** Do not replace or regenerate the `LOKITUNES_*` release keystore/secrets. Losing or changing your private signing key prevents normal install-over updates of existing signed installations. No private `.jks` or passwords are included in this ZIP.

## v0.7.0 validation status

- Pure Kotlin core compilation and **10 smoke assertions passed** locally for playlist naming, unique membership, sorting, and duplicate detection.
- Kotlin JVM unit test files added for `PlaylistRules` and `LibraryInsights` in the GitHub Android test workflow.
- Android XML and GitHub workflow YAML were checked for validity, and ZIP integrity was verified.
- **A full Android/Compose APK build was not performed locally.** GitHub Actions must confirm compilation before release, and the real phone must confirm the UI and update path.

See [TEST_PLAN.md](TEST_PLAN.md) and [CHANGELOG.md](CHANGELOG.md).

## Privacy

- Offline scanning and playlist operations need no network connection.
- Optional online lyrics from v0.6.0, and GitHub update checks/downloads, use internet only for their own functions.
- No audio files are uploaded by the new library features.
- License: MIT.
