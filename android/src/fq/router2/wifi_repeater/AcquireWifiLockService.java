package fq.router2.wifi_repeater;

import android.app.Service;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import fq.router2.utils.LogUtils;

public class AcquireWifiLockService extends Service {

    private WifiManager.WifiLock wifiLock;
    @Override
    public void onCreate() {
        super.onCreate();
        try {
            WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
            if (null == wifiLock) {
                wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "fqrouter wifi hotspot");
            }
            wifiLock.acquire();
            LogUtils.i("acquired wifi lock");
        } catch (Exception e) {
            LogUtils.e("failed to acquire wifi lock", e);
        }
    }


    @Override
    public void onDestroy() {
        try {
            if (wifiLock.isHeld()) {
                wifiLock.release();
                LogUtils.i("released wifi lock");
            } else {
                LogUtils.e("wifi lock is not held when about to release it");
            }
        } catch (Exception e) {
            LogUtils.e("failed to release wifi lock", e);
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}