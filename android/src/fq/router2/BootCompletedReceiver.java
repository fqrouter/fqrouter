package fq.router2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import fq.router2.life_cycle.LaunchService;
import fq.router2.utils.LogUtils;

public class BootCompletedReceiver extends BroadcastReceiver {
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
                MainActivity.displayNotification(context, context.getString(R.string.status_ready));
            }
        } catch (Exception e) {
            LogUtils.e("failed to handle boot completed", e);
        }
    }
}
