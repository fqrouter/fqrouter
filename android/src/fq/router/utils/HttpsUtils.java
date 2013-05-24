package fq.router.utils;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;

public class HttpsUtils {

    public static int getTotalLength(URL url, Inet4Address staticAddress) throws Exception {
        HttpsURLConnection connection = createConnection(url, staticAddress);
        connection.connect();
        try {
            return connection.getContentLength();
        } finally {
            connection.disconnect();
        }
    }

    public static void download(
            URL url, Inet4Address staticAddress,
            OutputStream outputStream, int from, int to, IOUtils.ChunkCopied chunkCopied) throws Exception {
        HttpsURLConnection connection = createConnection(url, staticAddress);
        connection.setRequestProperty("Range", "bytes=" + from + "-" + to);
        connection.connect();
        try {
            InputStream inputStream = connection.getInputStream();
            try {
                IOUtils.copy(inputStream, outputStream, chunkCopied);
            } finally {
                inputStream.close();
            }
        } finally {
            connection.disconnect();
        }
    }

    private static HttpsURLConnection createConnection(URL url, Inet4Address staticAddress) throws IOException {
        HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
        urlConnection.setConnectTimeout(5);
        urlConnection.setSSLSocketFactory(new StaticAddressSSLSocketFactory(staticAddress));
        urlConnection.setHostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String s, SSLSession sslSession) {
                return true;
            }
        });
        return urlConnection;
    }

    private static class StaticAddressSSLSocketFactory extends SSLSocketFactory {

        private final SSLSocketFactory delegatedTo;
        private final Inet4Address staticAddress;

        public StaticAddressSSLSocketFactory(Inet4Address staticAddress) {
            this.staticAddress = staticAddress;
            delegatedTo = (SSLSocketFactory) SSLSocketFactory.getDefault();
        }

        @Override
        public String[] getDefaultCipherSuites() {
            return delegatedTo.getDefaultCipherSuites();
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return delegatedTo.getSupportedCipherSuites();
        }

        @Override
        public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException {
            return delegatedTo.createSocket(staticAddress, port);
        }

        @Override
        public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
            return delegatedTo.createSocket(staticAddress, port);
        }

        @Override
        public Socket createSocket(String host, int port, InetAddress localHost, int localPort)
                throws IOException, UnknownHostException {
            return delegatedTo.createSocket(staticAddress, port);
        }

        @Override
        public Socket createSocket(InetAddress host, int port) throws IOException {
            return delegatedTo.createSocket(staticAddress, port);
        }

        @Override
        public Socket createSocket(InetAddress host, int port, InetAddress localHost, int localPort)
                throws IOException {
            return delegatedTo.createSocket(staticAddress, port);
        }
    }
}
