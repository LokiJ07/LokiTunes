# Install and update LokiTunes on Android

**Official releases:** https://github.com/LokiJ07/LokiTunes/releases/latest  
**Minimum Android version:** Android 8.0 (API 26)

## Fresh installation

1. Visit the official GitHub Releases page from your Android phone.
2. Expand **Assets** on the newest published release.
3. Download **`LokiTunes-vX.Y.Z-release.apk`**, where `X.Y.Z` is the release version.
4. Open the downloaded APK. Android may prompt you to allow installation from your browser or file manager. Approve only if you trust the downloaded official release.
5. Confirm installation and open LokiTunes.
6. Grant the requested Android audio access so the app can find your music.

> **Important:** Download the **release-signed APK** from the Releases page, **not** a `LokiTunes-dev-debug-apk` artifact from Actions. A debug APK and public release APK may have different signing certificates and cannot normally update each other in place.

## Using LokiTunes offline

Your already-downloaded music stays on the phone. Music scanning, playback, playlist browsing and saved local lyrics work without internet. Network access is only needed for **optional LRCLIB online lyrics** and **update checks/downloads**. The app checks for new public updates at startup when internet is available.

Some music or lyrics in protected directories may not appear due to Android's storage restrictions. Use the music scanning and document-picker options offered in the app.

## Update from one signed release to another

1. Keep LokiTunes installed. **Do not uninstall it.**
2. Open LokiTunes and tap the **Updates** icon in the top bar.
3. If a newer stable release is available, select **Download Update**.
4. When download completes, tap **Install Update** and approve Android's installation prompt.
5. Reopen LokiTunes. Check the installed version and confirm Favorites, playlists and playback still work.

Android in-place upgrades require the **same package ID and compatible signing identity** plus a higher `versionCode`. LokiTunes uses the package ID `com.lokij.lokitunes`. The publisher must keep the original signing key safely backed up for future releases.

## If Android says “package conflicts with an existing package”

The most common cause for this project is that the currently installed version was a **debug APK** while the new download is a **release APK**, or an APK was signed with a different key.

- **First identify your installed APK source**. Avoid uninstalling if you have Favorites or playlists you want to preserve.
- If you installed the Actions **debug** APK and want to switch to signed Releases, export your Favorites first, check that the backup file exists, and understand that uninstalling may erase other local data that the Favorites export does not include. Install the signed release after the backup.
- If the old APK was **already an official signed release**, do **not** uninstall or generate a new key. Report the problem, including both version numbers, so the signing identity can be checked.

## Keep your data safe

- Favorite songs can be exported/imported in the Favorites tab, but the JSON contains song references—not the actual audio files.
- The built-in Favorites backup does **not** necessarily include custom playlists or lyrics cache.
- Store your music files in a device folder you control and back them up separately.
- Device behavior for Bluetooth, notifications and audio effects varies by manufacturer.

## Troubleshooting

| Problem | Suggested check |
|---|---|
| No tracks visible | Check Android audio permission; refresh the library; ensure the files are local, accessible audio files. |
| Some folders missing | Android scoped storage may block access to protected directories. |
| Lyrics missing | Choose a matching `.lrc`/`.txt` file or grant access to the music/lyrics folder; optionally search online. |
| Equalizer unavailable | Device/OS might not expose supported audio effects. Playback should still work. |
| Update download fails | Connect to the internet and retry; confirm GitHub Releases contains a matching official signed APK. |
| Update can't install | Check whether the old APK was debug-signed; preserve your data before taking action. |

Open a reproducible bug report at https://github.com/LokiJ07/LokiTunes/issues .
