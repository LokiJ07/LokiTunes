package com.lokij.lokitunes.update

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.core.content.pm.PackageInfoCompat
import com.lokij.lokitunes.BuildConfig
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** A public GitHub Release signed APK, NOT the GitHub Actions debug artifact. */
data class LokiRelease(
    val tag: String,
    val notes: String,
    val url: String,
    val sha256: String,
    val bytes: Long,
    val version: VersionNumber
)

data class VersionNumber(val major: Int, val minor: Int, val patch: Int) : Comparable<VersionNumber> {
    override fun compareTo(other: VersionNumber): Int =
        compareValuesBy(this, other, VersionNumber::major, VersionNumber::minor, VersionNumber::patch)

    companion object {
        private val versionRegex = Regex("^v?(\\d+)\\.(\\d+)\\.(\\d+)$")
        fun parse(value: String): VersionNumber? {
            val m = versionRegex.matchEntire(value.trim()) ?: return null
            return runCatching {
                VersionNumber(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt())
            }.getOrNull()
        }
    }
}

class UpdateRepository(private val context: Context) {
    private val owner = "LokiJ07"
    private val repo = "LokiTunes"
    private val releaseApi = "https://api.github.com/repos/$owner/$repo/releases/latest"

    fun latestRelease(): LokiRelease? {
        val connection = (URL(releaseApi).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12_000
            readTimeout = 12_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "LokiTunes-Android/${BuildConfig.VERSION_NAME}")
        }
        val body = try {
            when (val code = connection.responseCode) {
                200 -> connection.inputStream.bufferedReader().use { it.readText() }
                404 -> return null // No public, non-prerelease GitHub Release yet.
                else -> error("GitHub update check returned HTTP $code")
            }
        } finally { connection.disconnect() }
        val release = JSONObject(body)
        val tag = release.optString("tag_name")
        val version = VersionNumber.parse(tag) ?: return null
        val installedVersion = VersionNumber.parse(BuildConfig.VERSION_NAME) ?: return null
        if (version <= installedVersion) return null
        val expectedName = "LokiTunes-$tag-release.apk"
        val assets = release.optJSONArray("assets") ?: return null
        for (i in 0 until assets.length()) {
            val asset = assets.optJSONObject(i) ?: continue
            if (asset.optString("name") != expectedName) continue
            val url = asset.optString("browser_download_url")
            val safePrefix = "https://github.com/$owner/$repo/releases/download/$tag/"
            if (url != "$safePrefix$expectedName") continue
            val digest = asset.optString("digest")
            // GitHub supplies the SHA-256 of each uploaded asset; require it before installation.
            if (!Regex("^sha256:[0-9a-fA-F]{64}$").matches(digest)) {
                error("Release APK checksum is unavailable. Open GitHub Releases instead.")
            }
            val bytes = asset.optLong("size", -1L)
            if (bytes !in 1L..(150L * 1024 * 1024)) error("Unexpected release APK size")
            return LokiRelease(tag, release.optString("body"), url, digest.substringAfter(':'), bytes, version)
        }
        return null
    }

    /** Download to app-specific files: no storage permission, no access to music content. */
    fun download(release: LokiRelease, progress: (Int) -> Unit): File {
        val folder = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: File(context.filesDir, "updates").apply { mkdirs() }
        require(folder.exists() || folder.mkdirs()) { "Unable to create the update folder" }
        val result = File(folder, "LokiTunes-${release.tag}-release.apk")
        val temp = File(folder, "LokiTunes-${release.tag}-partial.apk")
        val connection = (URL(release.url).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 15_000
            readTimeout = 25_000
            setRequestProperty("User-Agent", "LokiTunes-Android/${BuildConfig.VERSION_NAME}")
        }
        try {
            require(connection.responseCode == 200) { "APK download failed: HTTP ${connection.responseCode}" }
            val digest = MessageDigest.getInstance("SHA-256")
            var count = 0L
            var previousPercent = -1
            connection.inputStream.use { input ->
                temp.outputStream().buffered().use { output ->
                    val buffer = ByteArray(32 * 1024)
                    while (true) {
                        val len = input.read(buffer)
                        if (len < 0) break
                        output.write(buffer, 0, len)
                        digest.update(buffer, 0, len)
                        count += len
                        require(count <= release.bytes) { "APK size exceeded the published size" }
                        val percent = (100L * count / release.bytes).toInt().coerceIn(0, 100)
                        if (percent > previousPercent) {
                            previousPercent = percent
                            progress(percent)
                        }
                    }
                }
            }
            require(count == release.bytes) { "Downloaded APK is incomplete" }
            val hash = digest.digest().joinToString("") { "%02x".format(it) }
            require(hash.equals(release.sha256, ignoreCase = true)) { "APK checksum mismatch. Nothing was installed." }
            // Never substitute a renamed third-party APK. The system installer also checks signatures.
            val pkg = archivePackage(temp) ?: error("Android could not read the update APK")
            require(pkg.packageName == context.packageName) { "Wrong app package in release" }
            val currentCode = PackageInfoCompat.getLongVersionCode(
                context.packageManager.getPackageInfo(context.packageName, 0)
            )
            require(PackageInfoCompat.getLongVersionCode(pkg) > currentCode) { "APK version code is not newer" }
            if (result.exists() && !result.delete()) error("Unable to replace old update file")
            require(temp.renameTo(result)) { "Unable to save downloaded APK" }
            return result
        } catch (e: Exception) {
            temp.delete()
            throw e
        } finally { connection.disconnect() }
    }

    @Suppress("DEPRECATION")
    private fun archivePackage(file: File) = context.packageManager.getPackageArchiveInfo(
        file.absolutePath,
        if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
    )
}
