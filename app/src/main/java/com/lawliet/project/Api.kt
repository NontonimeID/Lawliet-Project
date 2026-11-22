package com.lawliet.project

import android.text.Html
import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader

object Api {
    private val http = OkHttpClient()
    private val JSON = "application/json".toMediaType()

    suspend fun post(path: String, body: JSONObject, token: String? = null): JSONObject = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(BuildConfig.API_BASE + path)
            .post(body.toString().toRequestBody(JSON))
            .apply { if (token != null) header("Authorization", "Bearer $token") }.build()
        http.newCall(req).execute().use { JSONObject(it.body?.string() ?: "{}") }
    }

    suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build()
        http.newCall(req).execute().use { it.body?.string() ?: "" }
    }

    suspend fun news(): List<NewsItem> = withContext(Dispatchers.IO) {
        listOf(
            "https://rss.detik.com/index.php/detikcom",
            "https://www.antaranews.com/rss/terkini.xml",
            "https://www.cnnindonesia.com/nasional/rss",
        ).flatMap { runCatching { parseRss(get(it)).take(6) }.getOrDefault(emptyList()) }
    }

    fun parseRss(xml: String): List<NewsItem> {
        val p = Xml.newPullParser(); p.setInput(StringReader(xml))
        val out = mutableListOf<NewsItem>()
        var t = ""; var d = ""; var link = ""; var img: String? = null; var inItem = false
        var e = p.eventType
        while (e != XmlPullParser.END_DOCUMENT) {
            if (e == XmlPullParser.START_TAG) {
                when (p.name) {
                    "item" -> { inItem = true; t = ""; d = ""; link = ""; img = null }
                    "title" -> if (inItem) t = p.nextText()
                    "description" -> if (inItem) d = p.nextText()
                    "link" -> if (inItem) link = p.nextText()
                    "enclosure", "media:content", "media:thumbnail" ->
                        if (inItem && img == null) img = p.getAttributeValue(null, "url")
                }
            } else if (e == XmlPullParser.END_TAG && p.name == "item") {
                inItem = false
                if (img == null) img = Regex("src=\"([^\"]+)\"").find(d)?.groupValues?.get(1)
                val clean = Html.fromHtml(d, Html.FROM_HTML_MODE_LEGACY).toString().trim()
                out += NewsItem(t.trim(), clean, img, link.trim())
            }
            e = p.next()
        }
        return out
    }
}
