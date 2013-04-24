package fq.router;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.util.Log;
import fq.router.utils.HttpUtils;

import java.lang.reflect.Method;
import java.net.URLEncoder;

public class WifiHotspot {

    public static final String MODE_WIFI_REPEATER = "wifi-repeater";
    public static final String MODE_TRADITIONAL_WIFI_HOTSPOT = "traditional-wifi-hotspot";
    private final StatusUpdater statusUpdater;

    public WifiHotspot(StatusUpdater statusUpdater) {
        this.statusUpdater = statusUpdater;
    }

    public static boolean isStarted() {
        try {
            return "TRUE".equals(HttpUtils.get("http://10.1.2.3:8318/wifi/started"));

        } catch (Exception e) {
            Log.e("fqrouter", "failed to check wifi hotspot is started", e);
            return false;
        }
    }

    public boolean start(String wifiHotspotMode) {
        statusUpdater.appendLog("wifi hotspot mode: " + wifiHotspotMode);
        try {
            if (MODE_WIFI_REPEATER.equals(wifiHotspotMode)) {
                startWifiRepeater();
            } else {
                startTraditionalWifiHotspot();
            }
            statusUpdater.updateStatus("Started wifi hotspot");
            statusUpdater.appendLog("SSID: " + getSSID());
            statusUpdater.appendLog("PASSWORD: " + getPassword());
            statusUpdater.showWifiHotspotToggleButton(true);
            return true;
        } catch (HttpUtils.Error e) {
            statusUpdater.appendLog("error: " + e.output);
            reportStartFailure(wifiHotspotMode, e);
        } catch (Exception e) {
            reportStartFailure(wifiHotspotMode, e);
        }
        return false;
    }

    private void reportStartFailure(String wifiHotspotMode, Exception e) {
        statusUpdater.reportError("failed to start wifi hotspot as " + wifiHotspotMode, e);
        stop();
        statusUpdater.showWifiHotspotToggleButton(false);
    }

    private void startWifiRepeater() throws Exception {
        statusUpdater.updateStatus("Starting wifi hotspot");
        HttpUtils.post("http://10.1.2.3:8318/wifi/start",
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
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(statusUpdater.getBaseContext());
        return preferences.getString("WifiHotspotSSID", "fqrouter");
    }

    private String getPassword() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(statusUpdater.getBaseContext());
        return preferences.getString("WifiHotspotPassword", "12345678");
    }

    public boolean isConnected() {
        SupplicantState state = getWifiManager().getConnectionInfo().getSupplicantState();
        return SupplicantState.ASSOCIATED.equals(state) || SupplicantState.COMPLETED.equals(state);
    }

    private String getWifiIp() {
        int ip = getWifiManager().getConnectionInfo().getIpAddress();
        String ipText = String.format("%d.%d.%d.%d",
                (ip & 0xff),
                (ip >> 8 & 0xff),
                (ip >> 16 & 0xff),
                (ip >> 24 & 0xff));
        Log.i("fqrouter", "wifi ip: " + ip);
        return ipText;
    }

    public boolean setup() {
        try {
            if ("10.24.1.1".equals(getWifiIp())) {
                return false;
            }
            statusUpdater.updateStatus("Setup wifi hotspot network");
            HttpUtils.post("http://10.1.2.3:8318/wifi/setup");
            return true;
        } catch (HttpUtils.Error e) {
            statusUpdater.appendLog("error: " + e.output);
            reportSetupFailure(e);
        } catch (Exception e) {
            reportSetupFailure(e);
        }
        return false;
    }

    private void reportSetupFailure(Exception e) {
        statusUpdater.reportError("failed to setup existing wifi hotspot", e);
        stop();
        statusUpdater.showWifiHotspotToggleButton(false);
    }

    public void stop() {
        try {
            statusUpdater.updateStatus("Stopping wifi hotspot");
            try {
                setWifiApEnabled(false);
            } catch (Exception e) {
                Log.e("fqrouter", "failed to disable wifi ap", e);
            }
            HttpUtils.post("http://10.1.2.3:8318/wifi/stop");
        } catch (Exception e) {
            statusUpdater.reportError("failed to stop wifi hotspot", e);
        }
        WifiManager wifiManager = getWifiManager();
        wifiManager.setWifiEnabled(false);
        wifiManager.setWifiEnabled(true);
        statusUpdater.updateStatus("Stopped wifi hotspot");
        statusUpdater.showWifiHotspotToggleButton(false);
    }

    private WifiManager getWifiManager() {
        return (WifiManager) statusUpdater.getBaseContext().getSystemService(Context.WIFI_SERVICE);
    }
}
