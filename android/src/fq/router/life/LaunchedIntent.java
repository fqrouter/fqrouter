package fq.router.life;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import fq.router.utils.LoggedBroadcastReceiver;

public class LaunchedIntent extends Intent {
    private final static String ACTION_LAUNCHED = "Launched";

    public LaunchedIntent() {
        setAction(ACTION_LAUNCHED);
    }

    public static void register(final Handler handler) {
        handler.getBaseContext().registerReceiver(new LoggedBroadcastReceiver() {
            @Override
            public void handle(Context context, Intent intent) {
                handler.onLaunched();
            }
        }, new IntentFilter(ACTION_LAUNCHED));
    }

    public static interface Handler {
        void onLaunched();

        Context getBaseContext();
    }
}
