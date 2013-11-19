package fq.router2.feedback;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import fq.router2.utils.LogUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadService extends IntentService {

    public DownloadService() {
        super("Download");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String rawUrl = intent.getStringExtra("url");
        final String downloadTo = intent.getStringExtra("downloadTo");
        try {
            HttpURLConnection connection = followRedirect(rawUrl);
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                LogUtils.i("download failed: http not ok " + connection.getResponseCode());
                sendBroadcast(new DownloadFailedIntent(rawUrl, downloadTo));
                return;
            }
            int fileLength = connection.getContentLength();
            LogUtils.i("download length: " + fileLength);
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

    private HttpURLConnection followRedirect(String rawUrl) throws Exception {
        for (int i = 0; i < 10; i++) {
            URL url = new URL(rawUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.connect();
            try {
                int responseCode = getResponseCode(connection);
                LogUtils.i("download response code: " + responseCode);
                if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                        responseCode == HttpURLConnection.HTTP_MOVED_PERM) {
                    rawUrl = connection.getHeaderField("Location");
                    LogUtils.i("redirect to => " + rawUrl);
                    connection.disconnect();
                    continue;
                }
            } catch (IOException e) {
                LogUtils.e("download io exception", e);
                continue;
            }
            return connection;
        }
        throw new Exception("too many retries");
    }

    private int getResponseCode(HttpURLConnection connection) throws Exception {
        try {
            return connection.getResponseCode();
        } catch (Exception e) {
            LogUtils.e("failed to get response code", e);
            Thread.sleep(1000);
            return connection.getResponseCode();
        }
    }

    public static void execute(Context context, String url, String downloadTo) {
        Intent intent = new Intent(context, DownloadService.class);
        intent.putExtra("url", url);
        intent.putExtra("downloadTo", downloadTo);
        context.startService(intent);
    }
}
