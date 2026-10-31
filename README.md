# LokiTunes 🎧 — v0.5.0 · Sound & Personalization

**Your music. Your rules.** Free, open-source, native Android offline music player, Midnight Aurora (black / purple / cyan), folder explorer, Favorites, queue and sleep timer, and signed GitHub Releases in-app updates.

- Android 8+ (`minSdk = 26`), package `com.lokij.lokitunes` (unchanged).
- New version **0.5.0**, Android **versionCode 7** (higher than v0.4.0 build 6).
- No music streaming, lyrics servers, accounts, ads or extra network access. The existing GitHub updater is the only internet-dependent part.

## What's new in v0.5.0

### 🎤 Offline lyrics (LRC + TXT)

1. Play a track and open **Now Playing**.
2. Tap **Lyrics** → **Load .lrc / .txt**.
3. Select a **local** text file using Android's system picker (the picker may show other file types; choose `.lrc` or `.txt`).
4. For a timestamped `.lrc`, the current line highlights automatically while the song plays. Multi-timestamp lines and `[offset:...]` are supported.
5. For `.txt`, read scrollable **plain lyrics** (no synchronization).
6. Lyrics are associated with that exact music-library track. The selected file URI is remembered across restarts/signed updates as long as Android can still access it.
7. Tap **Change file** or **Remove** to update the selection. Original audio and lyric files are never modified.

**Important:** This version does not automatically find lyrics on the internet or automatically match `.lrc` files in music folders. The user chooses the file for each track. If a lyric file is moved/deleted, reselect it. No copyrighted lyrics are bundled with the app. `examples/demo-lyrics.lrc` contains an original demonstration file.

### 🎚️ Sound settings

- **Now Playing → Sound**: choose **0.75×, 1×, 1.25×, 1.5×, or 2×** playback speed. Speed preference persists across app restarts and signed upgrades and applies to the background Media3 playback service.
- **Open device equalizer** asks Android to display the phone's **system audio effects panel**. Some phones don't have one, and some effects panels may not apply to LokiTunes; the app shows a message if none is found. **This is not a built-in multi-band EQ.** A custom EQ needs separate cross-device research/testing.
- All existing playback controls, sleep timer, queue, folder navigation, in-app updater and Favorites are intended to remain unchanged.

## How to ship the signed update (no Android Studio)

**KEEP the original LokiTunes signing keystore / GitHub Secrets from v0.3.x and v0.4.0.** Never create a replacement keystore for an existing installed user base. DO NOT uninstall the current signed LokiTunes; keep Favorites and other local data. You can optionally create a Favorites JSON backup.

1. Extract the **v0.5.0 PATCH ZIP**, merge it into your existing LokiTunes Git repository, keeping paths intact. Do **not** extract the full-source ZIP into a different Git working directory unless deliberately replacing your project.
2. Commit and push the code:

   ```bash
   git add -A
   git commit -m "feat: LokiTunes v0.5.0 offline lyrics and sound settings"
   git push origin main
   ```

3. Open **GitHub → Actions** and make sure the **main** build is green, including `:app:testDebugUnitTest` and `:app:assembleDebug`. If it fails, **do not tag yet**; inspect the first compilation error.
4. Confirm `app/build.gradle.kts` contains `versionName = "0.5.0"` and `versionCode = 7`. Then tag that exact tested commit:

   ```bash
   git tag v0.5.0
   git push origin v0.5.0
   ```

5. Wait for the **tag-triggered signed build** and **Publish signed public GitHub Release** to both succeed. Confirm Releases contains **`LokiTunes-v0.5.0-release.apk`**, signed using the SAME certificate as the previously installed release.
6. On your phone, open your **installed signed v0.4.0** → Updates → Download / Install. Approve Android's installer. After relaunch, Updates should display **v0.5.0 · Build 7**. Music and Favorites should remain.

**If the installer says package conflict, do NOT uninstall**: verify the installed APK and downloaded APK use the same signing certificate; use GitHub **Releases** APKs for updates, never debug artifacts from Actions.

## Known limits / release gate

- Parser and speed-preset pure-Kotlin smoke checks, XML/YAML/static inspection and ZIP integrity validation are performed for source distribution.
- **A complete Android Gradle compilation and physical-device test have NOT been run here.** Only publish the tag after the actual GitHub Actions build succeeds. Test speed, LRC sync, background playback, Favorites and update installation on the phone before calling the release stable.
- External equalizer support depends on the device, and opening the system panel without a specific ExoPlayer session ID does not guarantee effects will apply to LokiTunes.
- User-selected `.lrc`/`.txt` documents are limited to 200k characters to avoid excessive memory use.

## Project and release details

- Workflow: `.github/workflows/android.yml` (unchanged from working v0.4.0).
- New lyric parser: `app/src/main/java/com/lokij/lokitunes/lyrics/LrcParser.kt`.
- New lyric URI persistence: `app/src/main/java/com/lokij/lokitunes/lyrics/LyricsStore.kt`.
- UI: `app/src/main/java/com/lokij/lokitunes/ui/LokiApp.kt`.
- Playback speed persistence: `ui/LokiViewModel.kt` and `playback/MusicPlaybackService.kt`.
- Tests: `app/src/test/java/com/lokij/lokitunes/{lyrics,ui}`.
- License: MIT. Keep signing keys, `.jks` files, passwords and decoded secrets **out of public GitHub**.
