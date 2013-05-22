package fq.router.feedback;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class UpdateStatusIntent extends Intent {

    private final static String ACTION_UPDATE_STATUS = "UpdateStatus";

    public UpdateStatusIntent(String status) {
        setAction(ACTION_UPDATE_STATUS);
        putExtra("status", status);
    }

    public static void register(final Handler handler) {
        handler.getBaseContext().registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                handler.updateStatus(intent.getStringExtra("status"));
            }
        }, new IntentFilter(ACTION_UPDATE_STATUS));
    }

    public static interface Handler {
        void updateStatus(String status);

        Context getBaseContext();
    }
}
