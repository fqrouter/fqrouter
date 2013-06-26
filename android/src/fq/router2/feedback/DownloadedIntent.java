package fq.router2.feedback;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import fq.router2.utils.LoggedBroadcastReceiver;

public class DownloadedIntent extends Intent {
    private final static String ACTION_DOWNLOADED = "Downloaded";

    public DownloadedIntent(String url, String downloadTo) {
        setAction(ACTION_DOWNLOADED);
        putExtra("url", url);
        putExtra("downloadTo", downloadTo);
    }

    public static void register(final Handler handler) {
        handler.getBaseContext().registerReceiver(new LoggedBroadcastReceiver() {
            @Override
            protected void handle(Context context, Intent intent) {
                handler.onDownloaded(intent.getStringExtra("url"), intent.getStringExtra("downloadTo"));
            }
        }, new IntentFilter(ACTION_DOWNLOADED));
    }

    public static interface Handler {
        void onDownloaded(String url, String downloadTo);

        Context getBaseContext();
    }
}
