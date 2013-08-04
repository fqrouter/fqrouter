package fq.router2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import fq.router2.life_cycle.LaunchService;
import fq.router2.utils.IOUtils;
import fq.router2.utils.LogUtils;
import fq.router2.utils.ShellUtils;

import java.io.File;

public class ConnectivityChangeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, Intent intent) {
        if (intent.getExtras() != null) {
            NetworkInfo ni = (NetworkInfo) intent.getExtras().get(ConnectivityManager.EXTRA_NETWORK_INFO);
            if (ni != null && ni.getState() == NetworkInfo.State.CONNECTED) {
                LogUtils.i("Received event: connected");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            refreshProxies(context);
                        } catch (Exception e) {
                            LogUtils.e("failed to refresh proxies", e);
                        }
                    }
                }).start();
            } else if (intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, Boolean.FALSE)) {
                LogUtils.i("Received event: no network connectivity");
            }
        }
    }

    private void refreshProxies(Context context) throws Exception {
        int processId = findProcessId();
        if (0 == processId) {
            LogUtils.e("failed to find process id to refresh proxies");
            return;
        }
        if (LaunchService.ping(context, true)) {
            ShellUtils.execute("/data/data/fq.router2/busybox", "kill", "-HUP", String.valueOf(processId));
        } else if (LaunchService.ping(context, false)) {
            ShellUtils.sudo("/data/data/fq.router2/busybox", "kill", "-HUP", String.valueOf(processId));
        }
    }

    private int findProcessId() {
        for (File file : new File("/proc").listFiles()) {
            try {
                int processId = Integer.parseInt(file.getName());
                String cmdline = IOUtils.readFromFile(new File(file, "cmdline"));
                if (cmdline.contains("fqsocks")) {
                    return processId;
                }
                if (cmdline.contains("vpn.py")) {
                    return processId;
                }
            } catch (Exception e) {
                // ignore
            }
        }
        return 0;
    }
}
