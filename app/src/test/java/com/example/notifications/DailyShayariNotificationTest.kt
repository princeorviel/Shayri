package com.example.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ShayariSeedData
import com.example.data.model.ShayariCategory
import com.example.data.model.ShayariEntity
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DailyShayariNotificationTest {

    private lateinit var context: Application

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `computeNextTriggerTimeMillis schedules today when time is in future`() {
        val nowCal = Calendar.getInstance().apply {
            set(2026, Calendar.APRIL, 10, 8, 15, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val triggerMillis = DailyShayariNotificationHelper.computeNextTriggerTimeMillis(
            hour = 9,
            minute = 30,
            nowMillis = nowCal.timeInMillis
        )
        val triggerCal = Calendar.getInstance().apply { timeInMillis = triggerMillis }
        assertEquals(2026, triggerCal.get(Calendar.YEAR))
        assertEquals(Calendar.APRIL, triggerCal.get(Calendar.MONTH))
        assertEquals(10, triggerCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(9, triggerCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, triggerCal.get(Calendar.MINUTE))
        assertEquals(0, triggerCal.get(Calendar.SECOND))
    }

    @Test
    fun `computeNextTriggerTimeMillis rolls to tomorrow when time has passed`() {
        val nowCal = Calendar.getInstance().apply {
            set(2026, Calendar.APRIL, 10, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val triggerMillis = DailyShayariNotificationHelper.computeNextTriggerTimeMillis(
            hour = 9,
            minute = 0,
            nowMillis = nowCal.timeInMillis
        )
        val triggerCal = Calendar.getInstance().apply { timeInMillis = triggerMillis }
        assertEquals(Calendar.APRIL, triggerCal.get(Calendar.MONTH))
        assertEquals(11, triggerCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(9, triggerCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, triggerCal.get(Calendar.MINUTE))
    }

    @Test
    fun `pickShayariOfTheDay chooses deterministically from featured pool and falls back safely`() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_YEAR, 42)
        }
        val featuredPick = DailyShayariNotificationHelper.pickShayariOfTheDay(
            allShayari = ShayariSeedData.allShayari,
            calendar = calendar
        )
        assertNotNull(featuredPick)
        assertTrue(featuredPick!!.isFeatured)

        val nonFeatured = listOf(
            ShayariEntity(
                id = 999,
                couplet = "पहली पंक्ति\nदूसरी पंक्ति",
                poet = "टेस्ट शायर",
                categoryId = ShayariCategory.MOHABBAT.id,
                moodTag = "टेस्ट",
                isFeatured = false
            )
        )
        val fallbackPick = DailyShayariNotificationHelper.pickShayariOfTheDay(nonFeatured, calendar)
        assertEquals(999, fallbackPick?.id)
        assertNull(DailyShayariNotificationHelper.pickShayariOfTheDay(emptyList(), calendar))
    }

    @Test
    fun `scheduler arms and cancels AlarmManager alarm for DailyShayariReceiver`() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarmManager = Shadows.shadowOf(alarmManager)

        val now = Calendar.getInstance().apply {
            set(2026, Calendar.APRIL, 10, 7, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val scheduledAt = DailyShayariScheduler.scheduleDailyNotification(
            context = context,
            hour = 20,
            minute = 45,
            nowMillis = now
        )

        val scheduledAlarm = requireNotNull(shadowAlarmManager.nextScheduledAlarm)
        assertEquals(scheduledAt, scheduledAlarm.triggerAtTime)
        val pendingIntentShadow = Shadows.shadowOf(scheduledAlarm.operation)
        assertEquals(
            DailyShayariReceiver::class.java.name,
            pendingIntentShadow.savedIntent.component?.className
        )

        DailyShayariScheduler.cancelDailyNotification(context)
        assertNull(shadowAlarmManager.nextScheduledAlarm)
    }

    @Test
    fun `showShayariOfTheDayNotification respects permission and posts rich notification with deep link`() {
        val target = ShayariSeedData.allShayari.first()
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadowNotificationManager = Shadows.shadowOf(notificationManager)

        // Denied initially on API 36 -> returns false
        Shadows.shadowOf(context).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertFalse(DailyShayariNotificationHelper.showShayariOfTheDayNotification(context, target))
        assertTrue(shadowNotificationManager.allNotifications.isEmpty())

        // Granted -> posts notification with channel and deep link shayariId
        Shadows.shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val posted = DailyShayariNotificationHelper.showShayariOfTheDayNotification(context, target)
        assertTrue(posted)

        val channel = notificationManager.getNotificationChannel(DailyShayariNotificationHelper.CHANNEL_ID)
        assertNotNull(channel)

        val postedNotification = requireNotNull(
            shadowNotificationManager.getNotification(
                DailyShayariNotificationHelper.NOTIFICATION_ID
            )
        )
        assertEquals(
            DailyShayariNotificationHelper.CHANNEL_ID,
            postedNotification.channelId
        )
        val title = postedNotification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val bigText = postedNotification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
        assertTrue(title.contains(target.poet))
        assertTrue(bigText.contains(target.couplet))

        val contentIntentShadow = Shadows.shadowOf(postedNotification.contentIntent)
        assertEquals(
            target.id,
            contentIntentShadow.savedIntent.getIntExtra(
                DailyShayariNotificationHelper.EXTRA_NOTIFICATION_SHAYARI_ID,
                -1
            )
        )
    }
}
