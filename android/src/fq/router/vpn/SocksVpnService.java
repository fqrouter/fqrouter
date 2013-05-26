package fq.router.vpn;

import android.content.Intent;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import fq.router.feedback.UpdateStatusIntent;
import fq.router.life.LaunchedIntent;
import fq.router.utils.LogUtils;

import java.io.*;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;

public class SocksVpnService extends VpnService {

    private static LocalServerSocket fdServerSocket;

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
            final ParcelFileDescriptor tunPFD = new Builder()
                    .setSession("fqrouter")
                    .addAddress("10.24.1.1", 24)
                    .addRoute("0.0.0.0", 0)
                    .establish();
            if (tunPFD == null) {
                stopSelf();
                return;
            }
            final int tunFD = tunPFD.getFd();
            LogUtils.i("tunFD is " + tunFD);
            fdServerSocket = new LocalServerSocket("fdsock");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        LocalSocket fdSocket = fdServerSocket.accept();
                        try {
                            passFileDescriptors(fdSocket, tunPFD.getFileDescriptor());
                        } finally {
                            fdSocket.close();
                        }
                    } catch (Exception e) {
                        LogUtils.e("fdsock failed " + e, e);
                    }
                }
            }).start();
            updateStatus("Started Vpn Mode with Limited Functions");
            sendBroadcast(new LaunchedIntent(true));
        } catch (Exception e) {
            LogUtils.e("establish failed", e);
        }
    }

    private void passFileDescriptors(LocalSocket fdSocket, FileDescriptor tunFD) throws Exception {
        OutputStream outputStream = fdSocket.getOutputStream();
        InputStream inputStream = fdSocket.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream), 1);
        fdSocket.setFileDescriptorsForSend(new FileDescriptor[]{tunFD});
        outputStream.write('*');
        while (isRunning()) {
            String request = reader.readLine();
            LogUtils.i("fdsock request: " + request);
            String[] parts = request.split(",");
            if ("UDP".equals(parts[0])) {
                passUdpFileDescriptor(fdSocket, outputStream);
            } else if ("TCP".equals(parts[0])) {
                String dstIp = parts[1];
                int dstPort = Integer.parseInt(parts[2]);
                passTcpFileDescriptor(fdSocket, outputStream, dstIp, dstPort);
            } else {
                throw new UnsupportedOperationException("fdsock unable to handle: " + request);
            }
        }
    }

    public static boolean isRunning() {
        return fdServerSocket != null;
    }

    private void passTcpFileDescriptor(
            LocalSocket fdSocket, OutputStream outputStream,
            String dstIp, int dstPort) throws Exception {
        Socket sock = new Socket();
        sock.setTcpNoDelay(true); // force file descriptor being created
        if (protect(sock)) {
            sock.connect(new InetSocketAddress(dstIp, dstPort), 5000);
            FileDescriptor fd = (FileDescriptor) sock.getClass().getMethod("getFileDescriptor$").invoke(sock);
            fdSocket.setFileDescriptorsForSend(new FileDescriptor[]{fd});
            outputStream.write('*');
        } else {
            LogUtils.e("protect tcp socket failed");
        }
    }

    private void passUdpFileDescriptor(LocalSocket fdSocket, OutputStream outputStream) throws Exception {
        DatagramSocket sock = new DatagramSocket();
        if (protect(sock)) {
            FileDescriptor fd = (FileDescriptor) sock.getClass().getMethod("getFileDescriptor$").invoke(sock);
            fdSocket.setFileDescriptorsForSend(new FileDescriptor[]{fd});
            outputStream.write('*');
        } else {
            LogUtils.e("protect udp socket failed");
        }
    }

    private void stopVpn() {
        try {
            fdServerSocket.close();
        } catch (IOException e) {
            LogUtils.e("failed to stop fdsock", e);
        }
        fdServerSocket = null;
    }

    private void updateStatus(String status) {
        sendBroadcast(new UpdateStatusIntent(status));
    }
}

