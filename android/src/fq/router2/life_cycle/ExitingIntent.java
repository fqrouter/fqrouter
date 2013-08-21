package fq.router2.life_cycle;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import fq.router2.utils.LoggedBroadcastReceiver;

public class ExitingIntent extends Intent {
    private final static String ACTION_EXITING = "Exiting";

    public ExitingIntent() {
        setAction(ACTION_EXITING);
    }

    public static void register(final Handler handler) {
        handler.getBaseContext().registerReceiver(new LoggedBroadcastReceiver() {
            @Override
            public void handle(Context context, Intent intent) {
                handler.onExiting();
            }
        }, new IntentFilter(ACTION_EXITING));
    }

    public static interface Handler {
        void onExiting();

        Context getBaseContext();
    }
}