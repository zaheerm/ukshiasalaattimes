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
        Data data = Data.getData(context);
        data.scheduleNextSalaatNotification(context);
        data.close();
    }
}
