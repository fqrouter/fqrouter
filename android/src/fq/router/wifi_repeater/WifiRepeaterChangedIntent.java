package fq.router.wifi_repeater;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import fq.router.utils.LoggedBroadcastReceiver;

public class WifiRepeaterChangedIntent extends Intent {

    private final static String ACTION_WIFI_REPEATER_CHANGED = "WifiRepeaterChanged";

    public WifiRepeaterChangedIntent(boolean isStarted) {
        setAction(ACTION_WIFI_REPEATER_CHANGED);
        putExtra("isStarted", isStarted);
    }

    public static void register(final Handler handler) {
        handler.getBaseContext().registerReceiver(new LoggedBroadcastReceiver() {
            @Override
            public void handle(Context context, Intent intent) {
                handler.onWifiRepeaterChanged(intent.getBooleanExtra("isStarted", false));
            }
        }, new IntentFilter(ACTION_WIFI_REPEATER_CHANGED));
    }

    public static interface Handler {
        void onWifiRepeaterChanged(boolean isStarted);

        Context getBaseContext();
    }
}
