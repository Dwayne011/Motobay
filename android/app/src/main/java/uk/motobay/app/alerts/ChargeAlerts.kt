package uk.motobay.app.alerts

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import uk.motobay.app.MainActivity
import uk.motobay.app.MotoBayApplication
import uk.motobay.app.R
import uk.motobay.app.data.Parked
import uk.motobay.app.data.ParkingPlan
import uk.motobay.app.data.clockText
import uk.motobay.app.data.epochMillis
import uk.motobay.app.data.plan

/**
 * The "charging starts soon" reminder for where the rider parked. One alarm at a time,
 * inexact (no exact-alarm permission), allowed to fire while the phone is dozing; Android
 * may deliver it a few minutes late, which the 15-minute lead allows for.
 */
object ChargeAlerts {
    const val CHANNEL = "charge-alerts"
    private const val NOTIFICATION_ID = 1001
    const val EXTRA_BAY_ID = "uk.motobay.app.BAY_ID"

    /** Sets (or clears) the alarm for [parked]. Returns the plan, so callers can say what will happen. */
    fun schedule(context: Context, parked: Parked?): ParkingPlan? {
        val app = context.applicationContext as MotoBayApplication
        val alarms = context.getSystemService(AlarmManager::class.java)
        val pending = alarmIntent(context)
        alarms.cancel(pending)
        if (parked == null) {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
            return null
        }
        val plan = parked.plan(app.now())
        if (plan is ParkingPlan.FreeUntil) {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, plan.alertAt.epochMillis(), pending)
        }
        return plan
    }

    fun canNotify(context: Context): Boolean =
        (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    private fun alarmIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, 0, Intent(context, ChargeAlertReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    internal fun notify(context: Context) {
        val app = context.applicationContext as MotoBayApplication
        val parked = app.user.parked.value ?: return
        val now = app.now()
        // Work out the plan again rather than trusting the alarm (Android may deliver it late).
        val text = when (val p = parked.plan(now)) {
            is ParkingPlan.FreeUntil -> "Charging starts at ${clockText(now, p.startsAt)}. Pay or move your bike."
            is ParkingPlan.ChargedNow -> "Charging has started. Pay or move your bike."
            else -> return
        }
        if (!canNotify(context)) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, "Charge alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "A reminder before charging starts at the bay where you parked"
            },
        )
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).putExtra(EXTRA_BAY_ID, parked.bayId).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_motorcycle)
            .setContentTitle(parked.title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        @Suppress("MissingPermission") // checked in canNotify
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}

/** Fires at the alert time. */
class ChargeAlertReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = ChargeAlerts.notify(context)
}

/** Alarms don't survive a restart, so this sets it again after the phone boots or the app updates. */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val app = context.applicationContext as MotoBayApplication
        ChargeAlerts.schedule(context, app.user.parked.value)
    }
}
