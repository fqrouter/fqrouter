package fq.router2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import fq.router2.free_internet.ConnectFreeInternetService;
import fq.router2.free_internet.DisconnectFreeInternetService;
import fq.router2.utils.LogUtils;

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
                        ConnectFreeInternetService.connect(context);
                    }
                }).start();
            } else if (intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, Boolean.FALSE)) {
                LogUtils.i("Received event: no network connectivity");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        DisconnectFreeInternetService.disconnect(context);
                    }
                }).start();
            }
        }
    }
}
