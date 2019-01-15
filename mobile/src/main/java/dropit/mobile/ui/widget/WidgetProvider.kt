package dropit.mobile.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import dropit.mobile.R
import dropit.mobile.ui.sending.SendFileActivity

class WidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id ->
            val pendingIntent = Intent(context, SendFileActivity::class.java)
                .apply {
                    putExtra("sendClipboard", true)
                }.let {
                    PendingIntent.getActivity(context, 0, it, 0)
                }
            val views = RemoteViews(context.packageName, R.layout.clipboard_widget)
            views.setOnClickPendingIntent(R.id.button, pendingIntent)
            appWidgetManager.updateAppWidget(id, views)
        }
    }
}