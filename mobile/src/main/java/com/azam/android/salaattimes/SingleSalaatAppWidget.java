package com.azam.android.salaattimes;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

import java.util.Calendar;


/**
 * Implementation of App Widget functionality.
 */
public class SingleSalaatAppWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.i("widget", "onUpdate, scheduling next salaat");

        SalaatTimes salaatTimes = SalaatTimes.build(context);
        Salaat salaat = null;
        try {
            salaat = salaatTimes.getNextSalaat(context, Calendar.getInstance());
        } catch (SecurityException e) {}
        if (salaat != null) salaatTimes.scheduleNextSalaatNotification(context);
        salaatTimes.close();

        // There may be multiple widgets active, so update all of them
        if (salaat != null) {
            Log.i("widget", "onUpdate, about to update app widgets");

            for (int i: appWidgetIds) {
                updateAppWidget(context, appWidgetManager, i, salaat);
            }
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
            int appWidgetId, Salaat salaat) {
        Log.i("widget", "updateAppWidget for widget " + appWidgetId);

        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.single_salaat_app_widget);
        views.setTextViewText(R.id.nextsalaat_label, salaat.getSalaatName());
        views.setTextViewText(R.id.nextsalaat_value, salaat.getSalaatTimeAsString());
        Intent intent = new Intent(context, SalaatTimesActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK  | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, appWidgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        views.setOnClickPendingIntent(R.id.nextsalaat_widget, pendingIntent);
        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}


