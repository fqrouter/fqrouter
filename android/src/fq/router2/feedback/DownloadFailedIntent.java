package fq.router2.feedback;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import fq.router2.utils.LoggedBroadcastReceiver;

public class DownloadFailedIntent extends Intent {
    private final static String ACTION_DOWNLOAD_FAILED = "DownloadFailed";

    public DownloadFailedIntent(String url, String downloadTo) {
        setAction(ACTION_DOWNLOAD_FAILED);
        putExtra("url", url);
        putExtra("downloadTo", downloadTo);
    }

    public static void register(final Handler handler) {
        handler.getBaseContext().registerReceiver(new LoggedBroadcastReceiver() {
            @Override
            protected void handle(Context context, Intent intent) {
                handler.onDownloadFailed(intent.getStringExtra("url"), intent.getStringExtra("downloadTo"));
            }
        }, new IntentFilter(ACTION_DOWNLOAD_FAILED));
    }

    public static interface Handler {
        void onDownloadFailed(String url, String downloadTo);

        Context getBaseContext();
    }
}
