package fq.router2.life_cycle;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import fq.router2.utils.LoggedBroadcastReceiver;

public class ExitIntent extends Intent {
    private final static String ACTION_EXIT = "Exit";

    public ExitIntent() {
        setAction(ACTION_EXIT);
    }

    public static void register(final Handler handler) {
        handler.getBaseContext().registerReceiver(new LoggedBroadcastReceiver() {
            @Override
            public void handle(Context context, Intent intent) {
                handler.exit();
            }
        }, new IntentFilter(ACTION_EXIT));
    }

    public static interface Handler {
        void exit();

        Context getBaseContext();
    }
}
