package com.azam.android.salaattimes;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

import java.util.Calendar;

public class NotificationPublisher extends BroadcastReceiver {

    private static String NOTIFICATION_ID = "salaat-notification";

    public NotificationPublisher() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("salaattimes_notification", "Received notification");
        NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        String salaat_name = intent.getStringExtra(Data.SALAAT_NAME);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle(salaat_name + " Salaat Time Now")
                        .setContentText("Time to pray " + salaat_name);
        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(context, SalaatTimes.class);

// The stack builder object will contain an artificial back stack for the
// started Activity.
// This ensures that navigating backward from the Activity leads out of
// your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
// Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(SalaatTimes.class);
// Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        builder.setContentIntent(resultPendingIntent);
// mId allows you to update the notification later on.
        notificationManager.notify(NOTIFICATION_ID, 0, builder.build());
        Data data = Data.getData(context);
        data.scheduleNextSalaatNotification(context);
        Salaat salaat = data.getNextSalaat(context, Calendar.getInstance());
        data.close();
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.single_salaat_app_widget);
        views.setTextViewText(R.id.nextsalaat_label, salaat.getSalaatName());
        views.setTextViewText(R.id.nextsalaat_value, salaat.getSalaatTimeAsString());
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context, SingleSalaatAppWidget.class);
        manager.updateAppWidget(thisWidget, views);

    }

    public static void cancel(Context context) {
        NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID, 0);
    }
}
