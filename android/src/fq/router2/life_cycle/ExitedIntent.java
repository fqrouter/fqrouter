package fq.router2.life_cycle;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import fq.router2.utils.LoggedBroadcastReceiver;

public class ExitedIntent extends Intent {
    private final static String ACTION_EXITED = "Exited";

    public ExitedIntent() {
        setAction(ACTION_EXITED);
    }

    public static void register(final Handler handler) {
        handler.getBaseContext().registerReceiver(new LoggedBroadcastReceiver() {
            @Override
            public void handle(Context context, Intent intent) {
                handler.onExited();
            }
        }, new IntentFilter(ACTION_EXITED));
    }

    public static interface Handler {
        void onExited();

        Context getBaseContext();
    }
}
