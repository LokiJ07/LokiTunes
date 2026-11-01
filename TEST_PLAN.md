# LokiTunes v0.8.0 — real-device QA checklist

## A. Compilation and release safety

- [ ] `:app:testDebugUnitTest` passes on GitHub Actions.
- [ ] `:app:assembleDebug` and `:app:assembleRelease` pass.
- [ ] GitHub release signing shows the **same certificate fingerprint** as previous signed LokiTunes releases.
- [ ] Release APK is named `LokiTunes-v0.8.0-release.apk` and versionCode is 10.
- [ ] In-app update from signed v0.7.0 works without uninstalling or losing Favorites or playlists.

## B. Sound Studio

- [ ] Open Now Playing → Sound; EQ starts disabled (unless previously enabled).
- [ ] Enable EQ, select Bass / Vocal / Rock / Pop / Flat while music plays.
- [ ] Adjust any band, ensure preset changes to Custom and slider retains selected value.
- [ ] Test bass boost 0 / 25 / 50; avoid extreme output or high volume.
- [ ] Disable EQ and verify the audio returns toward unprocessed sound.
- [ ] Restart the app and verify preferences persist.
- [ ] Compare built-in speaker, wired headphones (if available), and Bluetooth headset.
- [ ] Pause/resume, skip, background playback, lock phone and unlock; verify no crash or stuck effects.
- [ ] Audio should still play on devices where the EQ / BassBoost implementations are unsupported; audio improvement is device dependent.
- [ ] With no audio playing, toggling EQ must not crash the app.

## C. Regression

- [ ] Song scanning, folder navigation, favorites, playlist creation/playback and duplicate list work.
- [ ] Local lyrics / online lyrics cache still works (if configured).
- [ ] Sleep timer, shuffle, repeat, queue, and playback speed still work.
- [ ] Check CPU/battery or audible glitches while rapidly moving EQ sliders.
- [ ] Saved favorites and playlist data remain through signed update.

## Limitations

Local tests include pure Kotlin audio tuning only; Android Equalizer/BassBoost functionality requires live devices with compatible audio effect engines. Avoid high volume during EQ testing.
