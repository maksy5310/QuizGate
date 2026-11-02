package com.tvguard.tutor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Prefs p = new Prefs(context);
            if (p.isRunning()) {
                context.startService(new Intent(context, MonitorService.class));
            }
        }
    }
}
