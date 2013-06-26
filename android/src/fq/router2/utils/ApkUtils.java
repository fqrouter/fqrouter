package fq.router2.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.util.HashMap;

public class ApkUtils {

    public static void installApk(Context context, String apkPath) {
        LogUtils.i("Installing fqrouter...");
        if (ShellUtils.checkRooted()) {
            try {
                installApkAutomatically(apkPath);
                installApkManually(context, apkPath); // fallback
            } catch (Exception e) {
                LogUtils.e("silent install failed", e);
                installApkManually(context, apkPath);
            }
        } else {
            installApkManually(context, apkPath);
        }
    }

    private static void installApkAutomatically(String apkPath) throws Exception {
        ShellUtils.sudoNoWait(
                new HashMap<String, String>(),
                ShellUtils.findCommand("sleep"), "10", ";",
                ShellUtils.findCommand("am"), "start", "-n", "fq.router/.MainActivity");
        ShellUtils.sudo(ShellUtils.findCommand("pm"), "install", "-r", apkPath);
    }

    private static void installApkManually(Context context, String apkPath) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse("file://" + apkPath), "application/vnd.android.package-archive");
        context.startActivity(intent);
    }
}
