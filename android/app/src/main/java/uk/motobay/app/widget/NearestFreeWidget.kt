package uk.motobay.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import kotlinx.coroutines.launch
import uk.motobay.app.MainActivity
import uk.motobay.app.MotoBayApplication
import uk.motobay.app.R
import uk.motobay.app.alerts.ChargeAlerts
import uk.motobay.app.data.Bay
import uk.motobay.app.data.ChargedHours
import uk.motobay.app.data.Fee
import uk.motobay.app.data.LatLon
import uk.motobay.app.data.clockText
import uk.motobay.app.data.isFreeAt
import uk.motobay.app.util.distanceMetres
import uk.motobay.app.util.formatDistance
import java.time.LocalDateTime

/** The nearest bay that's free to park in right now, and how far it is. */
data class NearestFree(val bay: Bay, val metres: Double, val detail: String)

/** How far the widget looks. Further than this, it says there's nothing near. */
const val WIDGET_RADIUS_METRES = 5_000.0

/**
 * Free bays for everyone; Supporters also get paid bays that aren't being charged at
 * the moment, as with the "Free now" filter.
 */
fun nearestFree(bays: Collection<Bay>, from: LatLon, now: LocalDateTime, supporter: Boolean): NearestFree? =
    bays.asSequence()
        .filter { it.fee == Fee.Free || (supporter && it.isFreeAt(now)) }
        .map { it to distanceMetres(from, it) }
        .filter { it.second <= WIDGET_RADIUS_METRES }
        .minByOrNull { it.second }
        ?.let { (bay, d) ->
            val detail = if (bay.fee == Fee.Free) {
                "free bay"
            } else {
                ChargedHours.parse(bay.charged)?.nextChange(now)?.let { "free until ${clockText(now, it)}" } ?: "free now"
            }
            NearestFree(bay, d, "${formatDistance(d)} · $detail")
        }

/**
 * Home-screen widget: the nearest free bay to where the rider last opened MotoBays. A
 * widget can't use location in the background (the app never asks for that), so it uses
 * the last position the app saw, and says so. Tapping it opens the bay.
 */
class NearestFreeWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        val app = context.applicationContext as MotoBayApplication
        app.appScope.launch {
            try {
                if (app.bays.bays.value.isEmpty()) app.bays.load()
                render(context, manager, ids)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        /** Redraws every placed widget, e.g. after the app gets a new position. */
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, NearestFreeWidget::class.java))
            if (ids.isNotEmpty()) render(context, manager, ids)
        }

        private fun render(context: Context, manager: AppWidgetManager, ids: IntArray) {
            val app = context.applicationContext as MotoBayApplication
            val fix = app.user.settings.value.lastFix
            val result = fix?.let {
                nearestFree(app.allBays.value.values.ifEmpty { app.bays.bays.value.values }, LatLon(it.lat, it.lon), app.now(), app.supporter.state.value.isSupporter)
            }
            val views = RemoteViews(context.packageName, R.layout.widget_nearest_free)
            when {
                fix == null -> {
                    views.setTextViewText(R.id.widget_title, "Open MotoBays to find bays near you")
                    views.setViewVisibility(R.id.widget_detail, View.GONE)
                }
                result == null -> {
                    views.setTextViewText(R.id.widget_title, "No free bay within 5 km")
                    views.setViewVisibility(R.id.widget_detail, View.GONE)
                }
                else -> {
                    views.setTextViewText(R.id.widget_title, result.bay.title)
                    views.setTextViewText(R.id.widget_detail, result.detail)
                    views.setViewVisibility(R.id.widget_detail, View.VISIBLE)
                }
            }
            val open = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            result?.let { open.putExtra(ChargeAlerts.EXTRA_BAY_ID, it.bay.id) }
            views.setOnClickPendingIntent(
                R.id.widget_root,
                PendingIntent.getActivity(context, 1, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE),
            )
            ids.forEach { manager.updateAppWidget(it, views) }
        }
    }
}
