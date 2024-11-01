package com.azam.android.salaattimes;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import androidx.core.app.NotificationCompat;

import android.os.Bundle;
import android.util.Log;

import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.Calendar;

import static androidx.core.app.NotificationCompat.PRIORITY_HIGH;

import io.sentry.Sentry;

public class NotificationPublisher extends BroadcastReceiver {

    private static final String NOTIFICATION_ID = "salaat-notification";
    private final static String LOG_TAG = "salaattimes_notify";

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
            int importance = NotificationManager.IMPORTANCE_HIGH;
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
        // ...
        // Obtain the FirebaseAnalytics instance.
        FirebaseAnalytics mFirebaseAnalytics = FirebaseAnalytics.getInstance(context);
        String salaat_name = intent.getStringExtra(SalaatTimes.SALAAT_NAME);
        if (salaat_name != null) salaat_name = "fajr";
        Log.i(LOG_TAG, "Received notification for " + salaat_name);
        SalaatTimes salaatTimes = SalaatTimes.build(context);
        salaatTimes.scheduleNextSalaatNotification(context);
        Salaat salaat = null;
        try {
            salaat = salaatTimes.getNextSalaat(context, Calendar.getInstance());
        } catch (SecurityException e) {
            Sentry.captureException(e);
        }
        salaatTimes.close();
        if (salaat != null && salaat.getSalaatName().equalsIgnoreCase(salaat_name)) {
            Log.w(LOG_TAG, "Next salaat happens to be the same: " + salaat.getSalaatName());
            return;
        }
        SalaatTimesActivity.updateAllWidgets(context);
        SharedPreferences preferences = context.getSharedPreferences("salaat", 0);
        Uri adhanUri = Uri.parse("android.resource://"
                + context.getPackageName() + "/" + R.raw.azan);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannels(notificationManager, adhanUri);
        String channel = "salaat";

        if ((preferences.contains(salaat_name.toLowerCase() + "notify") && preferences.getBoolean(salaat_name.toLowerCase() + "notify", false)) ||
                (!preferences.contains(salaat_name.toLowerCase() + "notify") && preferences.getBoolean("nextsalaatnotify", false))) {
            channel = "salaat-with-adhan";
        }
        Bundle bundle = new Bundle();
        bundle.putString("salaat_name", salaat_name);
        bundle.putString("salaat_time_str", intent.getStringExtra(SalaatTimes.SALAAT_TIME_STR));
        bundle.putLong("salaat_time", intent.getLongExtra(SalaatTimes.SALAAT_TIME, 0));
        bundle.putLong("actual_time", System.currentTimeMillis());
        bundle.putLong("lag", System.currentTimeMillis() - intent.getLongExtra(SalaatTimes.SALAAT_TIME, 0));
        bundle.putString("city", preferences.getString("city", "London"));
        bundle.putBoolean("adhan", channel.equals("salaat-with-adhan"));

        mFirebaseAnalytics.logEvent("salaat_notify", bundle);
        Log.i(LOG_TAG, "Displaying notification for " + salaat_name + " on channel " + channel);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channel)
                .setDefaults(Notification.DEFAULT_VIBRATE)
                .setSmallIcon(R.drawable.ic_stat_kaaba)
                .setContentTitle(salaat_name + " Salaat Time Now")
                .setContentText("Time to pray " + salaat_name)
                .setLights(Color.GREEN, 500, 500)
                .setPriority(PRIORITY_HIGH);

        if (channel.equals("salaat-with-adhan"))
            builder.setSound(adhanUri);

        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(context, SalaatTimesActivity.class);

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(SalaatTimesActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_MUTABLE
                );
        builder.setContentIntent(resultPendingIntent);
        Notification notification = builder.build();
        // mId allows you to update the notification later on.
        notificationManager.notify(NOTIFICATION_ID, 0, notification);


    }

    public static void cancel(Context context) {
        NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID, 0);
        initializeChannels(context);
    }
}
