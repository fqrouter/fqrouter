package fq.router2.feedback;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import fq.router2.utils.LoggedBroadcastReceiver;

public class DownloadingIntent extends Intent {
    private final static String ACTION_DOWNLOADING = "Downloading";

    public DownloadingIntent(String url, String downloadTo, int percent) {
        setAction(ACTION_DOWNLOADING);
        putExtra("url", url);
        putExtra("downloadTo", downloadTo);
        putExtra("percent", percent);
    }

    public static void register(final Handler handler) {
        handler.getBaseContext().registerReceiver(new LoggedBroadcastReceiver() {
            @Override
            protected void handle(Context context, Intent intent) {
                handler.onDownloading(
                        intent.getStringExtra("url"), intent.getStringExtra("downloadTo"),
                        intent.getIntExtra("percent", 0));
            }
        }, new IntentFilter(ACTION_DOWNLOADING));
    }

    public static interface Handler {
        void onDownloading(String url, String downloadTo, int percent);

        Context getBaseContext();
    }
}
