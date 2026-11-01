# 🎧 LokiTunes v1.0.0 — First Public Release

**Your Music. Your Rules.** 💜🩵

LokiTunes is a free, open-source, offline-first Android music player. The first public version brings together your local song library, folder browsing, favorites, playlists, synchronized lyrics, audio controls and a polished Midnight Aurora design.

## ✨ Highlights

- **Music you already own:** scan and play locally stored audio files without an account, ads or streaming subscription.
- **Library your way:** browse Songs, Folders, Albums, Artists and custom Playlists; search and sort your collection.
- **Favorites and queue:** heart your tracks, edit the play queue, use shuffle/repeat, and set a sleep timer.
- **Smart Lyrics:** use `.lrc` and `.txt` files offline, auto-match accessible local files, or search LRCLIB when you choose and save results for offline use.
- **Sound Studio:** audio presets, adjustable bands, bass boost and playback speed (device support may vary).
- **Midnight Aurora UI:** purple/cyan accents, dynamic album colors and an artwork-free fallback.
- **Background playback:** continue listening outside the app with supported lock-screen and media controls.
- **Signed in-app updates:** the updater checks GitHub Releases, verifies a candidate APK, and asks Android to install it with your approval.

## 🛡️ v1.0.0 stability work

- Added a signing-certificate compatibility check before the APK installer is launched.
- Preserved the package ID `com.lokij.lokitunes` and existing Favorites, playlist and lyrics storage identifiers.
- Included regression tests and a public release checklist.

## 📥 Download

**Android 8.0+ is required.** Download the **official signed** `LokiTunes-v1.0.0-release.apk` under **Assets** on this page. **Do not use GitHub Actions debug APK artifacts** for normal installs/updates.

For an existing signed LokiTunes installation, use **Updates → Download → Install**; don't uninstall the app first. Android may ask you to authorize installation from this source.

## 🔒 Privacy

LokiTunes does not require an account or upload music files. Music playback is offline. The app contacts GitHub for update checks/downloads and contacts LRCLIB only when you request an online lyrics search. Those services may receive standard request metadata. See [Privacy Policy](https://github.com/LokiJ07/LokiTunes/blob/main/docs/PRIVACY.md).

## 💬 Feedback

This version has been tried on the developer's Android device, but broader hardware compatibility is still being evaluated. Please share reproducible issues or feature requests in [GitHub Issues](https://github.com/LokiJ07/LokiTunes/issues). Some Android audio effects and folder access vary by device.

**Thank you for checking out LokiTunes! 🎶**

**Full project & screenshots:** https://github.com/LokiJ07/LokiTunes
