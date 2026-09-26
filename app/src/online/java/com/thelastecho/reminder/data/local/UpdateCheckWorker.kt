package com.thelastecho.reminder.data.local

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.thelastecho.reminder.core.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class UpdateCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result { return try {
        val prefs = UserPreferencesRepository(applicationContext)
        val settings = prefs.themeSettings.first()
        val manual = inputData.getBoolean(KEY_MANUAL, false)
        val now = System.currentTimeMillis()
        if (!manual && !settings.automaticUpdateChecks) return Result.success()
        if (!manual && now - settings.lastUpdateCheckMillis < CHECK_INTERVAL_MILLIS) return Result.success()

        val connection = URL(LATEST_RELEASE_API).openConnection() as HttpURLConnection
        val payload = try {
            connection.connectTimeout = 8_000
            connection.readTimeout = 8_000
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", "Reminder-Android")
            if (connection.responseCode !in 200..299) return Result.retry()
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally { connection.disconnect() }
        val release = JSONObject(payload)
        val tag = release.getString("tag_name")
        val url = release.getString("html_url")
        prefs.saveUpdateCheck(now, tag, url)
        Result.success()
    } catch (_: Exception) { Result.retry() } }

    companion object {
        const val KEY_MANUAL = "manual"
        const val UNIQUE_MANUAL = "manual-release-check"
        const val UNIQUE_PERIODIC = "periodic-release-check"
        private const val CHECK_INTERVAL_MILLIS = 24L * 60 * 60 * 1000
        private const val LATEST_RELEASE_API = "https://api.github.com/repos/The-Last-Echo/android-reminder/releases/latest"
    }
}
