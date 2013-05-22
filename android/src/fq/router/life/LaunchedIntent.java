package fq.router.life;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class LaunchedIntent extends Intent {
    private final static String ACTION_LAUNCHED = "Launched";

    public LaunchedIntent() {
        setAction(ACTION_LAUNCHED);
    }

    public static void register(final Handler handler) {
        handler.getBaseContext().registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                handler.onLaunched();
            }
        }, new IntentFilter(ACTION_LAUNCHED));
    }

    public static interface Handler {
        void onLaunched();

        Context getBaseContext();
    }
}
