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
        i.putExtra(Intent.EXTRA_TEXT, getErrorMailBody());
        createLogFiles();
        attachLogFiles(i, "/sdcard/manager.log", "/sdcard/logcat.log",
                "/sdcard/getprop.log", "/sdcard/dmesg.log");
        return i;
    }

    private void createLogFiles() {
        try {
            deployCaptureLogSh();
            ShellUtils.sudo("/system/bin/sh", "/data/data/fq.router/capture-log.sh");
        } catch (Exception e) {
            Log.e("fqrouter", "failed to execute capture-log.sh", e);
            try {
                ShellUtils.sudo(false, "/system/bin/getprop", ">", "/sdcard/getprop.log");
            } catch (Exception e2) {
                Log.e("fqrouter", "failed to execute getprop", e2);
            }
            try {
                ShellUtils.sudo(false, "/system/bin/dmesg", ">", "/sdcard/dmesg.log");
            } catch (Exception e2) {
                Log.e("fqrouter", "failed to execute dmesg", e2);
            }
            try {
                ShellUtils.sudo(false, "/system/bin/logcat", "-d", "-v", "time", "-s", "fqrouter:V", ">", "/sdcard/logcat.log");
            } catch (Exception e2) {
                Log.e("fqrouter", "failed to execute logcat", e2);
            }
        }
    }

    private void deployCaptureLogSh() {
        try {
            InputStream inputStream = statusUpdater.getAssets().open("capture-log.sh");
            try {
                OutputStream outputStream = new FileOutputStream("/data/data/fq.router/capture-log.sh");
                try {
                    IOUtils.copy(inputStream, outputStream);
                } finally {
                    outputStream.close();
                }
            } finally {
                inputStream.close();
            }
        } catch (Exception e) {
            Log.e("fqrouter", "failed to deploy capture-log.sh", e);
        }
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
