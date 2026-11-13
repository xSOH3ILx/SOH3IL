package com.soh3il.core.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class UpdateInfo(
    val hasUpdate: Boolean,
    val latestVersionName: String,
    val changelog: String,
    val releaseHtmlUrl: String
)

/**
 * Reads release metadata from the project's official GitHub page.
 * Installation remains delegated to the platform's trusted distribution channel.
 */
object UpdateChecker {

    private const val GITHUB_REPO = "xSOH3ILx/SOH3IL"
    private const val GITHUB_API_URL = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"
    private const val DEFAULT_RELEASE_URL = "https://github.com/$GITHUB_REPO/releases/latest"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    private fun executeAllowed(initial: Request): okhttp3.Response {
        var request = initial
        repeat(5) {
            val response = client.newCall(request).execute()
            if (response.code !in 300..399) return response
            val location = response.header("Location")
            response.close()
            if (location.isNullOrBlank()) {
                throw IOException("پاسخ تغییر مسیر بدون مقصد بود")
            }
            val next = request.url.resolve(location)
                ?: throw IOException("مقصد تغییر مسیر نامعتبر است")
            if (!UpdateUrlPolicy.isAllowed(next.toString())) {
                throw SecurityException("مقصد به‌روزرسانی خارج از GitHub مجاز نیست")
            }
            request = request.newBuilder().url(next).build()
        }
        throw IOException("تغییر مسیر به‌روزرسانی بیش از حد تکرار شد")
    }

    suspend fun checkForUpdates(currentVersionName: String): UpdateInfo = withContext(Dispatchers.IO) {
        try {
            if (!UpdateUrlPolicy.isAllowed(GITHUB_API_URL)) {
                return@withContext noUpdate(currentVersionName)
            }

            val request = Request.Builder()
                .url(GITHUB_API_URL)
                .header("Accept", "application/vnd.github.v3+json")
                .build()

            executeAllowed(request).use { response ->
                if (response.code != 200) return@withContext noUpdate(currentVersionName)
                val body = response.body?.string() ?: return@withContext noUpdate(currentVersionName)
                val json = JSONObject(body)
                val latestVersionName = json.optString("tag_name", "").removePrefix("v")
                val releaseHtmlUrl = json.optString("html_url", DEFAULT_RELEASE_URL)
                    .takeIf { UpdateUrlPolicy.isAllowed(it) }
                    ?: DEFAULT_RELEASE_URL
                val changelog = json.optString(
                    "body",
                    "بهبود پایداری، رفع باگ‌ها و ارتقای رابط کاربری"
                )
                UpdateInfo(
                    hasUpdate = isVersionNewer(latestVersionName, currentVersionName),
                    latestVersionName = latestVersionName,
                    changelog = changelog,
                    releaseHtmlUrl = releaseHtmlUrl
                )
            }
        } catch (_: Exception) {
            noUpdate(currentVersionName)
        }
    }

    private fun noUpdate(currentVersionName: String) = UpdateInfo(
        hasUpdate = false,
        latestVersionName = currentVersionName,
        changelog = "",
        releaseHtmlUrl = DEFAULT_RELEASE_URL
    )

    private fun isVersionNewer(latest: String, current: String): Boolean {
        val latestParts = latest.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }
        val size = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until size) {
            val latestPart = latestParts.getOrElse(i) { 0 }
            val currentPart = currentParts.getOrElse(i) { 0 }
            if (latestPart > currentPart) return true
            if (latestPart < currentPart) return false
        }
        return false
    }

    fun openReleasePage(context: Context, url: String) {
        if (!UpdateUrlPolicy.isAllowed(url)) return
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
    }
}
