package me.rerere.rikkahub.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.rerere.rikkahub.R
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class LuWidgetProvider : AppWidgetProvider() {
    private fun baseViews(context: Context): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.lu_widget)
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        if (intent != null) {
            val pi = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pi)
        }
        return views
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id ->
            val views = baseViews(context)
            views.setTextViewText(R.id.widget_text, "去家里看一眼…")
            appWidgetManager.updateAppWidget(id, views)
        }
        if (appWidgetIds.isEmpty()) return
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            val line = try {
                val conn = URL("http://101.43.78.19:5002/api/mobile?token=lu-writes-home-5002").openConnection() as HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                val o = JSONObject(body)
                if (o.optBoolean("ok", false)) {
                    fun g(vararg keys: String): String {
                        for (k in keys) { val v = o.optString(k); if (v.isNotBlank()) return v }
                        return ""
                    }
                    val sb = StringBuilder()
                    val room = g("room", "place")
                    val act = g("activity", "doing", "action")
                    val chip = g("chip", "need", "status")
                    if (room.isNotBlank()) sb.append(room)
                    if (act.isNotBlank()) { if (sb.isNotEmpty()) sb.append(" · "); sb.append(act) }
                    if (chip.isNotBlank()) { if (sb.isNotEmpty()) sb.append(" · "); sb.append(chip) }
                    if (sb.isEmpty()) sb.append("在家")
                    sb.toString()
                } else "门关着"
            } catch (e: Exception) {
                "连不上家…"
            }
            appWidgetIds.forEach { id ->
                val views = baseViews(context)
                views.setTextViewText(R.id.widget_text, line)
                appWidgetManager.updateAppWidget(id, views)
            }
        }
    }
}
