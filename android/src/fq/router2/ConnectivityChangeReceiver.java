package fq.router2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import fq.router2.life_cycle.LaunchService;
import fq.router2.utils.*;

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
                            clearStates();
                        } catch (Exception e) {
                            LogUtils.e("failed to clear states", e);
                        }
                    }
                }).start();
            } else if (intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, Boolean.FALSE)) {
                LogUtils.i("Received event: no network connectivity");
            }
        }
    }

    private void clearStates() throws Exception {
        HttpUtils.post("http://127.0.0.1:" + ConfigUtils.getHttpManagerPort() + "/clear-states");
    }
}
