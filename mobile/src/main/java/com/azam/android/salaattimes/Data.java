package com.azam.android.salaattimes;

import java.util.Calendar;
import java.util.TimeZone;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by zaheer on 9/14/14.
 */
public class Data {
    private SQLiteOpenHelper openHelper;
    public Data(SQLiteOpenHelper openHelper) {
        this.openHelper = openHelper;
    }
    public Entry getEntry(Calendar day, String city) {
        TimeZone tz = TimeZone.getTimeZone("Europe/London");
        Log.i("sqlitedata", "day is " + day.get(Calendar.DAY_OF_MONTH) + " month is " + day.get(Calendar.MONTH));
        SQLiteDatabase db = openHelper.getReadableDatabase();
        String month = String.valueOf(day.get(Calendar.MONTH) + 1);
        String dom = String.valueOf(day.get(Calendar.DAY_OF_MONTH));
        String query = "SELECT imsaak, fajr, sunrise, zohr, sunset, maghrib, tomorrowfajr from salaat_times WHERE city='" + city + "' AND month=" + month + " AND day=" + dom;
        Log.i("sqlitedata", query);
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
}
