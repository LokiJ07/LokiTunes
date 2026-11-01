# LokiTunes v1.0.0 — Regression Test Plan

**Do not call this build stable based on source checks alone.** Run the GitHub Actions tests, then validate on a real phone.

## Automated checks (GitHub Actions)

- [ ] `:app:testDebugUnitTest` succeeds (including `SigningGuardTest`).
- [ ] `:app:assembleDebug` succeeds.
- [ ] Signed APK is created on version-tag build; `apksigner verify --print-certs` passes.
- [ ] Tag `v1.0.0` exactly matches Gradle `versionName = "1.0.0"`.
- [ ] Published release contains `LokiTunes-v1.0.0-release.apk` (not debug APK).

## Real-device test (start from signed v0.9.0)

1. [ ] Confirm LokiTunes v0.9.0 is installed from GitHub **Releases**, not GitHub **Actions debug APK**.
2. [ ] Mark a new Favorite and create a test playlist with 2 songs. Save a Favorites JSON backup for safety.
3. [ ] After a green main-branch workflow, download the **signed release APK artifact** (not the debug APK) and manually install it over v0.9.0 on a test phone **before public tagging**.
4. [ ] Android installer accepts update *over* v0.9.0; no uninstall or package-conflict warning. After tagging/publishing, test the in-app download/install on a second signed-v0.9.0 installation or the following release.
5. [ ] App shows v1.0.0 / build 12 after reopen; Favorite and playlist are still present.
6. [ ] App starts with Wi-Fi disabled; MediaStore scan shows expected songs/folders; search and sort work.
7. [ ] Play/pause, seek, next/previous, queue editing, shuffle/repeat work.
8. [ ] Lock screen, headset unplug/Bluetooth and background playback behave normally; no unexpected audio after unplug.
9. [ ] Sleep timer pauses music at deadline; cancel stops timer.
10. [ ] Manual `.lrc` and local auto-match work; synced lyrics follow seek position; saved lyrics work offline.
11. [ ] Sound presets and bass controls do not crash on this phone (actual effect varies by device).
12. [ ] Updates screen says no newer stable version when v1.0.0 is latest.
13. [ ] Play music for 30+ minutes with screen off; watch for crash, overheating or unexpected battery drain.

## Signing compatibility negative test (development only)

- [ ] With a **non-production test device**, test a mismatched APK signed with a throwaway debug key: updater must reject before installer. Do not change production release signing secrets merely to test this.

## Known constraints

- Android scoped storage may hide protected folders; this is expected.
- LRCLIB content availability and the device's built-in audio effects are not guaranteed.
- This release does not migrate or back up every application setting; preserve Favorites JSON separately if needed.
- The updater's certificate preflight intentionally requires the **same current signing certificate**, so APK signing-key rotation is not supported by this updater yet. Android ultimately decides if an APK is installable.
