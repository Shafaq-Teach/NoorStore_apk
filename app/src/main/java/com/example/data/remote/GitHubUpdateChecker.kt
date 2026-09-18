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
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
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
            val notes = json.optString("body", "يېڭى نەشر چىقتى. تېز كۆرۈنمە يۈز ۋە كاشىلا تۈزىتىش ئېلىپ بېرىلدى.")

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

    fun downloadAndInstallApk(context: Context, downloadUrl: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("GitHubUpdateChecker", "Failed to launch download URL: ${e.message}")
        }
    }
}
