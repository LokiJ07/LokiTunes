# Changelog

## 0.7.0 — Smart Music Library (versionCode 9)

- Added private local custom playlists: create, rename, delete with confirmation, add/remove tracks, and ordered playlist playback.
- Added “Add to playlist” to song context menus throughout Songs, Home, Favorites, Folders, Albums/Artists and duplicate results.
- Added persisted Songs sorting: title, date added, artist, album and duration.
- Added album name to search matches.
- Added read-only potential duplicate groups based on title, artist and approximate duration. No automatic deletion or file modifications.
- Removed a repeated linear lookup on each click within the common track list by using `itemsIndexed`.
- Added playlist and library-insight JVM unit tests.
- Kept app ID, stable signing workflow, Favorites preferences, lyrics features, UI theme, and updater configuration unchanged.

## 0.6.0 — Smart Lyrics

- Automatic offline same-basename lyrics matching in a user-granted music folder tree.
- User-triggered LRCLIB search, choice of match, and app-private offline lyrics cache.
- Retained manual lyrics selection and synchronized playback highlighting.

For previous details see Git history/releases.
