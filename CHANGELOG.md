# LokiTunes release notes

## v0.6.0 — Smart Lyrics

- **Offline auto-match** of `.lrc` and `.txt` in a user-approved folder tree; exact audio filename or song-title basename, case-insensitive.
- **Optional LRCLIB search** — manual action only, edit title/artist, inspect results and select a match, save lyrics to private local cache. No audio upload.
- **Offline replay** of downloaded lyrics, synced highlighting with existing LRC parser.
- **Manual local selection retained** from v0.5.0, plus ability to remove folder access.
- Version **0.6.0**, build **8**; preserves `com.lokij.lokitunes`, Favorites, playback, equalizer shortcut and signed in-app updater.

## Known limitations

- Automatic local lookup is scoped to **one folder that the user chooses** (plus matching subdirectories), not all protected Android storage. Android 11+ does not allow tree grants to the storage root or Downloads root. For Downloads, choose individual lyric documents manually or move them into Music.
- LRCLIB may lack or misidentify songs: always select the correct result; online requests need internet. Results may be rate-limited.
- Offline cached lyrics live in app-private storage; uninstalling the app removes them.
- Copyrighted lyrics are not included in this repository or APK.
