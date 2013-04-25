package fq.router.utils;

import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.security.MessageDigest;

public class IOUtils {

    public static String readAll(InputStream inputStream) throws Exception {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while (null != (line = reader.readLine())) {
                stringBuilder.append(line);
            }
            return stringBuilder.toString();
        } finally {
            inputStream.close();
        }
    }

    public static String copy(InputStream inputStream, OutputStream outputStream) throws Exception {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) > 0) {
            md5.update(buffer, 0, length);
            if (outputStream != null) {
                outputStream.write(buffer, 0, length);
            }
        }
        return new BigInteger(1, md5.digest()).toString(16);
    }

    public static void writeToFile(File file, String content) throws Exception {
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
    }

    public static String readFromFile(File file) {
        try {
            return readAll(new FileInputStream(file));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
