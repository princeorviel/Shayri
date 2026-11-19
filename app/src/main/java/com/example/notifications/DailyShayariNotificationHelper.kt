package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.ShayariCategory
import com.example.data.model.ShayariEntity
import java.util.Calendar
import java.util.Locale

object DailyShayariNotificationHelper {

    const val CHANNEL_ID = "daily_shayari_channel"
    const val NOTIFICATION_ID = 2026
    const val EXTRA_NOTIFICATION_SHAYARI_ID = "extra_notification_shayari_id"

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun pickShayariOfTheDay(
        allShayari: List<ShayariEntity>,
        calendar: Calendar = Calendar.getInstance()
    ): ShayariEntity? {
        if (allShayari.isEmpty()) return null
        val pool = allShayari.filter { it.isFeatured }.ifEmpty { allShayari }
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        return pool[dayOfYear % pool.size]
    }

    fun computeNextTriggerTimeMillis(
        hour: Int,
        minute: Int,
        nowMillis: Long = System.currentTimeMillis()
    ): Long {
        val target = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.timeInMillis <= nowMillis) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis
    }

    fun formatTimeLabelHi(hour: Int, minute: Int): String {
        val period = when (hour) {
            in 4..11 -> "सुबह"
            in 12..15 -> "दोपहर"
            in 16..19 -> "शाम"
            else -> "रात"
        }
        val hour12 = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return String.format(Locale.getDefault(), "%s %02d:%02d", period, hour12, minute)
    }

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = context.getString(R.string.notification_channel_desc)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun showShayariOfTheDayNotification(
        context: Context,
        shayari: ShayariEntity
    ): Boolean {
        if (!hasNotificationPermission(context)) return false
        ensureNotificationChannel(context)

        val category = ShayariCategory.fromId(shayari.categoryId)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NOTIFICATION_SHAYARI_ID, shayari.id)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            shayari.id,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val firstLine = shayari.couplet.lineSequence().firstOrNull()?.trim().orEmpty()
        val expandedText = "${shayari.couplet}\n— ${shayari.poet} (${category.titleHi})"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("${context.getString(R.string.featured_badge)} • ${shayari.poet}")
            .setContentText(firstLine.ifBlank { shayari.couplet })
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(expandedText)
                    .setSummaryText(category.titleHi)
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }
}
