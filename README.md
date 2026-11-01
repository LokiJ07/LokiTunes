# LokiTunes 🎧 — v0.8.0 Premium Audio

Midnight Aurora • Free • Open source • Offline-first Android music player. **Android 8+** (minSdk 26).

## New: Sound Studio

1. Play any song and open **Now Playing → Sound**.
2. Toggle **Enable equalizer** (off by default for safety and compatibility).
3. Select **Flat / Bass / Vocal / Pop / Rock / Custom**.
4. Adjust one of the five sliders (60 Hz, 230 Hz, 910 Hz, 4 kHz, 14 kHz) to switch into **Custom** mode. Values are limited to ±6 dB. Available physical equalizer bands are determined by Android; LokiTunes maps the five-point curve to each supported band and clamps within its advertised gain range.
5. Adjust **Bass boost** from 0 to 100% if your phone supports it.
6. Playback-speed controls and the **Open device equalizer** optional shortcut remain available.

Settings stay on your phone and persist through normal signed application updates. Nothing here needs an account or internet. **Audio engine remains functional if hardware EQ is unavailable.** Android devices differ in effect support; some headphones, audio routes, and phones may not apply effects. Start with low boost levels to avoid distortion.

## Release information

- Android application ID: `com.lokij.lokitunes` (**unchanged**)
- Version name: `0.8.0`, versionCode: `10`
- GitHub Actions build & signed release pipeline: **preserved**
- Same existing LokiTunes signing keystore and `LOKITUNES_*` secrets: **required for install-over update**
- No keystores or secrets included in the ZIP

### Updating your GitHub repository

Apply **PATCH ZIP** to the existing v0.7.0 project root (so e.g. `app/src/...` overwrites the existing files, not a nested LokiTunes folder).

```bash
git add -A
git commit -m "feat: LokiTunes v0.8.0 Premium Audio"
git push origin main
```

Verify **GitHub → Actions** has a green run for the commit, including Gradle unit tests and Android debug compile. Only then make the version tag:

```bash
git tag v0.8.0
git push origin v0.8.0
```

Verify **GitHub → Releases** has `LokiTunes-v0.8.0-release.apk` produced by a **signed** release workflow. Update through **LokiTunes → Updates → Download → Install**; do not uninstall your existing signed app or install the Actions debug APK.

### Test on your Android device

See [TEST_PLAN.md](TEST_PLAN.md). In particular, compare the same track with EQ off vs Bass/Vocal, then toggle on/off during playback and while playing in background. Check Bluetooth earbuds and speaker independently. Check Favorites, playlists, lyrics, and in-app updates remain intact.

## Engineering & validation

- Source architecture was inspected against v0.7.0, including the Media3 session service, current Sound sheet, and GitHub signing workflow.
- New DSP settings model compiled locally using Kotlin; **19 smoke assertions passed** for presets, storage encoding, frequency interpolation, gain clamping, and bass limits.
- JUnit tests were added to the Android project and will run during GitHub Actions `:app:testDebugUnitTest`.
- Manifest/XML and workflow YAML syntax can be checked statically, but full Android SDK/Compose APK compilation is **not available locally**. The actual GitHub build and your phone determine whether release is ready.

## Privacy & existing functions

Favorites, playlists, Smart Lyrics, song sorting, folder explorer, playback speed, sleep timer, and signed updater are preserved. Normal music playback stays offline. The optional LRCLIB lyrics search and GitHub updates still require internet when you use them. `MIT` license.
