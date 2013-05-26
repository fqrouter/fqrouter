package fq.router.life;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import fq.router.utils.LoggedBroadcastReceiver;

public class LaunchVpnIntent extends Intent {
    private final static String ACTION_LAUNCH_VPN = "LaunchVpn";

    public LaunchVpnIntent() {
        setAction(ACTION_LAUNCH_VPN);
    }

    public static void register(final Handler handler) {
        handler.getBaseContext().registerReceiver(new LoggedBroadcastReceiver() {
            @Override
            public void handle(Context context, Intent intent) {
                handler.onLaunchVpn();
            }
        }, new IntentFilter(ACTION_LAUNCH_VPN));
    }

    public static interface Handler {
        void onLaunchVpn();

        Context getBaseContext();
    }
}
