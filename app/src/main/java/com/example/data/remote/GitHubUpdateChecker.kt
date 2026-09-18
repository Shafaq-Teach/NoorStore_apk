package com.example.data.remote

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

import java.io.File
import androidx.core.content.FileProvider

data class AppUpdateInfo(
    val latestVersionName: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val apkDownloadUrl: String,
    val isUpdateAvailable: Boolean
)

object GitHubUpdateChecker {
    private const val GITHUB_REPO = "Shafaq-Teach/NoorStore_apk"
    private const val DEFAULT_VERSION = "1.0.0"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun checkForUpdate(context: Context): AppUpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://api.github.com/repos/$GITHUB_REPO/releases/latest")
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "NoorStoreApp")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val bodyString = response.body?.string() ?: return@withContext null
            val json = JSONObject(bodyString)

            val tagName = json.optString("tag_name", "")
            val title = json.optString("name", "Noor Store $tagName")
            val notes = json.optString("body", "يېڭى نەشر چىقتى.")

            var apkUrl = "https://github.com/$GITHUB_REPO/releases/latest"
            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk")) {
                        apkUrl = asset.optString("browser_download_url", apkUrl)
                        break
                    }
                }
            }

            val currentVer = try {
                val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                pInfo.versionName ?: DEFAULT_VERSION
            } catch (e: Exception) {
                DEFAULT_VERSION
            }

            val hasNewer = isNewerVersion(tagName, currentVer)

            AppUpdateInfo(
                latestVersionName = tagName,
                releaseTitle = title,
                releaseNotes = notes,
                apkDownloadUrl = apkUrl,
                isUpdateAvailable = hasNewer
            )
        } catch (e: Exception) {
            android.util.Log.e("GitHubUpdateChecker", "Update check failed: ${e.message}")
            null
        }
    }

    private fun isNewerVersion(remoteTag: String, localVersion: String): Boolean {
        val cleanRemote = remoteTag.replace(Regex("^[^0-9]*"), "").trim()
        val cleanLocal = localVersion.replace(Regex("^[^0-9]*"), "").trim()
        if (cleanRemote == cleanLocal || cleanRemote.isEmpty()) return false

        val remoteParts = cleanRemote.split(".").map { it.toIntOrNull() ?: 0 }
        val localParts = cleanLocal.split(".").map { it.toIntOrNull() ?: 0 }

        val maxLen = maxOf(remoteParts.size, localParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val l = localParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }

    suspend fun downloadAndInstallApk(
        context: Context,
        downloadUrl: String,
        onProgress: (Float) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(downloadUrl)
                .header("User-Agent", "NoorStoreApp")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                withContext(Dispatchers.Main) {
                    onError("Download failed: HTTP ${response.code}")
                }
                return@withContext
            }

            val body = response.body ?: run {
                withContext(Dispatchers.Main) { onError("چۈشۈرۈش مەغلۇپ بولدى") }
                return@withContext
            }

            val contentLength = body.contentLength()
            val outputFile = File(context.cacheDir, "NoorStore_update.apk")
            if (outputFile.exists()) outputFile.delete()

            body.byteStream().use { input ->
                outputFile.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var bytesCopied = 0L
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        bytesCopied += read
                        if (contentLength > 0) {
                            val progress = bytesCopied.toFloat() / contentLength.toFloat()
                            withContext(Dispatchers.Main) {
                                onProgress(progress)
                            }
                        }
                    }
                    output.flush()
                }
            }

            withContext(Dispatchers.Main) {
                onProgress(1.0f)
                installApk(context, outputFile)
            }
        } catch (e: Exception) {
            android.util.Log.e("GitHubUpdateChecker", "Download error: ${e.message}", e)
            withContext(Dispatchers.Main) {
                onError(e.localizedMessage ?: "چۈشۈرۈشتە خاتالىق كۆرۈلدى")
            }
        }
    }

    fun installApk(context: Context, apkFile: File) {
        try {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("GitHubUpdateChecker", "Install launch failed: ${e.message}", e)
        }
    }
}
