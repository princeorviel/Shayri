package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.ShayariDatabase
import com.example.data.local.ShayariSeedData
import com.example.data.local.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DailyShayariReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefsRepo = UserPreferencesRepository(appContext)
                val prefs = prefsRepo.preferencesFlow.first()
                if (!prefs.dailyNotificationEnabled) return@launch

                val db = ShayariDatabase.getInstance(appContext)
                val stored = runCatching { db.shayariDao().observeAllShayari().first() }
                    .getOrDefault(emptyList())
                val sourceList = stored.ifEmpty { ShayariSeedData.allShayari }

                val todayShayari = DailyShayariNotificationHelper.pickShayariOfTheDay(sourceList)
                if (todayShayari != null) {
                    DailyShayariNotificationHelper.showShayariOfTheDayNotification(
                        context = appContext,
                        shayari = todayShayari
                    )
                }

                // Chain next day's alarm at the user's configured time
                DailyShayariScheduler.scheduleDailyNotification(
                    context = appContext,
                    hour = prefs.dailyNotificationHour,
                    minute = prefs.dailyNotificationMinute
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
