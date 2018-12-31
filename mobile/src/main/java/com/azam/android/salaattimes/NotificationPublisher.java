package com.azam.android.salaattimes;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

import java.util.Calendar;

import static androidx.core.app.NotificationCompat.PRIORITY_DEFAULT;

public class NotificationPublisher extends BroadcastReceiver {

    private static String NOTIFICATION_ID = "salaat-notification";

    public NotificationPublisher() {
    }

    public static void initializeChannels(Context context) {
        NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        Uri adhanUri = Uri.parse("android.resource://"
                + context.getPackageName() + "/" + R.raw.azan);
        createNotificationChannels(notificationManager, adhanUri);
    }
    @TargetApi(26)
    private static void createNotificationChannels(NotificationManager notificationManager, Uri adhan) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("salaat", "Salaat", importance);
            channel.setDescription("Salaat time");
            channel.setLightColor(Color.GREEN);
            channel.enableLights(true);
            channel.enableVibration(true);

            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            notificationManager.createNotificationChannel(channel);
            NotificationChannel adhanChannel = new NotificationChannel("salaat-with-adhan", "Salaat with adhan", importance);
            adhanChannel.setDescription("Salaat time with Adhan (call to prayer) played");
            adhanChannel.setSound(adhan, new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
            adhanChannel.setLightColor(Color.GREEN);
            adhanChannel.enableLights(true);
            notificationManager.createNotificationChannel(adhanChannel);
        }
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("salaattimes_notify", "Received notification");
        SharedPreferences preferences = context.getSharedPreferences("salaat", 0);
        Uri adhanUri = Uri.parse("android.resource://"
                + context.getPackageName() + "/" + R.raw.azan);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannels(notificationManager, adhanUri);
        if (preferences.getBoolean("nextsalaatnotify", true)) {
            String salaat_name = intent.getStringExtra(Data.SALAAT_NAME);
            boolean adhan = preferences.getBoolean("nextsalaatadhan", true);
            String channel = "salaat";
            if (adhan)
                channel = "salaat-with-adhan";
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channel)
                    .setDefaults(Notification.DEFAULT_VIBRATE)
                    .setSmallIcon(R.drawable.ic_stat_kaaba)
                    .setContentTitle(salaat_name + " Salaat Time Now")
                    .setContentText("Time to pray " + salaat_name)
                    .setLights(Color.GREEN, 500, 500)
                    .setPriority(PRIORITY_DEFAULT);

            if (adhan)
                builder.setSound(adhanUri);

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
            Notification notification = builder.build();
            // mId allows you to update the notification later on.
            notificationManager.notify(NOTIFICATION_ID, 0, notification);
        }
        Data data = Data.getData(context);
        data.scheduleNextSalaatNotification(context);
        Salaat salaat = null;
        try {
            salaat = data.getNextSalaat(context, Calendar.getInstance());
        } catch (SecurityException e) {}
        data.close();
        if (salaat != null) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.single_salaat_app_widget);
            views.setTextViewText(R.id.nextsalaat_label, salaat.getSalaatName());
            views.setTextViewText(R.id.nextsalaat_value, salaat.getSalaatTimeAsString());
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            ComponentName thisWidget = new ComponentName(context, SingleSalaatAppWidget.class);
            manager.updateAppWidget(thisWidget, views);
        }

    }

    public static void cancel(Context context) {
        NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID, 0);
        initializeChannels(context);
    }
}
