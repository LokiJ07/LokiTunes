# LokiTunes public-launch checklist (v1.0.0)

## Upload these documentation files (no Android rebuild required)

- [ ] Replace the repository's `README.md` with the new README.
- [ ] Add `assets/screenshots/*.jpg` (9 user-supplied screenshots; confirm you're comfortable publicly sharing song names and library counts).
- [ ] Add `docs/PRIVACY.md`, `docs/INSTALLATION.md`, `docs/FAQ.md` and `docs/RELEASE_NOTES_v1.0.0.md`.
- [ ] Add `KNOWN_ISSUES.md`, `CONTRIBUTING.md`, issue templates and `docs/ANNOUNCEMENT.md`.
- [ ] Confirm the existing `LICENSE` file is the intended MIT license.

## GitHub public presentation

- [ ] Set the repo description to: `Free, open-source Android offline music player • Midnight Aurora UI • smart lyrics • playlists • Sound Studio • in-app updates`
- [ ] Suggested topics: `android`, `kotlin`, `jetpack-compose`, `music-player`, `offline-music`, `media3`, `open-source`.
- [ ] On the GitHub repo Code tab, verify all screenshot images render correctly.
- [ ] Open the **v1.0.0** release, select **Edit**, paste `docs/RELEASE_NOTES_v1.0.0.md` into the release description, and save. Do **not** retag or recreate the existing release just for documentation.
- [ ] Verify `LokiTunes-v1.0.0-release.apk` exists under release Assets and is the official signed APK (not debug).
- [ ] Check the release is public, not a draft or prerelease, and appears under `releases/latest`.

## Quality and safety

- [ ] Confirm the first public APK was built using the original, backed-up release signing key.
- [ ] Recheck Favorites and playlists after a signed in-place update; do not uninstall as a normal update method.
- [ ] Check Android 8.0+ requirement and permissions in the README are accurate.
- [ ] Review privacy policy after every change to remote services or data handling.
- [ ] Review known Now Playing track-title low-contrast issue; prioritize an accessibility fix in v1.0.1.
- [ ] Do not upload source archives containing `.jks`, `.keystore`, passwords, Base64 secrets or personal audio files.

## Git commands (from your existing LokiTunes local repo)

```bash
git status
git add README.md CONTRIBUTING.md KNOWN_ISSUES.md docs assets .github/ISSUE_TEMPLATE
git diff --cached --stat
git commit -m "docs: launch LokiTunes v1.0.0 public showcase"
git push origin main
```

**This is a documentation-only commit.** It does not need a new version tag, changes to APK signing, or reinstalling the app. Pushing will still trigger your repo's standard GitHub Actions main-branch job.
