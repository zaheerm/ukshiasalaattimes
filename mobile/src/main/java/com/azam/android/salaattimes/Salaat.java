package com.azam.android.salaattimes;

import java.util.Calendar;

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

    public Salaat(String salaatName, Calendar salaatTime) {
        this.salaatName = salaatName;
        this.salaatTime = salaatTime;
    }
}
