package fq.router2;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import fq.router2.free_internet.ConnectFreeInternetService;
import fq.router2.free_internet.FreeInternetChangedIntent;
import fq.router2.life_cycle.LaunchService;
import fq.router2.life_cycle.LaunchedIntent;
import fq.router2.utils.LogUtils;

public class BootCompletedReceiver extends BroadcastReceiver {
    private static boolean launchedOnce = false;
    private static boolean connectedOnce = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        PreferenceManager.setDefaultValues(context, R.xml.preferences, true);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (!preferences.getBoolean("AutoLaunchEnabled", false)) {
            return;
        }
        try {
            if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
                LaunchService.execute(context);
            } else if (intent.getAction().equals(LaunchedIntent.ACTION_LAUNCHED)) {
                if (!launchedOnce) {
                    launchedOnce = true;
                    ConnectFreeInternetService.execute(context);
                }
            } else if (intent.getAction().equals(FreeInternetChangedIntent.ACTION_FREE_INTERNET_CHANGED)) {
                if (intent.getBooleanExtra("isConnected", false)) {
                    boolean notificationEnabled = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                            "NotificationEnabled", true);
                    if (!connectedOnce) {
                        connectedOnce = true;
                    } else {
                        return;
                    }
                    if (notificationEnabled) {
                        MainActivity.displayNotification(
                                context, context.getResources().getString(R.string.status_free_internet_connected));
                    }
                }
            }
        } catch (Exception e) {
            LogUtils.e("failed to handle boot completed", e);
        }
    }
}
