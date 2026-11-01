# LokiTunes 🎧 — v1.0.0 Stable Candidate

**Your Music. Your Rules.** Free, open-source, offline-first Android music player with the Midnight Aurora purple/cyan theme. Android 8.0+ (`minSdk 26`). No login, ads, or subscriptions.

> **Release status:** Source candidate prepared. Only call the release stable after GitHub Actions passes and the on-device smoke tests in `TEST_PLAN.md` pass. Do not tag until then.

## Main features

- Device audio via Android MediaStore; Songs, folder browsing, Albums and Artists.
- Favorites and local custom playlists, sorting, duplicate suggestions (no deletions).
- Media3 background playback, queue, shuffle/repeat, lock-screen/Bluetooth controls, sleep timer.
- Offline `.lrc`/`.txt` lyrics; optional online lyrics search through LRCLIB when the user requests it; saved lyrics work offline.
- Sound Studio: device-dependent equalizer / bass effect, speed presets and dynamic album-art colors.
- Signed GitHub Releases updater (requires internet for checking/downloading updates, but music remains offline).

## Improvements for v1.0.0

- Production version metadata: `1.0.0`, `versionCode = 12` (up from v0.9.0 build 11).
- **Signing compatibility preflight:** before opening the Android installer, the updater compares the installed app's signing certificate with the downloaded APK's certificate. On a mismatch, it refuses installation and shows a useful error message rather than proceeding to a package-conflict dialog. Same-key updates behave as before. Android is still the final authority on signature validity.
- Added regression unit tests for same-key, wrong-key and missing-signature cases.
- Documented quality gates, privacy, and signing-key recovery expectations for the first stable release.

No new runtime permissions, no changed `applicationId`, no migration of Favorites, playlist or lyrics storage, and no changes to the playback engine in this release.

## Develop, build and publish (GitHub-only workflow)

1. Extract the **PATCH ZIP** into the root of the existing GitHub repository, retaining directory structure.
2. Push normally:

```bash
git add -A
git commit -m "release: prepare LokiTunes v1.0.0 stability update"
git push origin main
```

3. Confirm the **main branch GitHub Actions job is green**, including unit tests and debug build. The existing workflow also produces a **signed release APK artifact on main** when signing secrets are configured. Use **that signed artifact** (not the debug artifact) on a test phone to verify install-over-update before public tagging.
4. **Confirm real device results** using [`TEST_PLAN.md`](TEST_PLAN.md), including Favorites/playlists retention. Fix regressions before public tagging. To separately test the *in-app download path* after publishing, keep another device on signed v0.9.0 or test the next signed update.
5. When verified, publish **with the original signing key** stored in the existing `LOKITUNES_*` GitHub Actions secrets:

```bash
git tag v1.0.0
git push origin v1.0.0
```

6. Check that the **Publish signed public GitHub Release** job passes and contains `LokiTunes-v1.0.0-release.apk`.
7. From the **installed, signed v0.9.0 release**, use LokiTunes → Updates → Download → Install. **Do not uninstall.** Verify Favorites and playlists remain intact.

If the certificate preflight says the signing key changed, do **not** generate a new keystore or uninstall. Restore the **original** signing secret/keystore and re-release a properly signed APK after diagnosis.

## Privacy and data

- Playback, audio library scanning, playlists, favorites and saved lyrics are stored/handled on device.
- Network access is used for optional GitHub update checks and on-demand LRCLIB lyrics search; online requests reveal the song title and artist to the third-party lyrics service.
- Imported/exported Favorites JSON is metadata only, not MP3 files. App uninstall can remove private local data; keep your exports if important.
- Some folders/files cannot be read due to Android scoped-storage restrictions. No automatic download of music.

See [TEST_PLAN.md](TEST_PLAN.md) and [RELEASE_CHECKLIST.md](RELEASE_CHECKLIST.md). Source is licensed under MIT (`LICENSE`). Do not commit `.jks`, `.keystore`, passwords, or Base64 secrets.
