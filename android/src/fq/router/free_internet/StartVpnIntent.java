package fq.router.free_internet;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import fq.router.utils.LoggedBroadcastReceiver;

public class StartVpnIntent extends Intent {
    private final static String ACTION_START_VPN = "StartVpn";

    public StartVpnIntent() {
        setAction(ACTION_START_VPN);
    }

    public static void register(final Handler handler) {
        handler.getBaseContext().registerReceiver(new LoggedBroadcastReceiver() {
            @Override
            public void handle(Context context, Intent intent) {
                handler.onStartVpn();
            }
        }, new IntentFilter(ACTION_START_VPN));
    }

    public static interface Handler {
        void onStartVpn();

        Context getBaseContext();
    }
}
