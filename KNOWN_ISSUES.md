# Known Issues and limitations — v1.0.0

These items help set accurate expectations for the first public release. A green build and a working installation on one device do **not** establish compatibility with every Android phone.

- **Now Playing text contrast:** In a provided v1.0.0 screenshot, the track title renders in a very dark color against a dark background and is difficult to read. A targeted contrast/accessibility fix is recommended for v1.0.1. This issue is visible in the screenshot rather than verified across devices.
- **Audio metadata:** Downloaded files without tags may show filename-based titles or `Unknown artist`.
- **Android storage restrictions:** Some device directories are inaccessible, and some audio may not be indexed by MediaStore.
- **Audio effects:** Equalizer and bass boost availability/behavior vary across Android devices.
- **Lyrics:** LRCLIB cannot guarantee a match for every track; local matching requires access to selected files/folders.
- **Local app data:** Uninstalling may remove Favorites, playlists and saved lyric cache. Favorites export does not include complete app data or MP3 files.
- **APK signing:** Debug and release builds may be incompatible for install-over-updates. The original release signing key is required for subsequent public updates.

If you notice additional issues, open a [bug report](https://github.com/LokiJ07/LokiTunes/issues/new/choose) with version, Android model/OS, and reproduction steps. Avoid posting personal music files or sensitive data publicly.
