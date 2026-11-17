package ee.episodeday.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object Storage {
    private const val PREFS = "episode_day"
    private const val SHOWS = "shows"
    fun getShows(context: Context): List<Show> {
        val raw = context.getSharedPreferences(PREFS, 0).getString(SHOWS, "[]") ?: "[]"
        val arr = JSONArray(raw)
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it); Show(o.getInt("id"), o.getString("name"))
        }
    }
    fun saveShows(context: Context, shows: List<Show>) {
        val arr = JSONArray()
        shows.distinctBy { it.id }.forEach { arr.put(JSONObject().put("id", it.id).put("name", it.name)) }
        context.getSharedPreferences(PREFS, 0).edit().putString(SHOWS, arr.toString()).apply()
    }
    fun notified(context: Context, key: String): Boolean =
        context.getSharedPreferences(PREFS, 0).getBoolean("notified_$key", false)
    fun markNotified(context: Context, key: String) {
        context.getSharedPreferences(PREFS, 0).edit().putBoolean("notified_$key", true).apply()
    }
}
