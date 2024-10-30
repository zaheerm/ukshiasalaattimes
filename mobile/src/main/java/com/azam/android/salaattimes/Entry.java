package com.azam.android.salaattimes;

/**
 * Created by zaheer on 9/14/14.
 */
public class Entry {
    public Entry(
            String imsaak,
            String fajr,
            String sunrise,
            String zohr,
            String sunset,
            String maghrib,
            String tomorrowFajr) {
        this.imsaak = imsaak;
        this.fajr = fajr;
        this.sunrise = sunrise;
        this.zohr = zohr;
        this.sunset = sunset;
        this.maghrib = maghrib;
        this.tomorrowFajr = tomorrowFajr;
    }

    public String getSalaat(int resourceId) {
        switch (resourceId) {
            case R.id.imsaak_value:
                return imsaak;
            case R.id.fajr_value:
                return fajr;
            case R.id.sunrise_value:
                return sunrise;
            case R.id.zohr_value:
                return zohr;
            case R.id.sunset_value:
                return sunset;
            case R.id.maghrib_value:
                return maghrib;
            case R.id.tomorrowfajr_value:
                return tomorrowFajr;
        }
        return "undefined";
    }

    public static String reformatString(String time, boolean dst) {
        String[] time_split = time.split(":");
        int hour = Integer.valueOf(time_split[0]).intValue();
        if (dst) hour = hour + 1;
        String hour_str = (hour < 10) ? "0" + String.valueOf(hour) : String.valueOf(hour);
        return hour_str + ":" + time_split[1];
    }

    public static String reformatImsaak(String city, String time, boolean dst) {
        String[] time_split = time.split(":");
        int hour = Integer.valueOf(time_split[0]).intValue();
        String minute = time_split[1];
        if (dst) hour = hour + 1;
        if (city.equals("Leicester") && dst) {
            // special case, drop imsaak by 10 minutes
            int minute_int = Integer.valueOf(minute).intValue();
            minute_int -= 10;
            if (minute_int < 0) {
                minute_int = 60 - Math.abs(minute_int);
                hour -= 1;
            }
            minute = (minute_int < 10) ? "0" + String.valueOf(minute_int) : String.valueOf(minute_int);

        }
        String hour_str = (hour < 10) ? "0" + String.valueOf(hour) : String.valueOf(hour);
        return hour_str + ":" + minute;
    }

    public static String computeImsaak(String fajr, boolean dst) {
        String[] time_split = fajr.split(":");
        int hour = Integer.valueOf(time_split[0]).intValue();
        String minute = time_split[1];
        if (dst) hour = hour + 1;
        int minute_int = Integer.valueOf(minute).intValue();
        minute_int -= 10;
        if (minute_int < 0) {
            minute_int = 60 - Math.abs(minute_int);
            hour -= 1;
        }
        minute = (minute_int < 10) ? "0" + String.valueOf(minute_int) : String.valueOf(minute_int);

        String hour_str = (hour < 10) ? "0" + String.valueOf(hour) : String.valueOf(hour);
        return hour_str + ":" + minute;
    }

    @Override
    public String toString() {
        return "Fajr: " + fajr + " Zohr: " + zohr + " Maghrib: " + maghrib;
    }
    private String imsaak;
    private String fajr;
    private String sunrise;
    private String zohr;
    private String sunset;
    private String maghrib;
    private String tomorrowFajr;
}
