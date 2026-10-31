# LokiTunes 🎧 — v0.6.0 Smart Lyrics

Android 8+ offline-first music player. Midnight Aurora UI. Free open source. **Package: `com.lokij.lokitunes` — unchanged**. `versionCode = 8`, `versionName = "0.6.0"`.

## New: automatic offline lyrics matching

1. Put lyrics alongside the music, e.g. `Music/OPM/Example.mp3` and `Music/OPM/Example.lrc`.
2. In **Now Playing → Lyrics**, tap **Music folder** and choose `Music` (Android folder permission prompt).
3. LokiTunes remembers that folder and attempts exact filename matches for each song on the next Lyrics opening. Subfolders are checked for songs whose MediaStore folder paths are beneath the selected folder.
4. `.lrc` uses timestamps for synchronized highlighting. `.txt` is shown as plain text. Manual `.lrc/.txt` loading from v0.5.0 still works.

**Android 11+ scoped-storage restriction:** root storage and `Download` folder cannot be granted with the folder picker. Choose a specific allowed music folder or use **Local file** to select a single `.lrc` in Downloads. There is no broad `MANAGE_EXTERNAL_STORAGE` permission. This app only reads the folder you authorize. Local automatic matching never sends data online.

## New: optional online lyrics search

1. Now Playing → **Lyrics** → **Online**.
2. Review/edit song title and artist. Tap **Search lyrics**. Only these query terms are sent to `https://lrclib.net/api/search`.
3. Select the correct match (synced or plain) → **Save**. The lyrics are saved within LokiTunes app-private storage and work offline afterward.
4. Internet is not necessary for playback, normal lyrics viewing, or local matching. Lyrics search needs internet and depends on LRCLIB availability and rate limiting.

No internet search is performed in the background. No music files are uploaded. Lyrics text is never bundled in LokiTunes source. Credits: **LRCLIB** (user-selected lyrics search). Follow applicable lyrics rights.

## Signed GitHub release (no Android Studio needed)

Use the **existing LokiTunes signing keystore/secrets from v0.3.0**. Never regenerate release keys. Use the PATCH ZIP on top of the latest v0.5.0 Git project.

```bash
git add -A
git commit -m "feat: LokiTunes v0.6.0 Smart Lyrics"
git push origin main
```

Confirm **GitHub Actions build + unit tests are green**. Then tag the exact validated commit:

```bash
git tag v0.6.0
git push origin v0.6.0
```

Check signed release APK `LokiTunes-v0.6.0-release.apk` in GitHub Releases. Update through **LokiTunes → Updates** from signed v0.5.0. No uninstall needed when signing certificate and package ID match; Favorites should remain.

## Validation before publishing

Run `:app:testDebugUnitTest` and `:app:assembleDebug` on GitHub Actions, then install/test the signed release on a phone. Test Music folder granting, one-level and nested matches, manual file picker, remote search success/failure, downloaded lyrics without Wi-Fi, and existing music playback, Favorites, and updater. **No full Android Gradle build was run in the local packaging environment.**

## Privacy and safety

- No broad storage privileges, server account, ads or analytics.
- Optional online requests are user-triggered, to LRCLIB only.
- Songs never uploaded.
- Local cached lyrics are removed if the app is uninstalled.
- Signing passwords/keystores should NEVER be committed to public GitHub.
- MIT licensed.
