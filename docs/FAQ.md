# Frequently asked questions

### Is LokiTunes free?
Yes. The Android source is published under the MIT License and LokiTunes is offered without ads or subscription requirements.

### Does it work without Wi-Fi or mobile data?
Yes for local music playback, Favorites, playlists, and lyrics already saved on your device. Checking GitHub for updates and searching LRCLIB for lyrics requires internet access.

### Can LokiTunes play Spotify or YouTube streams?
No. LokiTunes is primarily an offline player for audio files available on your phone. It does not connect to a streaming service account.

### Does it read every music file on Android?
It reads indexed audio through Android MediaStore and lets you work with selected lyrics files/folders. Android can restrict access to some files or directories; a permission grant does not bypass protected system locations.

### Why are some artist names “Unknown artist”?
Song metadata may be missing or inconsistent in the downloaded audio file. LokiTunes generally shows metadata provided by Android's media index; it does not automatically rewrite music tags.

### What if my track has no album art?
LokiTunes uses the Midnight Aurora gradient artwork placeholder.

### How do I add lyrics?
On **Now Playing → Lyrics**, choose a local `.lrc` or `.txt` file, grant access to a music folder for filename matching, or perform an optional LRCLIB search and save a match locally.

### Are online lyrics guaranteed for every song?
No. LRCLIB may not have a matching result, and lyrics availability is subject to rights and service availability.

### Can I update without uninstalling?
Usually yes if both APKs are officially release-signed with the same signing key and the newer APK has a higher version code. Updates require your approval in Android's installer.

### Will I lose Favorites or playlists when updating?
Normal install-over updates generally retain app data. Uninstalling can erase locally saved data. Back up Favorites before a risky migration; a Favorites export is not a complete app backup.

### What is the difference between debug and release APKs?
Development builds from **GitHub Actions → Artifacts** use debug signing. Public APKs under **GitHub → Releases** use the permanent release key. Their signing identities may not match.

### Why doesn't the equalizer work on my device?
Equalizer/bass effects depend on the audio system and device support. Try another preset or disable the effect; core playback should continue to work.

### How do I report a bug?
Open a [GitHub Issue](https://github.com/LokiJ07/LokiTunes/issues/new/choose) and include your Android version, LokiTunes version, steps to reproduce, expected/actual results, and a screenshot if safe to share.
