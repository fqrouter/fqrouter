package fq.router.utils;

import android.util.Log;

import java.io.*;
import java.util.Date;

public class LogUtils {

    private static File logFile;

    public static void e(String msg) {
        try {
            Log.e("fqrouter", msg);
            writeLogFile("ERROR", msg);
        } catch (Exception e) {
            System.out.println(msg);
        }
    }

    public static String e(String msg, Throwable exception) {
        try {
            Log.e("fqrouter", msg, exception);
            writeLogFile("ERROR", msg + "\r\n" + formatException(exception));
        } catch (Exception e) {
            System.out.println(msg);
            exception.printStackTrace();
        }
        return msg;
    }

    private static String formatException(Throwable e) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        e.printStackTrace(ps);
        ps.close();
        return baos.toString();
    }

    public static void i(String msg) {
        try {
            Log.i("fqrouter", msg);
            writeLogFile("INFO", msg);
        }  catch (Exception e) {
            System.out.println(msg);
        }
    }

    private static void writeLogFile(String level, String line) {
        if (logFile == null) {
            logFile = new File("/data/data/fq.router/log/current-java.log");
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
