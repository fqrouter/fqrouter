package fq.router2.life_cycle;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.preference.PreferenceManager;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.analytics.tracking.android.Tracker;
import fq.router2.MainActivity;
import fq.router2.R;
import fq.router2.feedback.DownloadService;
import fq.router2.utils.*;
import fq.router2.wifi_repeater.AcquireWifiLockService;

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
        try  {
            MainActivity.displayNotification(this, getResources().getString(R.string.status_exiting));
            sendBroadcast(new ExitingIntent());
            long elapsedTime = StartedAtFlag.delete();
            if (elapsedTime > 0) {
                GoogleAnalytics gaInstance = GoogleAnalytics.getInstance(this);
                Tracker gaTracker = gaInstance.getTracker("UA-37740383-2");
                gaTracker.setCustomDimension(1, Build.MODEL);
                gaTracker.setCustomDimension(2, String.valueOf(ShellUtils.isRooted()));
                gaTracker.sendTiming("engagement", elapsedTime, "session", "session");
            }
            LogUtils.i("Exiting, session life " + elapsedTime + "..." );
            new Thread(new Runnable() {
                @Override
                public void run() {
                    stopService(new Intent(ExitService.this, DownloadService.class));
                    stopService(new Intent(ExitService.this, AcquireWifiLockService.class));
                    if (ShellUtils.isRooted()) {
                        for (File file : new File[]{IOUtils.ETC_DIR, IOUtils.LOG_DIR, IOUtils.VAR_DIR}) {
                            if (file.listFiles().length > 0) {
                                try {
                                    ShellUtils.sudo(ShellUtils.BUSYBOX_FILE + " chmod 666 " + file + "/*");
                                } catch (Exception e) {
                                    LogUtils.e("failed to chmod files to non-root", e);
                                }
                            }
                        }
                    }
                }
            }).start();
            try {
                ManagerProcess.kill();
            } catch (Exception e) {
                LogUtils.e("failed to kill manager process", e);
            }
            sendBroadcast(new ExitedIntent());
            MainActivity.clearNotification(this);
        } finally {
            MainActivity.isReady = false;
        }
    }

    public static void execute(Context context) {
        context.startService(new Intent(context, ExitService.class));
    }
}
