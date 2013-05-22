package fq.router.life;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class ExitedIntent extends Intent {
    private final static String ACTION_EXITED = "Exited";

    public ExitedIntent() {
        setAction(ACTION_EXITED);
    }

    public static void register(final Handler handler) {
        handler.getBaseContext().registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                handler.onExited();
            }
        }, new IntentFilter(ACTION_EXITED));
    }

    public static interface Handler {
        void onExited();

        Context getBaseContext();
    }
}
