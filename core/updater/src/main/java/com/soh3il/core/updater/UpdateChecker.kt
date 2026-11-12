package com.soh3il.core.updater

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import java.security.MessageDigest
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
            val next = request.url.resolve(location) ?: throw IOException("مقصد تغییر مسیر نامعتبر است")
            if (!UpdateUrlPolicy.isAllowed(next.toString())) {
                throw SecurityException("مقصد به‌روزرسانی خارج از GitHub مجاز نیست")
            }
            request = request.newBuilder().url(next).build()
        }
        throw IOException("تغییر مسیر به‌روزرسانی بیش از حد تکرار شد")
    }

    suspend fun checkForUpdates(currentVersionName: String): UpdateInfo = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(GITHUB_API_URL)
                .header("Accept", "application/vnd.github.v3+json")
                .build()

            if (!UpdateUrlPolicy.isAllowed(GITHUB_API_URL)) {
                return@withContext UpdateInfo(false, currentVersionName, "", "")
            }
            val response = executeAllowed(request)
            if (!response.isSuccessful) {
                response.close()
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
            val downloadUrl = chosenAsset?.downloadUrl ?: ""
            val isNewer = isVersionNewer(tagName, currentVersionName)
            val safeUrl = downloadUrl.isNotBlank() && UpdateUrlPolicy.isAllowed(downloadUrl)

            UpdateInfo(
                hasUpdate = isNewer && chosenAsset != null && safeUrl,
                latestVersionName = tagName,
                downloadUrl = if (safeUrl) downloadUrl else "",
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
            val match = assets.find { apkNameMatchesAbi(it.name, abi) }
            if (match != null) return match
        }

        // 2. Universal build fallback
        val universal = assets.find { it.name.lowercase().contains("universal") }
        if (universal != null) return universal

        // 3. First available APK
        return assets.firstOrNull { UpdateUrlPolicy.isAllowed(it.downloadUrl) }
    }

    private fun apkNameMatchesAbi(name: String, abi: String): Boolean {
        val normalized = name.lowercase()
        val token = abi.lowercase()
        return normalized.contains("-$token.") ||
            normalized.contains("-$token-") ||
            normalized.contains("_$token.") ||
            normalized.contains("_$token-") ||
            normalized.endsWith("-$token.apk") ||
            normalized.endsWith("_$token.apk")
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
     * Downloads APK directly into the app's cache directory with atomic .part file handling and progress reporting.
     */
    suspend fun downloadApk(
        context: Context,
        downloadUrl: String,
        versionName: String,
        onProgress: (progress: Float, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        var partFile: File? = null
        var apkFile: File? = null
        try {
            if (!UpdateUrlPolicy.isAllowed(downloadUrl)) {
                return@withContext Result.failure(SecurityException("نشانی دریافت به‌روزرسانی مجاز نیست"))
            }
            val request = Request.Builder()
                .url(downloadUrl)
                .build()

            val response = executeAllowed(request)
            if (!response.isSuccessful) {
                val code = response.code
                response.close()
                return@withContext Result.failure(IOException("خطا در دریافت فایل از سرور: کد $code"))
            }

            val body = response.body ?: return@withContext Result.failure(IOException("محتوای دریافتی خالی است"))
            val totalBytes = body.contentLength()

            val updateDir = File(context.cacheDir, "updates")
            if (!updateDir.exists()) {
                updateDir.mkdirs()
            }
            // Clean up any stale APKs, parts, or tmp files before downloading
            updateDir.listFiles()?.forEach { file ->
                if (file.name.endsWith(".apk") || file.name.endsWith(".part") || file.name.endsWith(".tmp")) {
                    runCatching { file.delete() }
                }
            }

            val part = File(updateDir, "SOH3IL-v$versionName.apk.part")
            val targetApk = File(updateDir, "SOH3IL-v$versionName.apk")
            partFile = part
            apkFile = targetApk

            var downloadedBytes = 0L
            body.byteStream().use { input ->
                part.outputStream().use { output ->
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

            // Verify content length integrity if provided
            if (totalBytes > 0 && downloadedBytes != totalBytes) {
                part.delete()
                return@withContext Result.failure(IOException("حجم فایل بارگیری شده با سرور همخوانی ندارد ($downloadedBytes / $totalBytes)"))
            }

            // Atomic rename to .apk
            if (!part.renameTo(targetApk)) {
                part.copyTo(targetApk, overwrite = true)
                part.delete()
            }

            // Package Integrity & Signature Verification
            if (!verifyApkSignature(context, targetApk)) {
                targetApk.delete()
                return@withContext Result.failure(SecurityException("امضای دیجیتال بسته به‌روزرسانی با برنامه فعلی مطابقت ندارد"))
            }

            Result.success(targetApk)
        } catch (e: Exception) {
            partFile?.let { runCatching { it.delete() } }
            apkFile?.let { runCatching { it.delete() } }
            Result.failure(e)
        }
    }

    /**
     * Verifies package name and cryptographic signature of downloaded APK against the currently running app.
     */
    fun verifyApkSignature(context: Context, apkFile: File): Boolean {
        return try {
            val pm = context.packageManager
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                @Suppress("DEPRECATION")
                PackageManager.GET_SIGNATURES
            }

            val archiveInfo = pm.getPackageArchiveInfo(apkFile.absolutePath, flags) ?: return false
            if (archiveInfo.packageName != context.packageName) {
                return false
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val apkSigningInfo = archiveInfo.signingInfo ?: return false
                val currentPackageInfo = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                val currentSigningInfo = currentPackageInfo.signingInfo ?: return false

                val apkCerts = if (apkSigningInfo.hasMultipleSigners()) {
                    apkSigningInfo.apkContentsSigners
                } else {
                    apkSigningInfo.signingCertificateHistory
                }

                val currentCerts = if (currentSigningInfo.hasMultipleSigners()) {
                    currentSigningInfo.apkContentsSigners
                } else {
                    currentSigningInfo.signingCertificateHistory
                }

                if (apkCerts == null || currentCerts == null) return false

                val apkDigests = apkCerts.map { computeCertificateSha256(it.toByteArray()) }.toSet()
                val currentDigests = currentCerts.map { computeCertificateSha256(it.toByteArray()) }.toSet()

                apkDigests.intersect(currentDigests).isNotEmpty()
            } else {
                @Suppress("DEPRECATION")
                val apkSignatures = archiveInfo.signatures
                @Suppress("DEPRECATION")
                val currentPackageInfo = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
                @Suppress("DEPRECATION")
                val currentSignatures = currentPackageInfo.signatures

                if (apkSignatures.isNullOrEmpty() || currentSignatures.isNullOrEmpty()) return false

                val apkDigests = apkSignatures.map { computeCertificateSha256(it.toByteArray()) }.toSet()
                val currentDigests = currentSignatures.map { computeCertificateSha256(it.toByteArray()) }.toSet()

                apkDigests.intersect(currentDigests).isNotEmpty()
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun computeCertificateSha256(certBytes: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(certBytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Launches system PackageInstaller for the downloaded APK using FileProvider.
     */
    fun launchApkInstall(context: Context, apkFile: File): Boolean {
        return try {
            if (!apkFile.exists()) return false

            // Strict signature and package verification before handing over to PackageInstaller
            if (!verifyApkSignature(context, apkFile)) {
                runCatching { apkFile.delete() }
                return false
            }

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
        if (!UpdateUrlPolicy.isAllowed(url)) return
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }
}
