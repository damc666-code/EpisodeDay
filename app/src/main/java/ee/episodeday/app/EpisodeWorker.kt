package ee.episodeday.app

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class EpisodeWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result = try {
        val shows = Storage.getShows(applicationContext)
        if (shows.isNotEmpty()) {
            val episodes = TvMazeApi.refreshAll(shows)
            Notifications.notifyToday(applicationContext, episodes)
        }
        Result.success()
    } catch (_: Exception) {
        Result.retry()
    }
}
