package com.example.tajweed

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import java.util.Calendar

object Reminder {
    private fun pending(c: Context): PendingIntent = PendingIntent.getBroadcast(
        c, 1, Intent(c, ReminderReceiver::class.java).setAction("com.example.tajweed.REMIND"),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    fun schedule(c: Context, hour: Int) {
        val am = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        am.setInexactRepeating(AlarmManager.RTC, cal.timeInMillis, AlarmManager.INTERVAL_DAY, pending(c))
    }

    fun cancel(c: Context) {
        (c.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pending(c))
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val p = c.getSharedPreferences("progress", 0)
        if (!p.getBoolean("remind", false)) return
        if (i.action == Intent.ACTION_BOOT_COMPLETED) {
            Reminder.schedule(c, p.getInt("hour", 20)); return
        }
        // لا تذكير إن أنجز المتعلم درسًا اليوم
        if (p.getLong("last", 0) == System.currentTimeMillis() / 86_400_000L) return
        val launch = c.packageManager.getLaunchIntentForPackage(c.packageName) ?: return
        val nm = c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(NotificationChannel("daily", "التذكير اليومي", NotificationManager.IMPORTANCE_DEFAULT))
        }
        val open = PendingIntent.getActivity(c, 2, launch, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(c, "daily")
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle("حان وقت التجويد 📖")
            .setContentText("درس قصير اليوم يحفظ سلسلة أيامك 🔥")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        nm.notify(7, n)
    }
}
