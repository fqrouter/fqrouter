package fq.router2.free_internet;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import fq.router2.utils.LoggedBroadcastReceiver;

public class FreeInternetChangedIntent extends Intent {

    private final static String ACTION_FREE_INTERNET_CHANGED = "FreeInternetChanged";

    public FreeInternetChangedIntent(boolean isConnected) {
        setAction(ACTION_FREE_INTERNET_CHANGED);
        putExtra("isConnected", isConnected);
    }

    public static void register(final Handler handler) {
        handler.getBaseContext().registerReceiver(new LoggedBroadcastReceiver() {
            @Override
            public void handle(Context context, Intent intent) {
                handler.onFreeInternetChanged(intent.getBooleanExtra("isConnected", false));
            }
        }, new IntentFilter(ACTION_FREE_INTERNET_CHANGED));
    }

    public static interface Handler {
        void onFreeInternetChanged(boolean isConnected);

        Context getBaseContext();
    }
}
