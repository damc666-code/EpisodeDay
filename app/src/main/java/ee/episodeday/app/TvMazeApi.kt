
package ee.episodeday.app

import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

object TvMazeApi {

    private fun get(path: String): String {
        val connection = URL("https://api.tvmaze.com$path")
            .openConnection() as HttpURLConnection

        connection.connectTimeout = 12000
        connection.readTimeout = 12000
        connection.setRequestProperty("User-Agent", "EpisodeDay-Android/2.0")

        return try {
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    fun search(query: String): List<Show> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val arr = JSONArray(get("/search/shows?q=$encoded"))

        return (0 until arr.length()).mapNotNull { i ->
            val show = arr.getJSONObject(i).optJSONObject("show")
                ?: return@mapNotNull null

            val image = show.optJSONObject("image")
            val imageUrl = image?.optString("original")
                ?.takeIf { !it.isNullOrBlank() && it != "null" }
                ?: image?.optString("medium")
                    ?.takeIf { !it.isNullOrBlank() && it != "null" }

            Show(
                id = show.optInt("id"),
                name = show.optString("name"),
                imageUrl = imageUrl
            )
        }.filter { it.id > 0 && it.name.isNotBlank() }.take(20)
    }

    fun episodes(show: Show): List<Episode> {
        val arr = JSONArray(get("/shows/${show.id}/episodes"))

        return (0 until arr.length()).mapNotNull { i ->
            val e = arr.getJSONObject(i)
            val date = e.optString("airdate", "")

            if (date.isBlank() || date == "null") {
                return@mapNotNull null
            }

            Episode(
                showId = show.id,
                showName = show.name,
                name = e.optString("name", "Episode"),
                season = e.optInt("season"),
                number = e.optInt("number"),
                airdate = date,
                summary = android.text.Html.fromHtml(
                    e.optString("summary", ""),
                    android.text.Html.FROM_HTML_MODE_LEGACY
                ).toString()
            )
        }
    }

    fun refreshAll(shows: List<Show>): List<Episode> =
        shows.flatMap { show ->
            runCatching { episodes(show) }.getOrDefault(emptyList())
        }.sortedBy { it.airdate }
}
