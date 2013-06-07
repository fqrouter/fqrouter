package fq.router.feedback;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import fq.router.life.LaunchService;
import fq.router.utils.IOUtils;
import fq.router.utils.LogUtils;
import fq.router.utils.ShellUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;

public class ErrorReportEmail {

    private final static String LOG_DIR = "/sdcard/fqrouter-log";
    private final Context context;

    public ErrorReportEmail(Context context) {
        this.context = context;
    }

    public Intent prepare() {
        Intent i = new Intent(Intent.ACTION_SEND_MULTIPLE);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_EMAIL, new String[]{"fqrouter@gmail.com"});
        i.putExtra(Intent.EXTRA_SUBJECT, "android fqrouter error report for version " + LaunchService.getMyVersion(context));
        String error = createLogFiles();
        i.putExtra(Intent.EXTRA_TEXT, getErrorMailBody() + error);
        attachLogFiles(i, "manager.log", "fqsocks.log", "logcat.log", "getprop.log", "dmesg.log",
                "iptables.log", "wifi.log", "wifi.log.1", "fqdns.log", "fqting.log", "fqlan.log", "scan.log",
                "current-java.log", "current-python.log");
        return i;
    }

    private String createLogFiles() {
        if (!new File(LOG_DIR).exists()) {
            new File(LOG_DIR).mkdir();
        }
        String error = "";
        try {
            ShellUtils.sudo(ShellUtils.findCommand("getprop"), ">", LOG_DIR + "/getprop.log");
        } catch (Exception e) {
            LogUtils.e("failed to execute getprop", e);
            error += "\n" + "failed to execute getprop" + "\n" + e;
        }
        try {
            ShellUtils.sudo(ShellUtils.findCommand("dmesg"), ">", LOG_DIR + "/dmesg.log");
        } catch (Exception e) {
            LogUtils.e("failed to execute dmesg", e);
            error += "\n" + "failed to execute dmesg" + "\n" + e;
        }
        try {
            ShellUtils.sudo(ShellUtils.findCommand("logcat"),
                    "-d", "-v", "time", "-s", "fqrouter:V", ">", LOG_DIR + "/logcat.log");
        } catch (Exception e) {
            LogUtils.e("failed to execute logcat", e);
            error += "\n" + "failed to execute logcat" + "\n" + e;
        }
        try {
            ShellUtils.sudo(ShellUtils.findCommand("iptables"),
                    "-L", "-v", "-n", ">", LOG_DIR + "/iptables.log");
        } catch (Exception e) {
            LogUtils.e("failed to execute iptables for filter table", e);
            error += "\n" + "failed to execute iptables for filter table" + "\n" + e;
        }
        try {
            ShellUtils.sudo(ShellUtils.findCommand("iptables"),
                    "-t", "nat", "-L", "-v", "-n", ">>", LOG_DIR + "/iptables.log");
        } catch (Exception e) {
            LogUtils.e("failed to execute iptables for nat table", e);
            error += "\n" + "failed to execute iptables for nat table" + "\n" + e;
        }
        try {
            ShellUtils.sudo("/data/data/fq.router/busybox", "chmod", "0666", "/data/data/fq.router/log/*.log");
        } catch (Exception e) {
            LogUtils.e("failed to change log file permission", e);
            error += "\n" + "failed to change log file permission using busybox chmod" + "\n" + e;
            try {
                ShellUtils.sudo(ShellUtils.findCommand("chmod"), "0666", "/data/data/fq.router/log/*.log");
            } catch (Exception e2) {
                LogUtils.e("failed to change log file permission", e2);
                error += "\n" + "failed to change log file permission using system chmod" + "\n" + e2;
            }
        }
        error += copyLog("manager.log");
        error += copyLog("fqsocks.log");
        error += copyLog("fqdns.log");
        error += copyLog("fqting.log");
        error += copyLog("fqlan.log");
        error += copyLog("scan.log");
        error += copyLog("wifi.log");
        error += copyLog("wifi.log.1");
        error += copyLog("current-java.log");
        error += copyLog("current-python.log");
        return error;
    }

    private String copyLog(String logFileName) {
        File destFile = new File(LOG_DIR + "/" + logFileName);
        if (destFile.exists()) {
            destFile.delete();
        }
        try {
            FileInputStream inputStream = new FileInputStream("/data/data/fq.router/log/" + logFileName);
            try {
                FileOutputStream outputStream = new FileOutputStream(LOG_DIR + "/" + logFileName);
                try {
                    IOUtils.copy(inputStream, outputStream);
                } finally {
                    outputStream.close();
                }
            } finally {
                inputStream.close();
            }
        } catch (Exception e) {
            LogUtils.e("failed to copy " + logFileName, e);
            return "\n" + "failed to copy " + logFileName + "\n" + e;
        }
        return "";
    }

    private void attachLogFiles(Intent i, String... logFileNames) {
        ArrayList<Uri> logFiles = new ArrayList<Uri>();
        for (String logFileName : logFileNames) {
            File logFile = new File(LOG_DIR + "/" + logFileName);
            if (logFile.exists()) {
                logFiles.add(Uri.fromFile(logFile));
            }
        }
        try {
            i.putParcelableArrayListExtra(Intent.EXTRA_STREAM, logFiles);
        } catch (Exception e) {
            LogUtils.e("failed to attach log", e);
        }
    }

    private String getErrorMailBody() {
        StringBuilder body = new StringBuilder();
        body.append("phone model: " + Build.MODEL + "\n");
        body.append("android version: " + Build.VERSION.RELEASE + "\n");
        body.append("kernel version: " + System.getProperty("os.version") + "\n");
        body.append("fqrouter version: " + LaunchService.getMyVersion(context) + "\n");
        return body.toString();
    }
}
