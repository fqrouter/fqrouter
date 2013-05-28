package fq.router.utils;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ClientConnectionOperator;
import org.apache.http.conn.OperatedClientConnection;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.DefaultClientConnectionOperator;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class HttpsUtils {

    public static long getTotalLength(String url, final String staticAddress) throws Exception {
        return execute(staticAddress, new HttpGet(url)).getEntity().getContentLength();
    }

    private static HttpResponse execute(final String staticAddress, HttpUriRequest request) throws IOException {
        return new DefaultHttpClient() {
            @Override
            protected ClientConnectionManager createClientConnectionManager() {
                SchemeRegistry schreg = new SchemeRegistry();
                org.apache.http.conn.ssl.SSLSocketFactory sslSocketFactory = org.apache.http.conn.ssl.SSLSocketFactory.getSocketFactory();
                sslSocketFactory.setHostnameVerifier(org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
                schreg.register(new Scheme("https", sslSocketFactory, 443));
                PlainSocketFactory plainSocketFactory = PlainSocketFactory.getSocketFactory();
                schreg.register(new Scheme("http", plainSocketFactory, 80));
                return new ThreadSafeClientConnManager(new BasicHttpParams(), schreg) {
                    @Override
                    protected ClientConnectionOperator createConnectionOperator(final SchemeRegistry schreg) {
                        try {
                            return new DefaultClientConnectionOperator(schreg) {
                                @Override
                                public void openConnection(OperatedClientConnection conn, HttpHost target, InetAddress local, HttpContext context, HttpParams params) throws IOException {
                                    Scheme scheme = schreg.getScheme(target);
                                    SocketFactory sf = scheme.getSocketFactory();
                                    Socket socket = sf.createSocket();
                                    conn.opening(socket, target);
                                    int port = target.getPort() == -1 ? scheme.getDefaultPort() : target.getPort();
                                    Socket newSocket = sf.connectSocket(socket, staticAddress, port, local, 0, params);
                                    if (newSocket != socket) {
                                        conn.opening(newSocket, target);
                                    }
                                    conn.openCompleted(sf.isSecure(socket), params);
                                }
                            };
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                };
            }
        }.execute(request);
    }

    public static void download(
            String url, String staticAddress,
            OutputStream outputStream, long from, long to, IOUtils.ChunkCopied chunkCopied) throws Exception {
        HttpGet request = new HttpGet(url);
        request.addHeader("Range", "bytes=" + from + "-" + to);
        HttpResponse response = execute(staticAddress, request);
        InputStream inputStream = response.getEntity().getContent();
        try {
            IOUtils.copy(inputStream, outputStream, chunkCopied);
        } finally {
            inputStream.close();
        }
    }

}
