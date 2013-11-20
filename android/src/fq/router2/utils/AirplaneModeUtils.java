package fq.router2.utils;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;

public class AirplaneModeUtils {

    public static void toggle(Context context) throws Exception {
        if (Build.VERSION.SDK_INT >= 17) {
            toggleAboveApiLevel17();
        } else {
            toggleBelowApiLevel17(context);
        }
    }

    private static void toggleAboveApiLevel17() throws Exception {
        // Android 4.2 and above
        try {
            ShellUtils.sudo("ndc", "resolver", "flushdefaultif");
        } catch (Exception e) {
            LogUtils.e("failed to flush dns cache via ndc", e);
            ShellUtils.sudo("settings", "put", "global", "airplane_mode_on", "1");
            ShellUtils.sudo("am", "broadcast", "-a", "android.intent.action.AIRPLANE_MODE", "--ez state", "true");
            ShellUtils.sudo("settings", "put", "global", "airplane_mode_on", "0");
            ShellUtils.sudo("am", "broadcast", "-a", "android.intent.action.AIRPLANE_MODE", "--ez state", "false");
        }
    }

    private static void toggleBelowApiLevel17(Context context) throws Exception {
        // Android 4.2 below
        Settings.System.putInt(
                context.getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, 1);
        Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intent.putExtra("state", true);
        context.sendBroadcast(intent);
        Thread.sleep(3000);
        Settings.System.putInt(
                context.getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, 0);
        intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intent.putExtra("state", false);
        context.sendBroadcast(intent);
    }
}
