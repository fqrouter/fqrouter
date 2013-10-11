package fq.router2;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import fq.router2.utils.LoggedBroadcastReceiver;

public class SocksVpnConnectedIntent extends Intent{
    public final static String ACTION_SOCKS_VPN_CONNECTED= "SocksVpnConnected";

    public SocksVpnConnectedIntent() {
        setAction(ACTION_SOCKS_VPN_CONNECTED);
    }

    public static void register(final Handler handler) {
        handler.getBaseContext().registerReceiver(new LoggedBroadcastReceiver() {
            @Override
            public void handle(Context context, Intent intent) {
                handler.onReady();
            }
        }, new IntentFilter(ACTION_SOCKS_VPN_CONNECTED));
    }

    public static interface Handler {
        void onReady();

        Context getBaseContext();
    }
}
