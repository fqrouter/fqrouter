package fq.router2.utils;

import android.util.Log;

import java.io.*;
import java.util.Date;

public class LogUtils {

    private static File logFile;

    public static String e(String msg) {
        try {
            try {
                Log.e("fqrouter", msg);
                writeLogFile("ERROR", msg);
            } catch (Exception e) {
                System.out.println(msg);
            }
            return msg;
        } catch (Exception e) {
            // ignore
            return msg;
        }
    }

    public static String e(String msg, Throwable exception) {
        try {
            try {
                Log.e("fqrouter", msg, exception);
                writeLogFile("ERROR", msg + "\r\n" + formatException(exception));
            } catch (Exception e) {
                System.out.println(msg);
                exception.printStackTrace();
            }
            return msg;
        } catch(Exception e) {
            // ignore
            return msg;
        }
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
            try {
                Log.i("fqrouter", msg);
                writeLogFile("INFO", msg);
            }  catch (Exception e) {
                System.out.println(msg);
            }
        } catch (Exception e) {
            // ignore
        }
    }

    private static void writeLogFile(String level, String line) {
        if (logFile == null) {
            logFile = new File("/data/data/fq.router2/log/current-java.log");
            if (logFile.length() > 1024 * 1024) {
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
