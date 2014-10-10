package com.azam.android.salaattimes;

import java.io.IOException;
import java.util.Calendar;
import java.util.TimeZone;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by zaheer on 9/14/14.
 */
public class Data {
    public final static String SALAAT_NAME = "com.azam.android.salaattimes.salaat_name";
    private SQLiteOpenHelper openHelper;
    public Data(SQLiteOpenHelper openHelper) {
        this.openHelper = openHelper;
    }

    public static Data getData(Context context) {
        DatabaseHelper myDbHelper = new DatabaseHelper(context);
        try {
            myDbHelper.createDataBase();
        } catch (IOException ioe) {
            throw new Error("Unable to create database");
        }

        try {
            myDbHelper.openDataBase();
        }catch(SQLException sqle){
            throw sqle;
        }
        return new Data(myDbHelper);
    }

    public void close() {
        openHelper.close();
    }

    public Entry getEntry(Calendar day, String city) {
        TimeZone tz = TimeZone.getTimeZone("Europe/London");
        Log.d("sqlitedata", "day is " + day.get(Calendar.DAY_OF_MONTH) + " month is " + day.get(Calendar.MONTH));
        SQLiteDatabase db = openHelper.getReadableDatabase();
        String month = String.valueOf(day.get(Calendar.MONTH) + 1);
        String dom = String.valueOf(day.get(Calendar.DAY_OF_MONTH));
        String query = "SELECT imsaak, fajr, sunrise, zohr, sunset, maghrib, tomorrowfajr from salaat_times WHERE city='" + city + "' AND month=" + month + " AND day=" + dom;
        Cursor c = db.rawQuery(query, new String[]{});
        c.moveToFirst();
        Calendar tomorrow = (Calendar)day.clone();
        tomorrow.add(Calendar.DAY_OF_MONTH, 1);
        boolean dst = false;
        if (tz.inDaylightTime(tomorrow.getTime())) {
            dst = true;
        }
        String tomorrowFajr = Entry.reformatString(c.getString(6), dst);
        dst = false;
        if (tz.inDaylightTime(day.getTime())) {
            dst = true;
        }
        Entry retVal = new Entry(
                Entry.reformatString(c.getString(0), dst),
                Entry.reformatString(c.getString(1), dst),
                Entry.reformatString(c.getString(2), dst),
                Entry.reformatString(c.getString(3), dst),
                Entry.reformatString(c.getString(4), dst),
                Entry.reformatString(c.getString(5), dst),
                tomorrowFajr);
        db.close();
        return retVal;
    }

    public void scheduleNextSalaatNotification(Context context) {
        Salaat nextSalaat = getNextSalaat(context);
        Intent notificationIntent = new Intent(context, NotificationPublisher.class);
        notificationIntent.putExtra(SALAAT_NAME, nextSalaat.getSalaatName());
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        long futureInMillis = nextSalaat.getSalaatTime().getTimeInMillis();
        Log.i("sqldata", "Scheduled next notification for " + String.valueOf(futureInMillis) + " ( " + nextSalaat.toString() + " )");
        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, futureInMillis, pendingIntent);

    }

    public static void cancelNextSalaatNotification(Context context) {
        Intent notificationIntent = new Intent(context, NotificationPublisher.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Log.i("sqldata", "Cancelling next salaat notification");
        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
    }

    public Salaat getNextSalaat(Context context) {
        SharedPreferences preferences = context.getSharedPreferences("salaat", 0);
        String city = preferences.getString("city", "London");
        Calendar now = Calendar.getInstance();
        Calendar salaat = (Calendar)now.clone();
        String salaatName = null;
        salaat.setTimeZone(TimeZone.getTimeZone("Europe/London"));
        boolean found = false;
        Entry entry = getEntry(now, city);
        for(int viewId : new int[]{
                R.id.fajr_value,
                R.id.zohr_value,
                R.id.maghrib_value,
        }) {
            try {
                String salaatTime = entry.getSalaat(viewId);
                String[] time_split = salaatTime.split(":");
                int hour = Integer.valueOf(time_split[0]);
                int minute = Integer.valueOf(time_split[1]);
                salaat.set(Calendar.HOUR_OF_DAY, hour);
                salaat.set(Calendar.MINUTE, minute);
                salaat.set(Calendar.SECOND, 0);
                salaat.set(Calendar.MILLISECOND, 0);

                Log.d("salaattimes_data", "Comparing " + salaat.toString() + " to " + now.toString());
                if (now.compareTo(salaat) < 0) {
                    Log.d("salaattimes_data", "Salaat " + String.valueOf(viewId) + " is after now");
                    found = true;
                    switch (viewId) {
                        case R.id.fajr_value:
                            salaatName = "Fajr";
                            break;
                        case R.id.zohr_value:
                            salaatName = "Zohr";
                            break;
                        case R.id.maghrib_value:
                            salaatName = "Maghrib";
                    }
                    break;
                }
            } catch (Exception e) {
                Log.w("salaattimes_data", "Got exception " + e.toString());
            }
        }
        if (!found) {
            try {
                String salaatTime = entry.getSalaat(R.id.tomorrowfajr_value);
                String[] time_split = salaatTime.split(":");
                int hour = Integer.valueOf(time_split[0]);
                int minute = Integer.valueOf(time_split[1]);
                salaat.add(Calendar.DAY_OF_MONTH, 1);
                salaat.set(Calendar.HOUR_OF_DAY, hour);
                salaat.set(Calendar.MINUTE, minute);
                salaat.set(Calendar.SECOND, 0);
                salaat.set(Calendar.MILLISECOND, 0);
                Log.w("salaattimes_data", "No salaat today is after now");
                salaatName = "Fajr";
            } catch (Exception e) {}

        }
        return new Salaat(salaatName, salaat);
    }
}
