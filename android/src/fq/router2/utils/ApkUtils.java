package fq.router2.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;

import java.util.HashMap;

public class ApkUtils {

    public static void install(Context context, String apkPath) {
        LogUtils.i("Installing fqrouter...");
        if (ShellUtils.checkRooted()) {
            try {
                installAutomatically(apkPath);
                installManually(context, apkPath); // fallback
            } catch (Exception e) {
                LogUtils.e("silent install failed", e);
                installManually(context, apkPath);
            }
        } else {
            installManually(context, apkPath);
        }
    }

    private static void installAutomatically(String apkPath) throws Exception {
        ShellUtils.sudoNoWait(
                new HashMap<String, String>(),
                ShellUtils.findCommand("sleep"), "10", ";",
                ShellUtils.findCommand("am"), "start", "-n", "fq.router2/.MainActivity");
        ShellUtils.sudo(ShellUtils.findCommand("pm"), "install", "-r", apkPath);
    }

    private static void installManually(Context context, String apkPath) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse("file://" + apkPath), "application/vnd.android.package-archive");
        context.startActivity(intent);
    }

    public static void uninstall(Context context, String packageName) {
        try {
            if (ShellUtils.checkRooted()) {
                uninstallAutomatically(packageName);
            } else {
                uninstallManually(context, packageName);
            }
        } catch (Exception e) {
            LogUtils.e("uninstall failed", e);
        }
    }

    private static void uninstallManually(Context context, String packageName) {
        Intent intent = new Intent(Intent.ACTION_DELETE, Uri.fromParts("package", packageName, null));
        context.startActivity(intent);
    }

    private static void uninstallAutomatically(String packageName) throws Exception {
        ShellUtils.sudo(ShellUtils.findCommand("pm"), "uninstall", packageName);
    }


    public static boolean isInstalled(Context context, String packageName) {
        try {
            context.getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
