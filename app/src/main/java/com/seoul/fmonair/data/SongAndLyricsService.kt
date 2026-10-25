package com.seoul.fmonair.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder

object SongAndLyricsService {
    private val client = OkHttpClient()

    suspend fun searchLyrics(query: String): Pair<String, String>? = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://lrclib.net/api/search?q=\$encodedQuery"
            val request = Request.Builder().url(url).build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val array = JSONArray(body)
                if (array.length() > 0) {
                    val item = array.getJSONObject(0)
                    val syncedLyrics = item.optString("syncedLyrics", "")
                    val plainLyrics = item.optString("plainLyrics", "")
                    val lyrics = if (syncedLyrics.isNotEmpty()) syncedLyrics else plainLyrics
                    val trackInfo = "\${item.optString("trackName")} - \${item.optString("artistName")}"
                    return@withContext Pair(trackInfo, lyrics)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }
}
