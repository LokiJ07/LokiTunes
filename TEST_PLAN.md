# v0.6.0 release and smoke-test checklist

1. Validate GitHub Actions `testDebugUnitTest` and `assembleDebug`.
2. Ensure signed v0.5.0 is installed. Confirm Favorites exist.
3. Add `Music/OPM/Example.mp3` and `Music/OPM/Example.lrc`, launch Lyrics and grant **Music** tree once. Check auto-match and timestamps.
4. Test `.txt` fallback, wrong/missing file, a directory outside the permitted Music tree, large/invalid document, revoked folder permission.
5. Manually select a `.lrc` in Downloads and verify manual precedence.
6. Turn Wi-Fi on and search with LRCLIB, select correct song, Save. Turn Wi-Fi off and view cached lyrics.
7. Verify no background online lyrics traffic when playback starts or a song changes.
8. Confirm offline playback/queue/sleep timer/normal Favorites update still work.
9. Publish tag v0.6.0 signed by original key. Install in-app over signed v0.5.0. Favorites remain.
10. If build fails, review the **first** Kotlin compile error; do not publish a release tag until green.
