package fq.router2.wifi_repeater;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.SystemClock;
import fq.router2.utils.LogUtils;

public class WifiGuardService extends Service implements WifiRepeaterChangedIntent.Handler {

    private WifiManager.WifiLock wifiLock;
    private PendingIntent checkWifi;
    private boolean restarting = true;

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
        WifiRepeaterChangedIntent.register(this);
        try {
            checkWifi = PendingIntent.getService(
                    this, 0, new Intent(this, CheckWifiRepeaterService.class), 0);
            AlarmManager alarm = (AlarmManager) getSystemService(ALARM_SERVICE);
            alarm.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(), 5 * 60 * 1000, checkWifi);
            LogUtils.i("set alarm to check wifi");
        } catch (Exception e) {
            LogUtils.e("failed to set alarm", e);
        }
    }

    @Override
    public void onDestroy() {
        try {
            AlarmManager alarm = (AlarmManager) getSystemService(ALARM_SERVICE);
            alarm.cancel(checkWifi);
            LogUtils.i("cancelled alarm to check wifi");
        } catch (Exception e) {
            LogUtils.e("failed to cancel alarm", e);
        }
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

    @Override
    public void onWifiRepeaterChanged(boolean isStarted) {
        if (isStarted) {
            restarting = false;
            LogUtils.i("wifi repeater is working OK");
        } else {
            if (restarting) {
                LogUtils.e("!!! detected wifi repeater died, and restart did not fix it !!!");
                stopService(new Intent(this, WifiGuardService.class));
                return;
            }
            LogUtils.e("!!! detected wifi repeater died, restart now !!!");
            restarting = true;
            StartWifiRepeaterService.execute(this);
        }
    }
}
