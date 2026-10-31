# LokiTunes v0.5.0 test plan — release gate

## Before tagging
- [ ] GitHub Actions `:app:testDebugUnitTest` GREEN (includes LRC parsing and speed preset tests).
- [ ] GitHub Actions `:app:assembleDebug` GREEN; no compile errors in `LokiApp.kt` or the new Kotlin files.
- [ ] `versionName = "0.5.0"`, `versionCode = 7`, and `applicationId = "com.lokij.lokitunes"` unchanged.
- [ ] `v0.5.0` Git tag points to the passing main-branch commit; release signed with the SAME keystore as v0.4.0.
- [ ] GitHub Release asset is exactly `LokiTunes-v0.5.0-release.apk`.

## In-app update regression
- [ ] Install signed update over signed v0.4.0 **without uninstall**.
- [ ] Favorites, queue behavior, last song, dynamic-color setting and other user data remain intact.
- [ ] Update dialog reads `v0.5.0 · Build 7`.
- [ ] Offline library scan, playback, lock-screen controls, Bluetooth, seek, repeat, shuffle and sleep timer still work.

## Lyrics
- [ ] Open a song → Now Playing → Lyrics. Without a selected file, the empty guidance is shown.
- [ ] Select `examples/demo-lyrics.lrc` copied to the phone; timed lines appear and highlight as playback progresses.
- [ ] Select a `.txt` file: plain scrolling lines, no timed highlight.
- [ ] Press Change file and Remove; no audio files are modified.
- [ ] Close/reopen app: chosen lyric document is remembered.
- [ ] Move/delete selected lyrics file: app shows an explanatory error, not a crash.
- [ ] Lyrics work with airplane mode ON.

## Sound
- [ ] Sound sheet shows 0.75× / 1× / 1.25× / 1.5× / 2×.
- [ ] Speed changes apply during playback, remain across song changes, and are remembered on relaunch.
- [ ] Music continues playing in the background at the chosen speed.
- [ ] Device equalizer opens when provided by device; otherwise app shows a nonfatal message.

## Notes
- UI and Android-specific integration have not been device-tested in the source packaging environment. GitHub Actions is the full compilation gate, phone testing is the behavior gate.
- Verify the existing signed update path rather than using debug APK for user upgrades.
