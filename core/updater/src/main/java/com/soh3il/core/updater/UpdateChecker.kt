package com.soh3il.core.updater

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

data class ApkAsset(
    val name: String,
    val downloadUrl: String,
    val sizeBytes: Long
)

data class UpdateInfo(
    val hasUpdate: Boolean,
    val latestVersionName: String,
    val downloadUrl: String,
    val changelog: String,
    val matchedApkName: String = "",
    val releaseHtmlUrl: String = ""
)

object UpdateChecker {

    private const val GITHUB_REPO = "xSOH3ILx/SOH3IL"
    private const val GITHUB_API_URL = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun checkForUpdates(currentVersionName: String): UpdateInfo = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(GITHUB_API_URL)
                .header("Accept", "application/vnd.github.v3+json")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext UpdateInfo(false, currentVersionName, "", "")
            }

            val body = response.body?.string() ?: return@withContext UpdateInfo(false, currentVersionName, "", "")
            val json = JSONObject(body)

            val tagName = json.optString("tag_name", "").removePrefix("v")
            val changelog = json.optString("body", "بهبود پایداری، رفع باگ‌ها و ارتقای رابط کاربری")
            val releaseHtmlUrl = json.optString("html_url", "https://github.com/$GITHUB_REPO/releases/latest")

            // Collect all APK assets
            val assetsJson = json.optJSONArray("assets")
            val apkAssets = mutableListOf<ApkAsset>()
            if (assetsJson != null) {
                for (i in 0 until assetsJson.length()) {
                    val asset = assetsJson.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk")) {
                        apkAssets.add(
                            ApkAsset(
                                name = name,
                                downloadUrl = asset.optString("browser_download_url", ""),
                                sizeBytes = asset.optLong("size", 0L)
                            )
                        )
                    }
                }
            }

            val chosenAsset = selectBestApkAsset(apkAssets)
            val isNewer = isVersionNewer(tagName, currentVersionName)

            UpdateInfo(
                hasUpdate = isNewer && chosenAsset != null,
                latestVersionName = tagName,
                downloadUrl = chosenAsset?.downloadUrl ?: "",
                changelog = changelog,
                matchedApkName = chosenAsset?.name ?: "",
                releaseHtmlUrl = releaseHtmlUrl
            )
        } catch (e: Exception) {
            UpdateInfo(false, currentVersionName, "", "")
        }
    }

    private fun selectBestApkAsset(assets: List<ApkAsset>): ApkAsset? {
        if (assets.isEmpty()) return null

        val supportedAbis = Build.SUPPORTED_ABIS.map { it.lowercase() }

        // 1. Exact CPU ABI match
        for (abi in supportedAbis) {
            val match = assets.find { it.name.lowercase().contains(abi) }
            if (match != null) return match
        }

        // 2. Universal build fallback
        val universal = assets.find { it.name.lowercase().contains("universal") }
        if (universal != null) return universal

        // 3. First available APK
        return assets.firstOrNull()
    }

    private fun isVersionNewer(latest: String, current: String): Boolean {
        val latestParts = latest.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }

        val size = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until size) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    /**
     * Checks if the app has permission to install unknown apps (Android 8.0+).
     */
    fun canInstallUnknownPackages(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /**
     * Opens the system settings screen for granting unknown app install permission for this package.
     */
    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                val fallback = Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
            }
        }
    }

    /**
     * Downloads APK directly into the app's cache directory with progress reporting.
     */
    suspend fun downloadApk(
        context: Context,
        downloadUrl: String,
        versionName: String,
        onProgress: (progress: Float, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(downloadUrl)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("خطا در دریافت فایل از سرور: کد ${response.code}"))
            }

            val body = response.body ?: return@withContext Result.failure(IOException("محتوای دریافتی خالی است"))
            val totalBytes = body.contentLength()

            val updateDir = File(context.cacheDir, "updates")
            if (!updateDir.exists()) {
                updateDir.mkdirs()
            }
            val apkFile = File(updateDir, "SOH3IL-v$versionName.apk")
            if (apkFile.exists()) {
                apkFile.delete()
            }

            var downloadedBytes = 0L
            body.byteStream().use { input ->
                apkFile.outputStream().use { output ->
                    val buffer = ByteArray(16 * 1024)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        val progress = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else 0f
                        onProgress(progress, downloadedBytes, totalBytes)
                    }
                    output.flush()
                }
            }

            Result.success(apkFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Launches system PackageInstaller for the downloaded APK using FileProvider.
     */
    fun launchApkInstall(context: Context, apkFile: File): Boolean {
        return try {
            if (!apkFile.exists()) return false

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Opens the browser download link as a safe secondary fallback.
     */
    fun openBrowserDownload(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }
}
