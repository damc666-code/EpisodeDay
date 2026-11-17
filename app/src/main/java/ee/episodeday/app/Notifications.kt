package ee.episodeday.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.time.LocalDate

object Notifications {
    private const val CHANNEL = "episodes"
    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL, "Uued episoodid", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
    }
    fun notifyToday(context: Context, episodes: List<Episode>) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val today = LocalDate.now().toString()
        episodes.filter { it.airdate == today }.forEach { ep ->
            val key = "${ep.showId}_${ep.season}_${ep.number}_$today"
            if (!Storage.notified(context, key)) {
                val text = "${ep.showName}: ${ep.name}"
                val notification = NotificationCompat.Builder(context, CHANNEL)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("Täna ilmub uus osa!")
                    .setContentText(text)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .build()
                NotificationManagerCompat.from(context).notify(key.hashCode(), notification)
                Storage.markNotified(context, key)
            }
        }
    }
}
