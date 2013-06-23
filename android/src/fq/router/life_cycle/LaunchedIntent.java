package fq.router.life_cycle;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import fq.router.utils.LoggedBroadcastReceiver;

public class LaunchedIntent extends Intent {
    private final static String ACTION_LAUNCHED = "Launched";

    public LaunchedIntent(boolean isVpnMode) {
        setAction(ACTION_LAUNCHED);
        putExtra("isVpnMode", isVpnMode);
    }

    public static void register(final Handler handler) {
        handler.getBaseContext().registerReceiver(new LoggedBroadcastReceiver() {
            @Override
            public void handle(Context context, Intent intent) {
                handler.onLaunched(intent.getBooleanExtra("isVpnMode", false));
            }
        }, new IntentFilter(ACTION_LAUNCHED));
    }

    public static interface Handler {
        void onLaunched(boolean isVpnMode);

        Context getBaseContext();
    }
}
