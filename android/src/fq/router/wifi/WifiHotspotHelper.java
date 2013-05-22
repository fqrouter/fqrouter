package fq.router.wifi;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.preference.PreferenceManager;
import fq.router.feedback.AppendLogIntent;
import fq.router.feedback.UpdateStatusIntent;
import fq.router.utils.HttpUtils;
import fq.router.utils.LogUtils;

import java.lang.reflect.Method;
import java.net.URLEncoder;

public class WifiHotspotHelper {

    public static final String MODE_WIFI_REPEATER = "wifi-repeater";
    public static final String MODE_TRADITIONAL_WIFI_HOTSPOT = "traditional-wifi-hotspot";
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


    public boolean start(String wifiHotspotMode) {
        try {
            if (MODE_WIFI_REPEATER.equals(wifiHotspotMode)) {
                if (Build.VERSION.SDK_INT < 14) {
                    appendLog("Android 4.0 or above is required to start wifi repeater, " +
                            "you may use 'Pick & Play' or traditional wifi hotspot instead.");
                    return false;
                }
                startWifiRepeater();
            } else {
                startTraditionalWifiHotspot();
            }
            updateStatus("Started wifi hotspot");
            appendLog("SSID: " + getSSID());
            appendLog("PASSWORD: " + getPassword());
            return true;
        } catch (HttpUtils.Error e) {
            appendLog("error: " + e.output);
            reportStartFailure(wifiHotspotMode, e);
        } catch (Exception e) {
            reportStartFailure(wifiHotspotMode, e);
        }
        return false;
    }

    private void reportStartFailure(String wifiHotspotMode, Exception e) {
        reportError("failed to start wifi hotspot as " + wifiHotspotMode, e);
        stop();
    }

    private void startWifiRepeater() throws Exception {
        updateStatus("Starting wifi hotspot");
        HttpUtils.post("http://127.0.0.1:8318/wifi/start",
                "ssid=" + URLEncoder.encode(getSSID(), "UTF-8") +
                        "&password=" + URLEncoder.encode(getPassword(), "UTF-8"));
    }

    private void startTraditionalWifiHotspot() {
        try {
            setWifiApEnabled(true);
            if (!setup()) {
                throw new RuntimeException("failed to setup network");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setWifiApEnabled(boolean enabled) throws Exception {
        WifiManager wifiManager = getWifiManager();
        Method setWifiApEnabledMethod = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
        WifiConfiguration wifiConfig = new WifiConfiguration();
        wifiConfig.SSID = getSSID();
        wifiConfig.allowedKeyManagement.set(WifiConfiguration.AuthAlgorithm.SHARED);
        wifiConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        wifiConfig.preSharedKey = getPassword();
        wifiManager.setWifiEnabled(false);
        Thread.sleep(1500);
        setWifiApEnabledMethod.invoke(wifiManager, wifiConfig, enabled);
    }

    private String getSSID() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString("WifiHotspotSSID", "fqrouter");
    }

    private String getPassword() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString("WifiHotspotPassword", "12345678");
    }

    public boolean setup() {
        try {
            appendLog("Setup wifi hotspot network");
            HttpUtils.post("http://127.0.0.1:8318/wifi/setup");
            return true;
        } catch (HttpUtils.Error e) {
            appendLog("error: " + e.output);
            reportSetupFailure(e);
        } catch (Exception e) {
            reportSetupFailure(e);
        }
        return false;
    }

    private void reportSetupFailure(Exception e) {
        reportError("failed to setup existing wifi hotspot", e);
        stop();
    }

    public void stop() {
        try {
            updateStatus("Stopping wifi hotspot");
            HttpUtils.post("http://127.0.0.1:8318/wifi/stop");
            try {
                setWifiApEnabled(false);
            } catch (Exception e) {
                LogUtils.e("failed to disable wifi ap", e);
            }
        } catch (Exception e) {
            reportError("failed to stop wifi hotspot", e);
        }
        WifiManager wifiManager = getWifiManager();
        wifiManager.setWifiEnabled(false);
        wifiManager.setWifiEnabled(true);
        updateStatus("Stopped wifi hotspot");
    }

    private WifiManager getWifiManager() {
        return (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    }

    private void reportError(final String msg, Exception e) {
        if (null == e) {
            LogUtils.e(msg);
        } else {
            LogUtils.e(msg, e);
        }
        updateStatus("Error: " + msg);
    }


    private void appendLog(String log) {
        context.sendBroadcast(new AppendLogIntent(log));
    }

    private void updateStatus(String status) {
        context.sendBroadcast(new UpdateStatusIntent(status));
    }
}
