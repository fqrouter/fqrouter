package fq.router2.feedback;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import fq.router2.utils.LogUtils;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadService extends IntentService {

    public DownloadService() {
        super("Download");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        final String rawUrl = intent.getStringExtra("url");
        final String downloadTo = intent.getStringExtra("downloadTo");
        try {
            URL url = new URL(rawUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setInstanceFollowRedirects(true);
            connection.connect();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                LogUtils.i("download failed: http not ok " + connection.getResponseCode());
                sendBroadcast(new DownloadFailedIntent(rawUrl, downloadTo));
                return;
            }
            int fileLength = connection.getContentLength();
            InputStream input = connection.getInputStream();
            try {
                FileOutputStream output = new FileOutputStream(downloadTo);
                try {
                    byte buffer[] = new byte[65536];
                    long total = 0;
                    int count;
                    while ((count = input.read(buffer)) != -1) {
                        total += count;
                        if (fileLength > 0) {
                            sendBroadcast(new DownloadingIntent(rawUrl, downloadTo, (int) (total * 100 / fileLength)));
                        }
                        output.write(buffer, 0, count);
                    }
                } finally {
                    output.close();
                }
            } finally {
                input.close();
            }
            sendBroadcast(new DownloadedIntent(rawUrl, downloadTo));
        } catch (Exception e) {
            LogUtils.e("failed to download", e);
            sendBroadcast(new DownloadFailedIntent(rawUrl, downloadTo));
        }
    }

    public static void execute(Context context, String url, String downloadTo) {
        Intent intent = new Intent(context, DownloadService.class);
        intent.putExtra("url", url);
        intent.putExtra("downloadTo", downloadTo);
        context.startService(intent);
    }
}
