package com.azam.android.salaattimes;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    public BootReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("bootreceiver", "At boot, scheduling next salaat");
        SalaatTimes salaatTimes = SalaatTimes.build(context);
        salaatTimes.scheduleNextSalaatNotification(context);
        salaatTimes.close();
    }
}
