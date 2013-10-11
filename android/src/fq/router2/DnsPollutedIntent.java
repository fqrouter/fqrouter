package fq.router2;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import fq.router2.utils.LoggedBroadcastReceiver;

import java.util.Date;

public class DnsPollutedIntent extends Intent {

    private final static String ACTION_DNS_POLLUTED = "DnsPolluted";

    public DnsPollutedIntent(long pollutedAt) {
        setAction(ACTION_DNS_POLLUTED);
        putExtra("pollutedAt", pollutedAt);
    }

    public static void register(final Handler handler) {
        handler.getBaseContext().registerReceiver(new LoggedBroadcastReceiver() {
            @Override
            public void handle(Context context, Intent intent) {
                handler.onDnsPolluted(intent.getLongExtra("pollutedAt", 0));
            }
        }, new IntentFilter(ACTION_DNS_POLLUTED));
    }

    public static interface Handler {
        void onDnsPolluted(long pollutedAt);

        Context getBaseContext();
    }
}
