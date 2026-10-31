# LokiTunes 🎧

**Offline music. No account. No ads. Your device, your rules.**

LokiTunes is a free, open-source, **native Android** local music player with an original **Midnight Aurora** (black, purple, cyan) interface.

**Version:** `0.1.0` (first development build). **Minimum Android:** Android 8.0 / API 26.

## Already coded for v0.1

- Detect audio indexed by Android MediaStore from accessible shared storage.
- Home/recent tracks, all songs, search, alphabetical/newest sorting.
- Two folder modes: folder list + hierarchical audio folder explorer.
- Favorites with local persistence across app launches.
- Album/artist browsing under Library.
- Media3 / ExoPlayer playback, background MediaSessionService, Android media controls.
- Mini-player, full-screen Now Playing, seek, shuffle, repeat, previous/next.
- Embedded artwork in Now Playing, with a beautiful gradient fallback where no art exists.
- Audio permissions adapted to Android version. No `INTERNET` permission or user account.

**Not yet implemented:** custom playlists, synced lyrics, sleep timer, equalizer, dynamic artwork-based theme, custom folder picker, music exclusions, queue editor, automatic upgrade checker. Folder Explorer displays **folders with indexed audio**, not arbitrary system folders. Some hidden, protected, or unindexed files are not visible because Android controls access to shared storage. Device codec support varies.

## Build entirely on GitHub — no Android Studio required

1. Create a **public** GitHub repository called `LokiTunes` (e.g., `LokiJ07/LokiTunes`). Do **not** generate a README/license on GitHub, since this ZIP already includes them.
2. Download and **extract** the source ZIP. Upload the **CONTENTS of the `LokiTunes` folder** to the root of your new GitHub repo, **including `.github/workflows/android.yml`**. GitHub web upload can miss hidden `.github` folders; easiest is `git add -A` from a terminal in the extracted folder (see commands below).
3. GitHub → **Actions** → **LokiTunes Android Build** → **Run workflow** (or just push to `main`).
4. Open the successful workflow → **Artifacts** → `LokiTunes-v0.1.0-debug-apk`. Download the artifact ZIP and extract `app-debug.apk`.
5. Copy `app-debug.apk` to your Android phone, tap it, allow install from your trusted file manager/browser, then grant **music/audio access** to LokiTunes.

> This source ZIP is **not an APK**. The ZIP alone cannot install on Android; GitHub Actions compiles the APK. First CI build requires internet for Android Gradle dependencies. App playback itself is offline.

### Git CLI upload (from inside the extracted LokiTunes directory)

```bash
git init
git branch -M main
git add -A
git commit -m "feat: LokiTunes v0.1 offline music player"
git remote add origin https://github.com/LokiJ07/LokiTunes.git
git push -u origin main
```

**If you already created a Git repo**, skip `git init`, and run `git remote -v` to check `origin`. Use `git remote set-url origin ...` if it exists with a different URL. GitHub requires you to authenticate to push; never paste access tokens in chat.

### IMPORTANT: debug builds vs. app updates

GitHub-hosted runners normally generate **new ephemeral debug signing keys**. Therefore APKs from different runs may be signed differently, causing Android to reject an update installed over an older APK (**APP_NOT_INSTALLED** / signature conflict). **Uninstalling the old APK would clear local favorites and settings.**

For reliable *in-place* updates that preserve favorites, you should use **one stable signing keystore** for every release:

1. Generate a private keystore **on your own computer** with Java's `keytool`. Example:
   ```bash
   keytool -genkeypair -v -keystore lokitunes-release.jks -storetype PKCS12 -alias lokitunes -keyalg RSA -keysize 3072 -validity 10000
   ```
   Choose strong passwords, and **back up the keystore and passwords privately**.
2. Base64-encode the **binary keystore** on your own PC (PowerShell):
   ```powershell
   [Convert]::ToBase64String([IO.File]::ReadAllBytes("lokitunes-release.jks")) | Set-Clipboard
   ```
3. GitHub repository → **Settings** → **Secrets and variables** → **Actions** → create these **repository secrets**:
   - `LOKITUNES_KEYSTORE_BASE64` = base64 text of keystore
   - `LOKITUNES_KEYSTORE_PASSWORD` = keystore password
   - `LOKITUNES_KEY_ALIAS` = `lokitunes` (or your chosen alias)
   - `LOKITUNES_KEY_PASSWORD` = private key password (PKCS12 usually uses the same as store password)
4. Re-run **LokiTunes Android Build**. In addition to debug, it will provide `LokiTunes-v0.1.0-signed-release` containing **`app-release.apk`**. Install **only signed releases from that keystore** and keep the signing key for future versions.

**Important:** A signed release cannot update a debug APK in place because signing keys differ. One-time uninstall/reinstall may be necessary when switching from debug testing to signed releases. Back up favorites before doing that (export is planned for a later version). **Never add your keystore / secrets to the public repository.**

## Android architecture

- Kotlin 2.3.21 + Jetpack Compose Material 3
- Android Gradle Plugin 8.13.2, Gradle 8.13, Java 17, compile SDK 36
- Compose BOM 2026.04.01
- Media3 ExoPlayer + `MediaSessionService` v1.11.1
- `MusicRepository` uses `ContentResolver` / `MediaStore`.
- `LokiViewModel` handles state + controller; `FavoritesStore` uses private app `SharedPreferences`.
- Entire app is offline: no cloud backend, Firebase, analytics SDK, ads, or INTERNET permission.
- Unit tests live in `app/src/test`.

## Inspect → Improve → Test → Fix

Current implementation was source-reviewed and has basic path-unit tests. **An Android SDK / Gradle environment was not available in the authoring container, so the code could not be compiled or run on a real device here.** The GitHub Actions build will be the first real Android compile + test check. If it fails, open the red job log in GitHub Actions, copy its **first error** back into this chat, and we will fix the actual cause before treating it as stable.

## Public releases / license

Repository source code is licensed under **MIT**. Each developer installing or forking the app is responsible for complying with licenses of any bundled assets. This repo **does not contain or distribute music files**.

To publish public APKs later: use a stable **signed release**, then create GitHub **Releases** and attach the APK, with changelog + version number.

Made with 💜 by LokiJ07.
