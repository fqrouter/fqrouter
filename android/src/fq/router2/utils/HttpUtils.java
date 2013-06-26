package fq.router2.utils;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpUtils {
    public static String post(String request) throws Exception {
        return post(request, "");
    }

    public static String post(String request, String body) throws Exception {
        LogUtils.i("HTTP POST " + request + ", body: " + body);
        URL url = new URL(request);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("charset", "utf-8");
            connection.setRequestProperty("Content-Length", "" + Integer.toString(body.getBytes().length));
            connection.setUseCaches(false);
            connection.setConnectTimeout(60000);
            connection.setReadTimeout(60000);
            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            try {
                wr.writeBytes(body);
                wr.flush();
            } finally {
                wr.close();
            }
            return handleResponse(connection, null);
        } finally {
            connection.disconnect();
        }
    }


    public static String get(String request) throws Exception {
        return get(request, null, 3000);
    }

    public static String get(String request, IOUtils.LineRead callback, int timeout) throws Exception {
        URL url = new URL(request);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("GET");
            connection.setUseCaches(false);
            if (timeout > 0) {
                connection.setConnectTimeout(timeout);
                connection.setReadTimeout(timeout);
            }
            return handleResponse(connection, callback);
        } finally {
            connection.disconnect();
        }
    }

    private static String handleResponse(HttpURLConnection connection, IOUtils.LineRead callback) throws Exception {
        int responseCode = connection.getResponseCode();
        if (responseCode >= 200 && responseCode < 300) {
            return IOUtils.readAll(connection.getInputStream(), callback);
        } else {
            throw new Error(responseCode, IOUtils.readAll(connection.getErrorStream(), callback));
        }
    }

    public static class Error extends RuntimeException {
        public final int responseCode;
        public final String output;

        public Error(int responseCode, String output) {
            this.responseCode = responseCode;
            this.output = output;
        }

        @Override
        public String toString() {
            return "Error{" +
                    "responseCode=" + responseCode +
                    ", output='" + output + '\'' +
                    '}';
        }
    }
}
