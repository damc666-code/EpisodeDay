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

        return (0 until arr.length()).map { index ->

            val o = arr.getJSONObject(index)

            val rating = if (
                o.has("rating") && !o.isNull("rating")
            ) {
                o.optDouble("rating").takeIf {
                    it > 0.0
                }
            } else {
                null
            }

            Show(
                id = o.getInt("id"),
                name = o.getString("name"),

                imageUrl = o.optString("imageUrl")
                    .takeIf { url ->
                        !url.isNullOrBlank() && url != "null"
                    },

                premieredYear = o.optInt("premieredYear")
                    .takeIf { year ->
                        year > 0
                    },

                rating = rating
            )
        }
    }

    fun saveShows(context: Context, shows: List<Show>) {
        val arr = JSONArray()

        shows.distinctBy {
            it.id
        }.forEach { show ->

            arr.put(
                JSONObject()
                    .put("id", show.id)
                    .put("name", show.name)
                    .put("imageUrl", show.imageUrl ?: "")
                    .put("premieredYear", show.premieredYear ?: 0)
                    .put("rating", show.rating ?: JSONObject.NULL)
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
