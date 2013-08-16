package fq.router2.life_cycle;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.analytics.tracking.android.Tracker;
import fq.router2.utils.IOUtils;
import fq.router2.utils.LogUtils;
import fq.router2.utils.ShellUtils;
import fq.router2.utils.StartedAtFlag;

import java.io.File;

public class ExitService extends IntentService {

    public ExitService() {
        super("Exit");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        exit();
    }

    private void exit() {
        long elapsedTime = StartedAtFlag.delete();
        GoogleAnalytics gaInstance = GoogleAnalytics.getInstance(this);
        Tracker gaTracker = gaInstance.getTracker("UA-37740383-2");
        gaTracker.setCustomDimension(1, Build.MODEL);
        gaTracker.setCustomDimension(2, String.valueOf(ShellUtils.isRooted()));
        gaTracker.sendTiming("engagement", elapsedTime, "session", "session");
        LogUtils.i("Exiting, session life " + elapsedTime + "..." );
        if (ShellUtils.isRooted()) {
            try {
                for (File file : new File[]{IOUtils.ETC_DIR, IOUtils.LOG_DIR, IOUtils.VAR_DIR}) {
                    if (file.listFiles().length > 0) {
                        ShellUtils.sudo(ShellUtils.BUSYBOX_FILE + " chmod 666 " + file + "/*");
                    }
                }
            } catch (Exception e) {
                LogUtils.e("failed to chmod files to non-root", e);
            }
        }
        try {
            ManagerProcess.kill();
        } catch (Exception e) {
            LogUtils.e("failed to kill manager process", e);
        }
        sendBroadcast(new ExitedIntent());
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(0);
    }

    public static void execute(Context context) {
        context.startService(new Intent(context, ExitService.class));
    }
}
