package fq.router2.life_cycle;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import fq.router2.utils.LoggedBroadcastReceiver;

public class LaunchingIntent extends Intent {

    public final static String ACTION_LAUNCHING = "Launching";

    public LaunchingIntent(String status, int progress) {
        setAction(ACTION_LAUNCHING);
        putExtra("status", status);
        putExtra("progress", progress);
    }

    public static void register(final Handler handler) {
        handler.getBaseContext().registerReceiver(new LoggedBroadcastReceiver() {
            @Override
            public void handle(Context context, Intent intent) {
                handler.updateStatus(intent.getStringExtra("status"), intent.getIntExtra("progress", 0));
            }
        }, new IntentFilter(ACTION_LAUNCHING));
    }

    public static interface Handler {
        void updateStatus(String status, int progress);

        Context getBaseContext();
    }
}
