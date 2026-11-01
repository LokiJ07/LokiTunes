# LokiTunes changelog

## v0.8.0 — Premium Audio (build 10)

- New built-in **Sound Studio** in Now Playing → Sound.
- Five-point EQ curve with Flat, Bass, Vocal, Pop, Rock, and Custom profiles.
- Adjustable bass boost (0–100%); off by default.
- Custom EQ supports -6 to +6 dB at 60 Hz / 230 Hz / 910 Hz / 4 kHz / 14 kHz reference bands. The Android audio framework may expose fewer/more physical bands; output uses log-frequency interpolation and clamps to device-supported gain range.
- Effects are attached only to LokiTunes' active Media3 audio session inside the playback service (not the system-wide mix), released at shutdown, and rebound if the session ID changes.
- Settings persisted using the existing `lokitunes_settings` preferences; audio writes are coalesced to reduce UI/audio-thread churn when dragging sliders.
- Existing playback speed presets, optional system EQ shortcut, lyrics, playlists, Favorites, and GitHub updater retained.
- Package stays `com.lokij.lokitunes`, versionCode increases from 9 to 10; release signing flow unchanged.

### Important limitations

- Audio effects depend on Android device implementation and processing output route. Some phones may not support EQ or bass boost, or may have OEM limitations. The feature does not alter or upload music files.
- Boosting multiple frequencies can cause distortion or excessive loudness. Start low and keep volume comfortable.
- Full Android compilation and real-device audio tests remain required before tagging a public release.
