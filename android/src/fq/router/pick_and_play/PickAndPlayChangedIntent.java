package fq.router.pick_and_play;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import fq.router.utils.LoggedBroadcastReceiver;

public class PickAndPlayChangedIntent extends Intent {

    private final static String ACTION_PICK_AND_PLAY_CHANGED = "PickAndPlayChanged";

    public PickAndPlayChangedIntent(boolean isStarted) {
        setAction(ACTION_PICK_AND_PLAY_CHANGED);
        putExtra("isStarted", isStarted);
    }

    public static void register(final Handler handler) {
        handler.getBaseContext().registerReceiver(new LoggedBroadcastReceiver() {
            @Override
            public void handle(Context context, Intent intent) {
                handler.onPickAndPlayChanged(intent.getBooleanExtra("isStarted", false));
            }
        }, new IntentFilter(ACTION_PICK_AND_PLAY_CHANGED));
    }

    public static interface Handler {
        void onPickAndPlayChanged(boolean isStarted);

        Context getBaseContext();
    }
}