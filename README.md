# LokiTunes 🎧 — v0.4.0 · Audio Experience

**Your music. Your rules.** Free, open-source, native Android offline music player, Midnight Aurora (black / purple / cyan), folder explorer, favorites and signed GitHub release in-app updates. Developed for Android 8+ (minSdk 26). App ID: `com.lokij.lokitunes`. MIT license.

## What's new in 0.4.0

- **Dynamic album colors:** Open **Now Playing**, tap the 🎨 palette icon to switch artwork-based background accent on/off. The selection is saved on your phone. If the song has no embedded art or it is grayscale, the original purple Midnight Aurora fallback is used. No artwork or color data is downloaded.
- **10-second rewind and fast-forward:** Buttons beneath the progress bar. Seeking is clamped to available duration.
- **Play next / Add to queue:** Three-dot menu on any song row; works from Home, Songs, Folder Explorer, Favorites, and Albums/Artists screens. Empty queue: item is queued without auto-playing. When Shuffle is enabled, queue order may differ from playback order.
- **Queue cleanup:** Now Playing → Queue → **Clear upcoming** removes tracks *after* the currently playing track. No audio files are deleted.
- Preserves background playback, device music discovery, Sleep Timer, Favorites JSON backup/restore, old folder views, and GitHub updater.

## IMPORTANT: Install as a normal in-app update (keep Favorites)

**Prerequisite:** Your existing installed LokiTunes should be the official **signed v0.3.2** from **GitHub → Releases**, not an Actions debug artifact. Because you already successfully updated from signed v0.3.1 → v0.3.2, the same private keystore and signing secrets can be reused. **Do not generate a new keystore, uninstall v0.3.2, change applicationId, or reset app data.** Make a Favorites → Backup JSON anyway if you want extra protection.

### Publish the update (no Android Studio required)

1. Extract **LokiTunes_v0.4.0_AudioExperience_PATCH.zip**. Copy/merge its contents into the root of your existing GitHub working folder; keep the subfolder paths unchanged.
2. From the repository root, use Git Bash:

```bash
git add -A
git commit -m "feat: LokiTunes v0.4.0 audio experience"
git push origin main
```

3. Open **GitHub → Actions**. The **main branch** build must be green (unit tests + APK compilation).
4. Before tagging, verify `app/build.gradle.kts` says `versionName = "0.4.0"` and `versionCode = 6`. Tag only the tested commit:

```bash
git tag v0.4.0
git push origin v0.4.0
```

5. In **GitHub → Actions**, verify the **tag build AND Publish signed public GitHub Release** jobs are both green. In **GitHub → Releases** verify the signed asset is **`LokiTunes-v0.4.0-release.apk`**.
6. **On your phone, keep v0.3.2 installed**. Connect to the internet, open LokiTunes → Updates → Download update → Install update, and approve the Android installer. Confirm the Updates dialog says **Installed version: v0.4.0 · Build 6**. Favorites and playback should remain intact. Once installed, music works offline as before.

**Package conflict?** Do not uninstall. Check the APK was downloaded from **Releases**, not **Actions / debug artifacts**, and verify the SHA-256 signing certificate of both versions matches. The signing key must be the original keystore used for v0.3.2.

## GitHub Actions / Release notes

- Automatic `gradle :app:testDebugUnitTest :app:assembleDebug` on pushes and PRs.
- On version tags, validates `versionName` against the tag, requires 4 GitHub signing secrets, signs and verifies the release APK, and publishes to GitHub Releases.
- **Keep the same secrets**: `LOKITUNES_KEYSTORE_BASE64`, `LOKITUNES_KEYSTORE_PASSWORD`, `LOKITUNES_KEY_ALIAS`, `LOKITUNES_KEY_PASSWORD`.
- Protect your keystore and passwords; never add them to this public repository. Neither the patch nor the full source includes a private key.
- Debug APK artifacts are development-only and should not be used for public update installations.

## Manual test plan for v0.4.0

1. Launch LokiTunes with mobile data off: music library loads and songs play normally.
2. Play a song **with** embedded artwork: open Now Playing → top gradient adapts; toggle palette icon off/on; relaunch app and confirm preference stays.
3. Play a song **without** album art: original Midnight Aurora gradient appears; no crash or network request.
4. Use ±10-second buttons near song start/end; seek never becomes negative or exceeds track duration.
5. While a song is playing, open a song's three-dot menu → Play next → go to Queue and inspect its position. Repeat with Add to queue. Try with Shuffle **off** for deterministic order.
6. Now Playing → Queue → Clear upcoming; current track continues to play, previously played/current entry remains.
7. Check Sleep Timer, Favorites (including sorting and backup/restore), mini-player, Folders, Background/lock-screen playback, and next/previous.
8. Confirm installed **v0.4.0 · Build 6** and saved Favorites survive the in-app update from signed v0.3.2.

## Repository layout

- `app/src/main/java/.../ui/LokiApp.kt` — main Compose UI / mini-player / update dialog / queue / Now Playing
- `app/src/main/java/.../ui/LokiViewModel.kt` — media controller, queue edits, local preferences
- `app/src/main/java/.../ui/ArtworkPalette.kt` + `ArtworkAccentSelector.kt` — local embedded-art colors
- `app/src/main/java/.../ui/QueueRules.kt` — pure queue / seeking logic
- `app/src/test/...` — JUnit tests including v0.4.0 regression rules
- `.github/workflows/android.yml` — signed APK build and release workflow

## Status & limits

Source review, source archive validation, and local pure-Kotlin smoke tests have been performed, but a full Android SDK build and actual device test **must be run via GitHub Actions and your phone**. Dynamic album colors depend on the file actually containing embedded artwork. Some Android MediaStore audio locations may be restricted by system permissions. The equalizer and lyrics are not yet implemented.
