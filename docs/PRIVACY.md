# LokiTunes Privacy Policy

**Effective date: November 1, 2026**  
**Applies to:** LokiTunes for Android, v1.0.0  
**Publisher:** LokiJ07 (independent, open-source developer)  
**Project:** https://github.com/LokiJ07/LokiTunes

## Short version

LokiTunes is an **offline-first** music player. It does not require an account, show advertisements, or upload your music collection to a developer server. Your music is read from the Android device, and most app data is stored locally.

**LokiTunes can use the internet:** it checks public GitHub Releases for updates, including a check when the app launches, and offers optional lyrics searches through LRCLIB when you request them. Like other internet services, GitHub and LRCLIB receive ordinary request and connection information, and may apply their own privacy policies.

## What LokiTunes accesses and why

| Data or permission | Reason | Processing / storage |
|---|---|---|
| Audio files and metadata available through Android MediaStore | Discover and play tracks; display title, artist, albums, durations and folders. | Accessed on the device. Audio files are not uploaded by LokiTunes. |
| Audio-library permission (`READ_MEDIA_AUDIO` on supported Android versions, or `READ_EXTERNAL_STORAGE` on older supported versions) | Show music already on the device. | Android permission can be revoked in system settings. |
| Selected lyrics file/folder access | Read `.lrc` / `.txt` lyrics, optionally auto-match files. | User-granted file/folder access through Android's document picker; access can be revoked. |
| Favorites, playlists and playback preferences | Remember library organization and sound settings. | Stored in app-private local storage. |
| Saved lyrics | Make lyrics available offline after importing or saving. | Stored in app-private local storage. |
| Favorites backup file | Export or restore favorite track references. | Saved only when the user chooses a destination; the export is not an MP3 backup. |
| Foreground media playback service | Keep playback available while the app is not in the foreground. | Runs locally while needed for audio playback. |
| APK installation permission (`REQUEST_INSTALL_PACKAGES`) | Hand a downloaded GitHub release APK to Android's installer. | Android must ask the user to approve installation. There is no silent install. |

## Network requests and third parties

### GitHub — updates

LokiTunes can request the latest public release metadata from `api.github.com` when the app starts or the user checks for updates. If the user chooses to download an update, it downloads the official release APK from GitHub. These requests may reveal ordinary network metadata, such as an IP address and user agent, to GitHub. LokiTunes does not attach local music files to these requests.

### LRCLIB — optional online lyrics

Only after the user starts an online lyrics search, LokiTunes sends a requested **song title** and, if present, the **artist name** to `lrclib.net` to look for lyrics. It does **not** upload the song's audio file. Search results may be saved locally at the user's choice. LRCLIB controls its own service and retention practices.

### No other intended online features in v1.0.0

The v1.0.0 source reviewed for this policy does not include developer-operated analytics, ad SDKs, cloud backups, or login services. Internet access exists for the purposes described above. This policy should be updated if later versions add new network features or third-party services.

## Storage, retention and deletion

- Saved Favorites, playlists, preferences and lyrics generally remain on the device until removed or the app is uninstalled.
- **Uninstalling may delete app-private data**, including local playlists and cached lyrics. LokiTunes's Favorites export/restore does not back up every app setting or your music files.
- Favorites backups saved to another folder remain there until you delete them using your device's file manager.
- Normal signed in-place updates usually preserve app data. Changing signing keys or uninstalling can interrupt that continuity.
- To revoke music access, open **Android Settings → Apps → LokiTunes → Permissions**. Selected folder access may also be managed in device storage permission settings, where supported.

## Children and accounts

No user account or profile creation is required. LokiTunes is a general-purpose local audio player; parents or guardians should supervise children's use of downloaded audio and third-party lyrics content as appropriate.

## Security and third-party links

LokiTunes attempts to verify downloaded release files (including checksum and signing compatibility) before handing installation to Android. Android performs its own installation and signature checks. Security protections can change in future versions; there is no guarantee that software is free from every vulnerability.

External GitHub and LRCLIB sites are governed by their own terms and privacy practices.

## Questions and updates to this policy

To ask a privacy question or report an issue, open an issue at https://github.com/LokiJ07/LokiTunes/issues . **Do not publish passwords, private keys, personal files or sensitive information in a public issue.**

Material privacy changes in future versions should be reflected here with an updated effective date.
