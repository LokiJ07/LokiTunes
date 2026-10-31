# LokiTunes 🎧 — v0.3.0 (GitHub Releases + in-app updater)

**Your music. Your rules.** Native Android offline music player with Midnight Aurora UI, folder music navigation, favorites, play queue and sleep timer. Free/open-source (MIT). App ID `com.lokij.lokitunes`, minimum API 26 (Android 8).

## What's new in 0.3.0

- Small **Updates** button in the app header, with a colored badge when a newer stable GitHub Release is available.
- Silent *update check* at app launch when internet is available; failure to connect is ignored and **offline music playback works as before**. Tap the button for manual checks.
- **Download update** inside the app, with progress, published GitHub asset SHA-256 verification, APK package/version verification, and a standard **Android install confirmation**. No background forced installation.
- GitHub Actions builds/tests on push and **automatically publishes a signed release APK for a `vX.Y.Z` tag** (only after four signing secrets are configured).
- All 0.2.0 audio, queue, Favorites backup/restore and UI functions are retained.

> **Music remains offline.** Internet access is needed only when checking or downloading an app update. Nothing sends your audio, favorites, listening history, or file list to a server.

## IMPORTANT: Before upgrading an existing debug install

The user's earlier GitHub Actions APK (`app-debug.apk`) was likely signed with a **temporary runner debug key**. Android will not install a stable release signed by a different key on top of it. **The first move from debug to a stable signed release can require uninstalling the debug app, which erases app data.**

If you have v0.2.0 installed, go to **Favorites → Backup** first and save the JSON file outside the app. After installing the signed v0.3.0 release, restore via **Favorites → Restore**. If your currently installed app is v0.1.0 without backup/restore, save favorite titles manually before uninstalling. Music files in shared device storage are not deleted by uninstalling LokiTunes. Never uninstall before securing the Favorites you want to keep.

Once you start using a stable signed APK, **keep the SAME private keystore, package ID, and increasing versionCode for every subsequent release**. Then the in-app updater can offer normal install-over-the-old-app updates with preserved data. If Android says "App not installed", do not uninstall first without a backup.

## Step 1 — Upload this project (no Android Studio needed)

Apply the PATCH ZIP over the existing `LokiTunes` repository, preserving paths and `.github/workflows/android.yml`, then:

```bash
git add -A
git commit -m "feat: LokiTunes v0.3.0 in-app updates and signed releases"
git push origin main
```

Open **GitHub → Actions** and confirm the push build is green. A passing Kotlin compilation and unit tests is required before release.

## Step 2 — Generate your OWN permanent release key

Create and permanently back up this key **on a computer you control**. Do not paste the password or keystore into ChatGPT or a public GitHub issue. You need a JDK with `keytool` available (a JDK can be installed without Android Studio).

Example (Git Bash or terminal, change the passwords when prompted):

```bash
keytool -genkeypair -v -keystore lokitunes-release.jks -alias lokitunes -keyalg RSA -keysize 3072 -validity 10000
```

Keep `lokitunes-release.jks` somewhere safe **outside the Git repo** and make a secure offline backup. If you lose this private key, you cannot normally update existing installed releases without requiring reinstall.

In Windows PowerShell, copy its base64 to your clipboard (change the path to your actual file):

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("C:\path\to\lokitunes-release.jks")) | Set-Clipboard
```

In GitHub go to **Repository → Settings → Secrets and variables → Actions → New repository secret** and create these *exactly*:

| Secret | Value |
|---|---|
| `LOKITUNES_KEYSTORE_BASE64` | Base64 string from PowerShell clipboard |
| `LOKITUNES_KEYSTORE_PASSWORD` | Password you set for `.jks` |
| `LOKITUNES_KEY_ALIAS` | `lokitunes` (or your chosen alias) |
| `LOKITUNES_KEY_PASSWORD` | Password of the signing key |

Never commit `.jks`, `.keystore`, `.p12`, or passwords. `.gitignore` protects common signing filenames, but you should still check `git status` before pushing. Avoid exposing signing key in screenshots or logs.

## Step 3 — Publish v0.3.0 publicly on GitHub Releases

After a **green main build** and all four secrets are set:

```bash
git tag v0.3.0
git push origin v0.3.0
```

The tag workflow runs tests, checks that the tag matches the app versionName, signs and verifies a **release** APK, then uses GitHub's built-in `GITHUB_TOKEN` via `gh release create` to publish:

`LokiTunes-v0.3.0-release.apk`

Users can download that file from **GitHub → Releases**. This is public distribution **on GitHub**, not a Google Play Store listing.

**Do not publish `app-debug.apk` as the public update package.** The automatic updater intentionally ignores debug artifacts and only recognizes signed GitHub Release assets with matching tag names. It also requires GitHub to report the uploaded asset SHA-256 digest before download/install. It may take a moment for the Releases API to include a freshly published asset digest.

If the `publish` job gets a 403, check **Repository → Settings → Actions → General → Workflow permissions** and allow read/write workflow token permissions where your GitHub policy permits, then rerun. The workflow already requests `contents: write` only for the publish job.

## Step 4 — Verify your updater works (test the NEXT release)

1. Install the **signed** v0.3.0 Release APK (after safely handling prior debug Favorites).
2. Open LokiTunes; verify music and Favorites. Tap the new update icon: it should say you're up to date (v0.3.0 is the latest public release).
3. For the next release, update `versionCode` to **4** and `versionName` to `"0.3.1"` in `app/build.gradle.kts`, commit/push, then push `v0.3.1` tag.
4. Open v0.3.0: a notification dialog should offer v0.3.1; select **Download update → Install update** and confirm Android's installer. Android 8+ may request **Allow from this source** on first install: enable it, return to LokiTunes, and tap Install again.
5. Confirm new version is installed **over** v0.3.0 with Favorites remaining. Confirm offline audio still works with Wi-Fi/data off.

## Limits and safety

- Checks only `https://api.github.com/repos/LokiJ07/LokiTunes/releases/latest` (public, non-draft, non-prerelease) and expects a named asset `LokiTunes-vX.Y.Z-release.apk` in its GitHub Release.
- Rejects unexpected asset name/URL, missing SHA-256 digest, checksum mismatch, wrong package name, not-newer APK versionCode, or APK larger than 150 MB. Android's installer independently validates the signing certificate for installed upgrades.
- Update checks do not run continuously, or when the app is closed. The downloader runs while the app is in use; Android may stop it if the process is killed. Reopening and retrying is safe.
- Android requires approval for package installation. For non-Play distribution, the app declares `REQUEST_INSTALL_PACKAGES`. **Do not submit this implementation unchanged to Google Play**: the Play policy restricts use of this permission for self-updates. For Play distribution, use Google Play In-App Updates instead.
- If your GitHub repository owner/name changes, update the constants in `app/src/main/java/com/lokij/lokitunes/update/UpdateRepository.kt` and the "View releases" URL in `ui/LokiApp.kt`.

## Inspect → Improve → Test → Fix

The existing v0.2.0 Material3 opt-in fix was retained. Manifest / GitHub Actions YAML / APK naming and source structure were statically checked, and `VersionNumberTest` covers semantic version comparisons. **Android SDK/Gradle compilation and real-device updater testing are still required via GitHub Actions and the phone**. A green source build does not establish that published APK installation works; test with the second signed release as described above.
