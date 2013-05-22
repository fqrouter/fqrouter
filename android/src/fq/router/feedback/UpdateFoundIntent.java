package fq.router.feedback;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class UpdateFoundIntent extends Intent {
    private final static String ACTION_UPDATE_FOUND = "UpdateFound";

    public UpdateFoundIntent(String latestVersion, String upgradeUrl) {
        setAction(ACTION_UPDATE_FOUND);
        putExtra("latestVersion", latestVersion);
        putExtra("upgradeUrl", upgradeUrl);
    }

    public static void register(final Handler handler) {
        handler.getBaseContext().registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                handler.onUpdateFound(intent.getStringExtra("latestVersion"), intent.getStringExtra("upgradeUrl"));
            }
        }, new IntentFilter(ACTION_UPDATE_FOUND));
    }

    public static interface Handler {
        void onUpdateFound(String latestVersion, String upgradeUrl);

        Context getBaseContext();
    }
}
