package fq.router.vpn;

import android.content.Intent;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import fq.router.MainActivity;
import fq.router.feedback.UpdateStatusIntent;
import fq.router.life.ExitService;
import fq.router.life.LaunchedIntent;
import fq.router.utils.LogUtils;

import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SocksVpnService extends VpnService {

    private static ParcelFileDescriptor tunPFD;

    @Override
    public void onStart(Intent intent, int startId) {
        startVpn();
        LogUtils.i("on start");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startVpn();
        return START_STICKY;
    }

    @Override
    public void onRevoke() {
        stopSelf();
    }

    @Override
    public void onDestroy() {
        stopVpn();
    }

    private void startVpn() {
        try {
            if (tunPFD != null) {
                throw new RuntimeException("another VPN is still running");
            }
            tunPFD = new Builder()
                    .setSession("fqrouter")
                    .addAddress("10.25.1.1", 24)
                    .addRoute("0.0.0.0", 0)
                    .addDnsServer("8.8.8.8")
                    .establish();
            if (tunPFD == null) {
                stopSelf();
                return;
            }
            final int tunFD = tunPFD.getFd();
            LogUtils.i("tunFD is " + tunFD);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        listenFdServerSocket(tunPFD);
                    } catch (Exception e) {
                        LogUtils.e("fdsock failed " + e, e);
                    }
                }
            }).start();
            updateStatus("Started in VPN mode");
            sendBroadcast(new LaunchedIntent(true));
        } catch (Exception e) {
            LogUtils.e("establish failed", e);
        }
    }

    private void listenFdServerSocket(final ParcelFileDescriptor tunPFD) throws Exception {
        final LocalServerSocket fdServerSocket = new LocalServerSocket("fdsock");
        try {

            ExecutorService executorService = Executors.newFixedThreadPool(16);
            while (isRunning()) {
                try {
                    final LocalSocket fdSocket = fdServerSocket.accept();
                    executorService.submit(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                passFileDescriptor(fdSocket, tunPFD.getFileDescriptor());
                            } catch (Exception e) {
                                LogUtils.e("failed to handle fdsock", e);
                            }
                        }
                    });
                } catch (Exception e) {
                    LogUtils.e("failed to handle fdsock", e);
                }
            }
            executorService.shutdown();
        } finally {
            fdServerSocket.close();
        }
    }

    public static boolean isRunning() {
        return tunPFD != null;
    }

    private void passFileDescriptor(LocalSocket fdSocket, FileDescriptor tunFD) throws Exception {
        OutputStream outputStream = fdSocket.getOutputStream();
        InputStream inputStream = fdSocket.getInputStream();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream), 1);
            String request = reader.readLine();
            String[] parts = request.split(",");
            if ("TUN".equals(parts[0])) {
                fdSocket.setFileDescriptorsForSend(new FileDescriptor[]{tunFD});
                outputStream.write('*');
            } else if ("OPEN UDP".equals(parts[0])) {
                String socketId = parts[1];
                passUdpFileDescriptor(fdSocket, outputStream, socketId);
            } else if ("OPEN TCP".equals(parts[0])) {
                String socketId = parts[1];
                String dstIp = parts[2];
                int dstPort = Integer.parseInt(parts[3]);
                int connectTimeout = Integer.parseInt(parts[4]);
                passTcpFileDescriptor(fdSocket, outputStream, socketId, dstIp, dstPort, connectTimeout);
            } else {
                throw new UnsupportedOperationException("fdsock unable to handle: " + request);
            }
        } finally {
            try {
                inputStream.close();
            } catch (Exception e) {
                LogUtils.e("failed to close input stream", e);
            }
            try {
                outputStream.close();
            } catch (Exception e) {
                LogUtils.e("failed to close output stream", e);
            }
            fdSocket.close();
        }
    }

    private void passTcpFileDescriptor(
            LocalSocket fdSocket, OutputStream outputStream,
            String socketId, String dstIp, int dstPort, int connectTimeout) throws Exception {
        Socket sock = new Socket();
        sock.setTcpNoDelay(true); // force file descriptor being created
        if (protect(sock)) {
            try {
                sock.connect(new InetSocketAddress(dstIp, dstPort), connectTimeout);
                ParcelFileDescriptor fd = ParcelFileDescriptor.fromSocket(sock);
                fdSocket.setFileDescriptorsForSend(new FileDescriptor[]{fd.getFileDescriptor()});
                outputStream.write('*');
                outputStream.flush();
                fd.close();
            } catch (ConnectException e) {
                LogUtils.e("connect " + dstIp + ":" + dstPort + " failed");
                outputStream.write('!');
                sock.close();
            } catch (SocketTimeoutException e) {
                LogUtils.e("connect " + dstIp + ":" + dstPort + " failed");
                outputStream.write('!');
                sock.close();
            } finally {
                outputStream.flush();
            }
        } else {
            LogUtils.e("protect tcp socket failed");
        }
    }

    private void passUdpFileDescriptor(LocalSocket fdSocket, OutputStream outputStream, String socketId) throws Exception {
        DatagramSocket sock = new DatagramSocket();
        if (protect(sock)) {
            ParcelFileDescriptor fd = ParcelFileDescriptor.fromDatagramSocket(sock);
            fdSocket.setFileDescriptorsForSend(new FileDescriptor[]{fd.getFileDescriptor()});
            outputStream.write('*');
            outputStream.flush();
            fd.close();
        } else {
            LogUtils.e("protect udp socket failed");
        }
    }

    private void stopVpn() {
        try {
            tunPFD.close();
        } catch (IOException e) {
            LogUtils.e("failed to stop tunPFD", e);
        }
        tunPFD = null;
        MainActivity.setShouldExit();
        ExitService.execute(this);
    }

    private void updateStatus(String status) {
        sendBroadcast(new UpdateStatusIntent(status));
    }
}

