package ee.episodeday.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object Storage {

    private const val PREFS = "episode_day"
    private const val SHOWS = "shows"

    fun getShows(context: Context): List<Show> {
        val raw = context.getSharedPreferences(PREFS, 0)
            .getString(SHOWS, "[]") ?: "[]"

        val arr = JSONArray(raw)

        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)

            Show(
                id = o.getInt("id"),
                name = o.getString("name"),
                imageUrl = o.optString("imageUrl")
                    .takeIf { url ->
                        !url.isNullOrBlank() && url != "null"
                    },
                premieredYear = o.optInt("premieredYear")
                    .takeIf { year -> year > 0 }
            )
        }
    }

    fun saveShows(context: Context, shows: List<Show>) {
        val arr = JSONArray()

        shows.distinctBy { it.id }.forEach { show ->
            arr.put(
                JSONObject()
                    .put("id", show.id)
                    .put("name", show.name)
                    .put("imageUrl", show.imageUrl ?: "")
                    .put("premieredYear", show.premieredYear ?: 0)
            )
        }

        context.getSharedPreferences(PREFS, 0)
            .edit()
            .putString(SHOWS, arr.toString())
            .apply()
    }

    fun notified(context: Context, key: String): Boolean =
        context.getSharedPreferences(PREFS, 0)
            .getBoolean("notified_$key", false)

    fun markNotified(context: Context, key: String) {
        context.getSharedPreferences(PREFS, 0)
            .edit()
            .putBoolean("notified_$key", true)
            .apply()
    }
}
