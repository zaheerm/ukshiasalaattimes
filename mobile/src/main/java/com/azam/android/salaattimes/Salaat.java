package com.azam.android.salaattimes;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Created by zmerali on 10/8/14.
 */
public class Salaat {
    public String getSalaatName() {
        return salaatName;
    }

    public Calendar getSalaatTime() {
        return salaatTime;
    }

    private String salaatName;
    private Calendar salaatTime;
    private boolean localTimezone;

    public Salaat(String salaatName, Calendar salaatTime, boolean localTimezone) {
        this.salaatName = salaatName;
        this.salaatTime = salaatTime;
        this.localTimezone = localTimezone;
    }

    public int getLabel() {
        if (salaatName.equals("Fajr")) {
            Calendar now = Calendar.getInstance();
            if (now.get(Calendar.DAY_OF_MONTH) == salaatTime.get(Calendar.DAY_OF_MONTH)) {
                return R.id.fajr_value;
            }
            else {
                return R.id.tomorrowfajr_value;
            }
        }
        if (salaatName.equals("Zohr")) return R.id.zohr_value;
        if (salaatName.equals("Maghrib")) return R.id.maghrib_value;
        return 0;
    }

    public String getSalaatTimeAsString() {
        SimpleDateFormat sdf = new SimpleDateFormat("kk:mm");
        if (!localTimezone) sdf.setTimeZone(TimeZone.getTimeZone("Europe/London"));
        return sdf.format(salaatTime.getTime());
    }

    public String toString() {
        return salaatName + " " + getSalaatTimeAsString();
    }
}
