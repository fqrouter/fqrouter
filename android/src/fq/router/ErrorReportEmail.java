package fq.router;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import fq.router.utils.IOUtils;
import fq.router.utils.ShellUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class ErrorReportEmail {

    private final StatusUpdater statusUpdater;

    public ErrorReportEmail(StatusUpdater statusUpdater) {
        this.statusUpdater = statusUpdater;
    }

    public Intent prepare() {
        Intent i = new Intent(Intent.ACTION_SEND_MULTIPLE);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_EMAIL, new String[]{"fqrouter@gmail.com"});
        i.putExtra(Intent.EXTRA_SUBJECT, "android fqrouter error report for version " + statusUpdater.getMyVersion());
        String error = createLogFiles();
        i.putExtra(Intent.EXTRA_TEXT, getErrorMailBody() + error);
        attachLogFiles(i, "/sdcard/manager.log", "/sdcard/redsocks.log", "/sdcard/logcat.log",
                "/sdcard/getprop.log", "/sdcard/dmesg.log", "/sdcard/iptables.log",
                "/sdcard/twitter.log", "/sdcard/wifi.log", "/sdcard/dns.log", "/sdcard/current.log");
        return i;
    }

    private String createLogFiles() {
        String error = "";
        try {
            ShellUtils.sudo(ShellUtils.findCommand("getprop"), ">", "/sdcard/getprop.log");
        } catch (Exception e) {
            Log.e("fqrouter", "failed to execute getprop", e);
            error += "\n" + "failed to execute getprop" + "\n" + e;
        }
        try {
            ShellUtils.sudo(ShellUtils.findCommand("dmesg"), ">", "/sdcard/dmesg.log");
        } catch (Exception e) {
            Log.e("fqrouter", "failed to execute dmesg", e);
            error += "\n" + "failed to execute dmesg" + "\n" + e;
        }
        try {
            ShellUtils.sudo(ShellUtils.findCommand("logcat"),
                    "-d", "-v", "time", "-s", "fqrouter:V", ">", "/sdcard/logcat.log");
        } catch (Exception e) {
            Log.e("fqrouter", "failed to execute logcat", e);
            error += "\n" + "failed to execute logcat" + "\n" + e;
        }
        try {
            ShellUtils.sudo(ShellUtils.findCommand("iptables"),
                    "-L", "-v", "-n", ">", "/sdcard/iptables.log");
        } catch (Exception e) {
            Log.e("fqrouter", "failed to execute iptables for filter table", e);
            error += "\n" + "failed to execute iptables for filter table" + "\n" + e;
        }
        try {
            ShellUtils.sudo(ShellUtils.findCommand("iptables"),
                    "-t", "nat", "-L", "-v", "-n", ">>", "/sdcard/iptables.log");
        } catch (Exception e) {
            Log.e("fqrouter", "failed to execute iptables for nat table", e);
            error += "\n" + "failed to execute iptables for nat table" + "\n" + e;
        }
        error += copyLog("manager.log");
        error += copyLog("redsocks.log");
        error += copyLog("twitter.log");
        error += copyLog("wifi.log");
        error += copyLog("dns.log");
        error += copyLog("current.log");
        return error;
    }

    private String copyLog(String logFileName) {
        try {
            ShellUtils.sudo(
                    "/data/data/fq.router/busybox", "cp", "/data/data/fq.router/" + logFileName, "/sdcard/" + logFileName);
        } catch (Exception e) {
            Log.e("fqrouter", "failed to copy " + logFileName, e);
            return "\n" + "failed to copy " + logFileName + "\n" + e;
        }
        return "";
    }

    private void attachLogFiles(Intent i, String... logFilePaths) {
        ArrayList<Uri> logFiles = new ArrayList<Uri>();
        for (String logFilePath : logFilePaths) {
            File logFile = new File(logFilePath);
            if (logFile.exists()) {
                logFiles.add(Uri.fromFile(logFile));
            }
        }
        try {
            i.putParcelableArrayListExtra(Intent.EXTRA_STREAM, logFiles);
        } catch (Exception e) {
            Log.e("fqrouter", "failed to attach log", e);
        }
    }

    private String getErrorMailBody() {
        StringBuilder body = new StringBuilder();
        body.append("phone model: " + Build.MODEL + "\n");
        body.append("android version: " + Build.VERSION.RELEASE + "\n");
        body.append("kernel version: " + System.getProperty("os.version") + "\n");
        body.append("fqrouter version: " + statusUpdater.getMyVersion() + "\n");
        return body.toString();
    }
}
