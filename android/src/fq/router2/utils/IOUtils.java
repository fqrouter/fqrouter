package fq.router2.utils;

import java.io.*;
import java.math.BigInteger;
import java.security.MessageDigest;

public class IOUtils {

    public static File ETC_DIR = new File("/data/data/fq.router2/etc");
    public static File VAR_DIR = new File("/data/data/fq.router2/var");
    public static File LOG_DIR = new File("/data/data/fq.router2/log");

    public static void createCommonDirs() {
        try {
            if (!ETC_DIR.exists()) {
                ETC_DIR.mkdir();
            }
            if (!VAR_DIR.exists()) {
                VAR_DIR.mkdir();
            }
            if (!LOG_DIR.exists()) {
                LOG_DIR.mkdir();
            }
        } catch (Exception e) {
            LogUtils.e("failed to create common dirs", e);
        }
    }

    public static String md5Checksum(File file) {
        try {
            return copy(new FileInputStream(file), null);
        } catch (Exception e) {
            LogUtils.e("failed to calculate md5 checksum", e);
            return "";
        }
    }

    public static interface LineRead {
        void onLineRead(String line);
    }

    public static interface ChunkCopied {
        void onChunkCopied(byte[] buffer, int length) throws Exception;
    }

    public static String readAll(InputStream inputStream) throws Exception {
        return readAll(inputStream, null);
    }

    public static String readAll(InputStream inputStream, LineRead callback) throws Exception {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while (null != (line = reader.readLine())) {
                if (null != callback) {
                    callback.onLineRead(line);
                }
                stringBuilder.append(line);
            }
            return stringBuilder.toString();
        } finally {
            inputStream.close();
        }
    }

    public static String copy(InputStream inputStream, OutputStream outputStream) throws Exception {
        return copy(inputStream, outputStream, null);
    }

    public static String copy(InputStream inputStream, OutputStream outputStream, ChunkCopied callback) throws Exception {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        byte[] buffer = new byte[1024 * 32];
        int length;
        while ((length = inputStream.read(buffer)) > 0) {
            md5.update(buffer, 0, length);
            if (null != outputStream) {
                outputStream.write(buffer, 0, length);
            }
            if (null != callback) {
                callback.onChunkCopied(buffer, length);
            }
        }
        return new BigInteger(1, md5.digest()).toString(16);
    }

    public static void writeToFile(File file, String content) {
        try {

            FileOutputStream outputStream = new FileOutputStream(file);
            try {
                OutputStreamWriter writer = new OutputStreamWriter(outputStream);
                try {
                    writer.write(content);
                } finally {
                    writer.close();
                }
            } finally {
                outputStream.close();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String readFromFile(File file) {
        if (!file.exists()) {
            return "";
        }
        try {
            return readAll(new FileInputStream(file));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
