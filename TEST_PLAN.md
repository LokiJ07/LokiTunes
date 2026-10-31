# LokiTunes 0.4.0 release gate

**Baseline:** Signed LokiTunes 0.3.2 installed; verify Favorites backup exists.

## Build gate
- [ ] Push patch to main; `:app:testDebugUnitTest :app:assembleDebug` GREEN.
- [ ] Check v0.4.0 / versionCode 6 / applicationId unchanged.
- [ ] Tag v0.4.0; signed build and GitHub Release publishing GREEN.
- [ ] Official release asset is `LokiTunes-v0.4.0-release.apk`; signing certificate unchanged from v0.3.2.

## Device gate
- [ ] Upgraded from signed v0.3.2 **without uninstalling**; Favorites retained.
- [ ] Device music found with internet OFF.
- [ ] Dynamic artwork color ON/OFF behaves and persists; fallback works for no-art songs.
- [ ] ±10-second seeking stays within media duration.
- [ ] Play Next and Add to Queue from song overflow menu with Shuffle OFF.
- [ ] Clearing upcoming preserves the current song, Favorites and audio files.
- [ ] Sleep timer, mini-player, background playback, notifications and headset controls still work.
- [ ] Updater identifies installed version v0.4.0 Build 6.

**Known behavior:** With Shuffle ON, visual queue position is not necessarily next in shuffle order. Switch Shuffle OFF for deterministic Play Next testing.
