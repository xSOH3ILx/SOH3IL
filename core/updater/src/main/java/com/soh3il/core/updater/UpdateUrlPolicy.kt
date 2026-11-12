package com.soh3il.core.updater

import java.net.URI

object UpdateUrlPolicy {

    private val ALLOWED_HOSTS = setOf(
        "github.com",
        "api.github.com",
        "objects.githubusercontent.com",
        "release-assets.githubusercontent.com",
        "github-releases.githubusercontent.com",
        "codeload.github.com"
    )

    fun isAllowed(url: String): Boolean {
        val uri = try {
            URI(url)
        } catch (_: Exception) {
            return false
        }
        if (!uri.scheme.equals("https", ignoreCase = true)) return false
        val host = uri.host?.lowercase() ?: return false
        if (host in ALLOWED_HOSTS) return true
        return host.endsWith(".githubusercontent.com")
    }
}
