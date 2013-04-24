package fq.router.utils;

import android.util.Log;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpUtils {
    public static String post(String request) throws Exception {
        return post(request, "");
    }

    public static String post(String request, String body) throws Exception {
        Log.i("fqrouter", "HTTP POST " + request + ", body: " + body);
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
            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            try {
                wr.writeBytes(body);
                wr.flush();
            } finally {
                wr.close();
            }
            int responseCode = connection.getResponseCode();
            String output = IOUtils.readAll(connection.getInputStream());
            if (responseCode >= 200 && responseCode < 300) {
                return output;
            } else {
                throw new Error(responseCode, output);
            }
        } finally {
            connection.disconnect();
        }
    }

    public static String get(String request) throws Exception {
        URL url = new URL(request);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("GET");
            connection.setUseCaches(false);
            int responseCode = connection.getResponseCode();
            String output = IOUtils.readAll(connection.getInputStream());
            if (responseCode >= 200 && responseCode < 300) {
                return output;
            } else {
                throw new Error(responseCode, output);
            }
        } finally {
            connection.disconnect();
        }
    }

    public static class Error extends RuntimeException {
        public final int responseCode;
        public final String output;

        public Error(int responseCode, String output) {
            this.responseCode = responseCode;
            this.output = output;
        }
    }
}
