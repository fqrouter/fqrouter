package fq.router.life;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import fq.router.utils.Downloader;

public class DownloadService extends IntentService {

    public DownloadService() {
        super("Download");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        final String url = intent.getStringExtra("url");
        final String downloadTo = intent.getStringExtra("downloadTo");
        Downloader.download(url, downloadTo, 16, new Downloader.ProgressUpdated() {
            @Override
            public void onProgressUpdated(int percent) {
                sendBroadcast(new DownloadingIntent(url, downloadTo, percent));
            }

            @Override
            public void onDownloaded() {
                sendBroadcast(new DownloadedIntent(url, downloadTo));
            }

            @Override
            public void onDownloadFailed() {
                sendBroadcast(new DownloadFailedIntent(url, downloadTo));
            }
        });
    }

    public static void execute(Context context, String url, String downloadTo) {
        Intent intent = new Intent(context, DownloadService.class);
        intent.putExtra("url", url);
        intent.putExtra("downloadTo", downloadTo);
        context.startService(intent);
    }

}
