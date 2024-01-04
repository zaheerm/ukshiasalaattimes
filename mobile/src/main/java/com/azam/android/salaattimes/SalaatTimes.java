package com.azam.android.salaattimes;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.util.Log;
import android.location.LocationManager;

import androidx.core.app.AlarmManagerCompat;

/**
 * Created by zaheer on 9/14/14.
 */
public class SalaatTimes {
    private final static String LOG_TAG = "salaat_times";
    public final static String SALAAT_NAME = "com.azam.android.salaattimes.salaat_name";
    public final static String SALAAT_TIME_STR = "com.azam.android.salaattimes.salaat_time_str";
    public final static String SALAAT_TIME = "com.azam.android.salaattimes.salaat_time";

    private SQLiteOpenHelper openHelper;

    private Context context;

    public SalaatTimes(SQLiteOpenHelper openHelper, Context context) {
        this.openHelper = openHelper;
        this.context = context;

    }

    public Location getLastKnownLocation() throws SecurityException {
        LocationManager mLocationManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
        List<String> providers = mLocationManager.getProviders(true);
        Location bestLocation = null;
        for (String provider : providers) {
            Location l = mLocationManager.getLastKnownLocation(provider);
            if (l == null) {
                continue;
            }
            if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                // Found best last known location: %s", l);
                bestLocation = l;
            }
        }
        return bestLocation;
    }

    public static SalaatTimes build(Context context) {
        DatabaseHelper myDbHelper = new DatabaseHelper(context);

        myDbHelper.createDataBase();

        myDbHelper.openDataBase();
        return new SalaatTimes(myDbHelper, context);
    }

    public void close() {
        openHelper.close();
    }

    public Entry computeEntry(Calendar day) throws SecurityException {
        TimeZone tz = TimeZone.getDefault();
        // Test Prayer times here
        ComputedSalaatTimes prayers = new ComputedSalaatTimes();
        Location l = getLastKnownLocation();
        double latitude;
        double longitude;
        if (l != null) {
            latitude = l.getLatitude();
            longitude = l.getLongitude();
            Log.i(LOG_TAG, "Using latitude " + latitude + " longitude " + longitude);
        } else {
            return null;
        }
        prayers.setTimeFormat(prayers.Time24);
        prayers.setCalcMethod(prayers.Jafari);
        prayers.setAsrJuristic(prayers.Shafii);
        prayers.setAdjustHighLats(prayers.AngleBased);
        int[] offsets = {0, 0, 0, 0, 0, 0, 0}; // {Fajr,Sunrise,Dhuhr,Asr,Sunset,Maghrib,Isha}
        prayers.tune(offsets);
        double todayOffset = TimeUnit.HOURS.convert(tz.getOffset(day.getTimeInMillis()), TimeUnit.MILLISECONDS);
        boolean dst = false; // offset will take care of dst

        ArrayList<String> prayerTimes = prayers.getPrayerTimes(day,
                latitude, longitude, todayOffset);
        Calendar tomorrow = (Calendar)day.clone();
        tomorrow.add(Calendar.DAY_OF_MONTH, 1);
        double tomorrowOffset = TimeUnit.HOURS.convert(tz.getOffset(tomorrow.getTimeInMillis()), TimeUnit.MILLISECONDS);

        ArrayList<String> tomorrowPrayerTimes = prayers.getPrayerTimes(tomorrow,
                latitude, longitude, tomorrowOffset);
        Entry retVal = new Entry(
                Entry.computeImsaak(prayerTimes.get(0), dst),
                Entry.reformatString(prayerTimes.get(0), dst),
                Entry.reformatString(prayerTimes.get(1), dst),
                Entry.reformatString(prayerTimes.get(2), dst),
                Entry.reformatString(prayerTimes.get(4), dst),
                Entry.reformatString(prayerTimes.get(5), dst),
                Entry.reformatString(tomorrowPrayerTimes.get(0), dst));
        return retVal;
    }

    public Entry getEntry(Calendar day, String city) throws SecurityException {
        if (city.equals("uselocation")) return computeEntry(day);
        TimeZone tz = TimeZone.getTimeZone("Europe/London");
        Log.d(LOG_TAG, "day is " + day.get(Calendar.DAY_OF_MONTH) + " month is " + day.get(Calendar.MONTH) + " city is " + city);
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
                Entry.reformatImsaak(city, c.getString(0), dst),
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
        Log.i(LOG_TAG, "About to schedule next salaat");
        Salaat nextSalaat = getNextSalaat(context, Calendar.getInstance());
        if (nextSalaat != null) {
            Intent notificationIntent = new Intent(context, NotificationPublisher.class);
            notificationIntent.putExtra(SALAAT_NAME, nextSalaat.getSalaatName());
            notificationIntent.putExtra(SALAAT_TIME_STR, nextSalaat.getSalaatTimeAsString());
            notificationIntent.putExtra(SALAAT_TIME, nextSalaat.getSalaatTime().getTimeInMillis());
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            long futureInMillis = nextSalaat.getSalaatTime().getTimeInMillis();
            Log.i(LOG_TAG, "Scheduling next notification for " + String.valueOf(futureInMillis) + " ( " + nextSalaat.toString() + " )");
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            // Cancel existing alarms with same intent
            alarmManager.cancel(pendingIntent);
            boolean hasExactAlarmPermission = true;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                hasExactAlarmPermission = alarmManager.canScheduleExactAlarms();
            }
            if (hasExactAlarmPermission) {
                AlarmManagerCompat.setExactAndAllowWhileIdle(alarmManager, AlarmManager.RTC_WAKEUP, futureInMillis, pendingIntent);
            } else {
                Log.w(LOG_TAG, "No exact alarm permission");
            }
            Log.i(LOG_TAG, "Finished scheduling next salaat");
        }
    }



    public Salaat getNextSalaat(Context context, Calendar now) throws SecurityException {
        SharedPreferences preferences = context.getSharedPreferences("salaat", 0);
        String city = preferences.getString("city", "London");
        Calendar salaat = (Calendar) now.clone();
        String salaatName = null;
        if (!city.equals("uselocation")) salaat.setTimeZone(TimeZone.getTimeZone("Europe/London"));
        boolean found = false;
        Entry entry = getEntry(now, city);
        try {
            if (entry != null) {
                for (int viewId : new int[]{
                        R.id.fajr_value,
                        R.id.zohr_value,
                        R.id.maghrib_value,
                }) {
                    String salaatTime = entry.getSalaat(viewId);
                    String[] time_split = salaatTime.split(":");
                    int hour = Integer.valueOf(time_split[0]);
                    int minute = Integer.valueOf(time_split[1]);
                    salaat.set(Calendar.HOUR_OF_DAY, hour);
                    salaat.set(Calendar.MINUTE, minute);
                    salaat.set(Calendar.SECOND, 0);
                    salaat.set(Calendar.MILLISECOND, 0);

                    Log.d(LOG_TAG, "Comparing " + salaat.toString() + " to " + now.toString());
                    if (now.compareTo(salaat) < 0) {
                        Log.d(LOG_TAG, "Salaat " + String.valueOf(viewId) + " is after now");
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

                }
            } else return null;
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
                    if (now.compareTo(salaat) < 0) {
                        Log.w(LOG_TAG, "No salaat today is after now");
                        salaatName = "Fajr";
                    } else {
                        Log.w(LOG_TAG, "You're set in another timezone and in fact next salaat is after tomorrow fajr");
                        salaatName = "Fajr";
                    }

                } catch (Exception e) {
                    return null;
                }

            }
            return new Salaat(salaatName, salaat, city.equals("uselocation"));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
