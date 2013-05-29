package fq.router.utils;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

public class HttpsUtils {

    public static long getTotalLength(URL url, final String staticAddress, boolean isSecure) throws Exception {
        HttpGet request = createRequest(url, staticAddress, isSecure);
        return execute(request).getEntity().getContentLength();
    }

    private static HttpGet createRequest(URL url, String staticAddress, boolean isSecure) {
        String uri = isSecure ? url.toString().replace(url.getProtocol(), "http")
                : url.toString().replace(url.getProtocol(), "https");
        uri = uri.replace(url.getHost(), staticAddress);
        HttpGet request = new HttpGet(uri);
        request.setHeader("Host", url.getHost());
        return request;
    }

    private static HttpResponse execute(HttpUriRequest request) throws IOException {
        return new DefaultHttpClient() {
            @Override
            protected ClientConnectionManager createClientConnectionManager() {
                SchemeRegistry schreg = new SchemeRegistry();
                org.apache.http.conn.ssl.SSLSocketFactory sslSocketFactory = org.apache.http.conn.ssl.SSLSocketFactory.getSocketFactory();
                sslSocketFactory.setHostnameVerifier(org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
                schreg.register(new Scheme("https", sslSocketFactory, 443));
                PlainSocketFactory plainSocketFactory = PlainSocketFactory.getSocketFactory();
                schreg.register(new Scheme("http", plainSocketFactory, 80));
                return new ThreadSafeClientConnManager(new BasicHttpParams(), schreg);
            }
        }.execute(request);
    }

    public static void download(
            URL url, String staticAddress, boolean isSecure,
            OutputStream outputStream, long from, long to, IOUtils.ChunkCopied chunkCopied) throws Exception {
        HttpGet request = createRequest(url, staticAddress, isSecure);
        request.addHeader("Range", "bytes=" + from + "-" + to);
        HttpResponse response = execute(request);
        InputStream inputStream = response.getEntity().getContent();
        try {
            IOUtils.copy(inputStream, outputStream, chunkCopied);
        } finally {
            inputStream.close();
        }
    }

}
