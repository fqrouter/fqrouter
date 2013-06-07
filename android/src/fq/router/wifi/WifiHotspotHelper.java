package fq.router.wifi;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.preference.PreferenceManager;
import fq.router.feedback.AppendLogIntent;
import fq.router.feedback.HandleFatalErrorIntent;
import fq.router.feedback.UpdateStatusIntent;
import fq.router.utils.HttpUtils;
import fq.router.utils.LogUtils;

import java.lang.reflect.Method;

public class WifiHotspotHelper {

    private final Context context;

    public WifiHotspotHelper(Context context) {
        this.context = context;
    }

    public boolean isStarted() {
        try {
            return "TRUE".equals(HttpUtils.get("http://127.0.0.1:8318/wifi/started"));
        } catch (Exception e) {
            LogUtils.e("failed to check wifi hotspot is started", e);
            return false;
        }
    }

    public boolean start() {
        try {
            if (Build.VERSION.SDK_INT < 14) {
                appendLog("Android 4.0 or above is required to start wifi repeater, " +
                        "you may use 'Pick & Play' instead.");
                return false;
            }
            startWifiRepeater();
            updateStatus("Started wifi hotspot");
            appendLog("SSID: " + getSSID());
            appendLog("PASSWORD: " + getPassword());
            return true;
        } catch (HttpUtils.Error e) {
            appendLog("error: " + e.output);
            reportStartFailure(e);
        } catch (Exception e) {
            reportStartFailure(e);
        }
        return false;
    }

    private void reportStartFailure(Exception e) {
        handleFatalError("failed to start wifi hotspot", e);
        stop();
    }

    private void startWifiRepeater() throws Exception {
        updateStatus("Starting wifi hotspot");
        HttpUtils.post("http://127.0.0.1:8318/wifi/start");
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
            updateStatus("Stopping wifi hotspot");
            HttpUtils.post("http://127.0.0.1:8318/wifi/stop");
        } catch (Exception e) {
            handleFatalError("failed to stop wifi hotspot", e);
        }
        WifiManager wifiManager = getWifiManager();
        wifiManager.setWifiEnabled(false);
        wifiManager.setWifiEnabled(true);
        updateStatus("Stopped wifi hotspot");
    }

    private WifiManager getWifiManager() {
        return (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    }

    private void handleFatalError(String message, Exception e) {
        context.sendBroadcast(new HandleFatalErrorIntent(message, e));
    }


    private void appendLog(String log) {
        context.sendBroadcast(new AppendLogIntent(log));
    }

    private void updateStatus(String status) {
        context.sendBroadcast(new UpdateStatusIntent(status));
    }
}
