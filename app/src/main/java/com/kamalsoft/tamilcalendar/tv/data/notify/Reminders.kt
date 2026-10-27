package com.kamalsoft.tamilcalendar.tv.data.notify

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.kamalsoft.tamilcalendar.tv.MainActivity
import com.kamalsoft.tamilcalendar.tv.R
import com.kamalsoft.tamilcalendar.tv.TamilCalendarApp
import com.kamalsoft.tamilcalendar.tv.di.AppContainer
import com.kamalsoft.tamilcalendar.tv.domain.model.AppSettings
import com.kamalsoft.tamilcalendar.tv.domain.model.TamilDayData
import com.kamalsoft.tamilcalendar.tv.domain.model.TamilTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

/** One reminder: when to fire and what to say. */
data class Reminder(val at: LocalDateTime, val titleTa: String, val titleEn: String, val text: String)

/** Reminders for [day] under [s]: 10 min before Rahu Kalam; at the start of each Nalla Neram window. */
fun remindersFor(day: TamilDayData, s: AppSettings): List<Reminder> = buildList {
    if (s.rahuReminder) {
        val r = day.timings.rahuKalam
        add(Reminder(day.date.atTime(r.start).minusMinutes(10), "ராகு காலம் 10 நிமிடத்தில்", "Rahu Kalam in 10 minutes", TamilTime.range(r.start, r.end)))
    }
    if (s.nallaReminder) {
        day.timings.nallaNeram.forEach { w ->
            add(Reminder(day.date.atTime(w.start), "நல்ல நேரம் தொடங்கியது", "Nalla Neram has begun", TamilTime.range(w.start, w.end)))
        }
    }
}

/**
 * Schedules system notifications with AlarmManager (exact, allowed while idle). Rebuilt after boot,
 * settings changes, location changes and each time an alarm fires, always for today and tomorrow.
 */
class ReminderScheduler(private val context: Context, private val container: AppContainer) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val prefs = context.getSharedPreferences("reminders", Context.MODE_PRIVATE)

    fun reschedule() {
        scope.launch { runCatching { scheduleNow() }.onFailure { Log.w("Reminders", "Scheduling failed", it) } }
    }

    private suspend fun scheduleNow() {
        cancelAll()
        val s = container.appSettings.settings.value
        if (!s.rahuReminder && !s.nallaReminder) return
        val session = container.session.value
        val zone = session.zone
        val today = LocalDate.now(zone)
        session.repository.ensureRange(today, today.plusDays(1))
        val now = LocalDateTime.now(zone)
        val codes = mutableSetOf<String>()
        for (d in listOf(today, today.plusDays(1))) {
            val day = session.getDay(d).firstOrNull() ?: continue
            remindersFor(day, s).filter { it.at.isAfter(now) }.forEach { r ->
                val code = (r.at.toString() + r.titleEn).hashCode()
                alarms.setExactCompat(r.at.atZone(zone).toInstant().toEpochMilli(), pending(code, r))
                codes += code.toString()
            }
        }
        prefs.edit().putStringSet(KEY_CODES, codes).apply()
        createChannel(context)
    }

    private fun cancelAll() {
        prefs.getStringSet(KEY_CODES, emptySet()).orEmpty().forEach { alarms.cancel(pending(it.toInt(), null)) }
        prefs.edit().remove(KEY_CODES).apply()
    }

    private fun pending(code: Int, r: Reminder?): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).setAction(ACTION_REMIND)
        if (r != null) intent.putExtra(EXTRA_TITLE, "${r.titleTa} · ${r.titleEn}").putExtra(EXTRA_TEXT, r.text)
        return PendingIntent.getBroadcast(context, code, intent, PendingIntent.FLAG_UPDATE_CURRENT or immutable())
    }

    private fun AlarmManager.setExactCompat(atMillis: Long, pi: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
        else setExact(AlarmManager.RTC_WAKEUP, atMillis, pi)
    }

    companion object {
        const val ACTION_REMIND = "com.kamalsoft.tamilcalendar.tv.REMIND"
        const val EXTRA_TITLE = "title"
        const val EXTRA_TEXT = "text"
        const val CHANNEL = "auspicious_times"
        private const val KEY_CODES = "codes"

        fun immutable() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0

        fun createChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "நல்ல நேரம் / ராகு காலம் · Auspicious times", NotificationManager.IMPORTANCE_HIGH),
            )
        }
    }
}

/** Posts the reminder notification; also re-arms reminders after boot and after each alarm. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val container = (context.applicationContext as TamilCalendarApp).container
        if (intent.action == ReminderScheduler.ACTION_REMIND) {
            ReminderScheduler.createChannel(context)
            val open = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or ReminderScheduler.immutable(),
            )
            val n = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL)
                .setSmallIcon(R.drawable.ic_nav_daily)
                .setContentTitle(intent.getStringExtra(ReminderScheduler.EXTRA_TITLE))
                .setContentText(intent.getStringExtra(ReminderScheduler.EXTRA_TEXT))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
            runCatching { NotificationManagerCompat.from(context).notify(intent.getStringExtra(ReminderScheduler.EXTRA_TITLE).hashCode(), n) }
                .onFailure { Log.w("Reminders", "Notification blocked", it) }
        }
        container.reminders.reschedule()
    }
}

/** In-app banner text for the current minute, or null (shown while the app is open). */
fun bannerFor(day: TamilDayData?, s: AppSettings, now: LocalDateTime): String? {
    day ?: return null
    return remindersFor(day, s).firstOrNull { !now.isBefore(it.at) && now.isBefore(it.at.plusMinutes(2)) }
        ?.let { "${it.titleTa} · ${it.titleEn} — ${it.text}" }
}
