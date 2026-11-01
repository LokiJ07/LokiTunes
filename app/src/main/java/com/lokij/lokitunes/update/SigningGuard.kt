package com.lokij.lokitunes.update

/** Refuse to open Android's installer if the app and update have different signers. */
object SigningGuard {
    fun sameCertificate(installed: Set<String>, downloaded: Set<String>): Boolean =
        installed.isNotEmpty() && downloaded.isNotEmpty() && installed == downloaded
}
