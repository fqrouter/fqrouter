package fq.router.wifi_repeater;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.preference.PreferenceManager;
import fq.router.feedback.HandleFatalErrorIntent;
import fq.router.utils.HttpUtils;
import fq.router.utils.LogUtils;

public class WifiRepeater {

    private final Context context;

    public WifiRepeater(Context context) {
        this.context = context;
    }

    public boolean isStarted() {
        try {
            return "TRUE".equals(HttpUtils.get("http://127.0.0.1:8318/wifi-repeater/is-started"));
        } catch (Exception e) {
            LogUtils.e("failed to check wifi repeater is started", e);
            return false;
        }
    }

    public void start() {
        try {
            if (Build.VERSION.SDK_INT < 14) {
                throw new HandleFatalErrorIntent.Message(
                        "Android 4.0 or above is required to start wifi repeater, " +
                        "you may use 'Pick & Play' instead.");
            }
            startWifiRepeater();
            LogUtils.i("Started wifi repeater");
            LogUtils.i("SSID: " + getSSID());
            LogUtils.i("PASSWORD: " + getPassword());
        } catch (HttpUtils.Error e) {
            LogUtils.e("failed to start wifi repeater", e);
            throw new HandleFatalErrorIntent.Message(e.output);
        } catch (Exception e) {
            LogUtils.e("failed to start wifi repeater", e);
            throw new HandleFatalErrorIntent.Message("failed to start wifi repeater");
        }
    }

    private void reportStartFailure(Exception e) {
        stop();
    }

    private void startWifiRepeater() throws Exception {
        LogUtils.i("Starting wifi repeater");
        HttpUtils.post("http://127.0.0.1:8318/wifi-repeater/start");
    }

    private String getSSID() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString("WifiHotspotSSID", "fqrouter");
    }

    private String getPassword() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString("WifiHotspotPassword", "12345678");
    }

    public void stop() {
        try {
            LogUtils.i("Stopping wifi repeater");
            HttpUtils.post("http://127.0.0.1:8318/wifi-repeater/stop");
        } catch (Exception e) {
            LogUtils.e("failed to stop wifi repeater", e);
        }
        WifiManager wifiManager = getWifiManager();
        wifiManager.setWifiEnabled(false);
        wifiManager.setWifiEnabled(true);
        LogUtils.i("Stopped wifi repeater");
    }

    private WifiManager getWifiManager() {
        return (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    }

    private void handleFatalError(String msg) {
        context.sendBroadcast(new HandleFatalErrorIntent(msg));
    }
}
