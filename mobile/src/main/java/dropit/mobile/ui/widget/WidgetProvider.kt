package dropit.mobile.ui.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import dropit.mobile.R
import dropit.mobile.ui.sending.SendFileActivity

class WidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.clipboard_widget)
            views.setOnClickPendingIntent(R.id.button, SendFileActivity.pendingIntent(context))
            appWidgetManager.updateAppWidget(id, views)
        }
    }
}