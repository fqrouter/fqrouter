package fq.router2.feedback;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import fq.router2.utils.LoggedBroadcastReceiver;

public class UpdateFoundIntent extends Intent {
    private final static String ACTION_UPDATE_FOUND = "UpdateFound";

    public UpdateFoundIntent(String latestVersion, String upgradeUrl) {
        setAction(ACTION_UPDATE_FOUND);
        putExtra("latestVersion", latestVersion);
        putExtra("upgradeUrl", upgradeUrl);
    }

    public static void register(final Handler handler) {
        handler.getBaseContext().registerReceiver(new LoggedBroadcastReceiver() {
            @Override
            public void handle(Context context, Intent intent) {
                handler.onUpdateFound(intent.getStringExtra("latestVersion"), intent.getStringExtra("upgradeUrl"));
            }
        }, new IntentFilter(ACTION_UPDATE_FOUND));
    }

    public static interface Handler {
        void onUpdateFound(String latestVersion, String upgradeUrl);

        Context getBaseContext();
    }
}
