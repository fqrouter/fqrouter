package fq.router.utils;

import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Date;

public class LogUtils {

    private static File logFile;

    public static void e(String msg) {
        Log.e("fqrouter", msg);
        writeLogFile("ERROR", msg);
    }

    public static void e(String msg, Throwable exception) {
        Log.e("fqrouter", msg, exception);
        writeLogFile("ERROR", msg + "\r\n" + exception.toString());
    }

    public static void i(String msg) {
        Log.i("fqrouter", msg);
        writeLogFile("INFO", msg);
    }

    private static void writeLogFile(String level, String line) {
        if (logFile == null) {
            logFile = new File("/data/data/fq.router/current-java.log");
            if (logFile.exists()) {
                logFile.delete();
            }
        }
        try {
            FileOutputStream outputStream = new FileOutputStream(logFile, true);
            try {
                OutputStreamWriter writer = new OutputStreamWriter(outputStream);
                try {
                    writer.write(new Date() + " " + level + " " + line + "\r\n");
                } finally {
                    writer.close();
                }
            } finally {
                outputStream.close();
            }
        } catch (Exception e) {
            Log.e("fqrouter", "failed to write log file", e);
        }
    }
}
