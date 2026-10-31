# LokiTunes 🎧 — v0.2.0

**Your music. Your rules.** A fully offline, free, open-source Android music player with the original **Midnight Aurora** purple/cyan theme.

Maintained by **LokiJ07**. Minimum supported Android: **8.0 (API 26)**. This repository contains **source code**, not copyrighted music. License: MIT.

## What's new in v0.2.0

- **Now Playing → Queue:** see current playback queue, tap a song to jump to it, move items up/down, or remove them. Queue edits operate on the actual Media3 player, not a fake list.
- **Now Playing → Sleep timer:** choose 10, 20, 30, 45, 60, or 90 minutes; live remaining-time indicator and cancel action. The timer runs in `MusicPlaybackService`, not just the screen, while Android allows the service to run.
- **Favorites sorting:** A–Z, recently liked, artist. Favorites from v0.1.0 continue to work if the new APK is installed **over** the old app using the **same signing certificate**.
- **Favorites → Backup/Restore:** export a small JSON file and import it later. Import merges known matching songs with existing favorites; it does not import audio files. Helps with future signing changes, device moves, or app reinstalls **after** a backup is made.
- v0.1.0's functioning library scanning, folder list/explorer, album/artist lists, search, playback, mini-player, media notification, and UI remain unchanged.

### Important limitations

- The sleep timer is best-effort: Android may kill playback services under aggressive battery-management settings. If the service/process is terminated unexpectedly, the timer cannot run until the service restarts. It pauses playback rather than powering off the phone.
- Queue edits are for the **current playback session**. Replacing the play queue by selecting a different song list resets them. Queue isn't yet persisted across Android process death.
- **Favorites restore** first matches stored MediaStore URI IDs, then matches title + artist + folder + duration. If files moved, changed metadata or are missing, some favorites may not match. Export does not include MP3 files.
- Songs inside inaccessible/protected or unindexed folders may not be detected by Android MediaStore. Android controls audio decoding support.
- Original v0.1.0 Favorites do **not** have date records. Their `Recently liked` order may default to alphabetical until favorites are newly toggled. Existing favorite statuses remain.

## GitHub-only build (no Android Studio needed)

1. Extract the **full source ZIP** and open the extracted `LokiTunes` project, or apply the **patch ZIP** over an existing LokiTunes repository. **Keep the `.github` folder.**
2. Commit and push all updated files to the `main` branch. GitHub Actions will trigger **LokiTunes Android Build**.
3. Open GitHub **Actions → latest green build → Artifacts**, then download and extract `LokiTunes-v0.2.0-debug-apk` to get `app-debug.apk`.
4. Install on your Android phone and verify playback, queue, timer, favorites, and background operation.

From Git Bash inside the existing repository after applying the patch:

```bash
git add -A
git commit -m "feat: LokiTunes v0.2 queue sleep timer and favorites backup"
git push origin main
```

### ⚠️ Signing and existing Favorites — read before installing

Every GitHub-hosted runner typically generates a **different debug signing key**. Android **will not update** a v0.1.0 debug APK with a v0.2.0 APK signed by a *different* key. You may get **App not installed** due to signature mismatch. **Do not uninstall the current app if you need to retain its stored Favorites.**

For reliable future updates, generate a **private release signing keystore** on your machine and store its base64 contents plus credentials as GitHub Actions secrets. The existing workflow supports:

- `LOKITUNES_KEYSTORE_BASE64`
- `LOKITUNES_KEYSTORE_PASSWORD`
- `LOKITUNES_KEY_ALIAS`
- `LOKITUNES_KEY_PASSWORD`

Once these four secrets are configured, GitHub Actions also uploads `LokiTunes-v0.2.0-signed-release`. Install release builds signed with **the same keystore** for all subsequent updates. Switching **once** from an existing debug installation to a signed release still requires uninstalling the debug APK (and clears app-local data). Since v0.1.0 has no backup button, **back up important Favorites manually before any uninstall** (for example, a screenshot or list of liked songs). From v0.2.0 onward, use the new **Favorites → Backup** button before reinstalling.

Never commit the keystore, credentials or APK signing secrets to the public repository. Debug artifacts should be considered development builds, not official security-verified releases.

## Architecture

- Kotlin 2.3.21, Compose/Material3, Android Gradle Plugin 8.13.2, Gradle 8.13 / Java 17 / compile SDK 36.
- Media3 ExoPlayer 1.11.1 and `MediaSessionService` for background audio playback.
- MediaStore for indexed local songs; app-only SharedPreferences for Favorites and sleep-timer deadline.
- Android Storage Access Framework document pickers for backup/restore. No account, Internet permission, ads, telemetry, cloud server, database, or remote music download.
- Unit tests under `app/src/test` include folder helper behavior and timer calculations.

## Inspect → Improve → Test → Fix

v0.2.0 was based directly on the user's **working v0.1.0 GitHub build**. Config/manifest versions, Kotlin source structure, and ZIP file integrity were inspected. Android SDK/Gradle are **not available** in the authoring environment; the **next GitHub Actions run** will verify actual Kotlin compilation, unit tests, and APK creation. Do not call the new build production-tested until it has passed both GitHub Actions and a real Android phone smoke test.

If the build fails, open the failing GitHub Actions step, locate the first Gradle error, and share it to fix the root cause. If it passes, test `Play → Queue → Move/Remove`, `Sleep Timer → Cancel`, Favorites sorting, Backup/Restore, and playback with screen off.
