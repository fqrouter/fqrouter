package fq.router;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;
import fq.router.utils.HttpUtils;

import java.lang.reflect.Method;

public class WifiHotspot {

    private final String MODE_WIFI_REPEATER = "wifi repeater";
    private final String MODE_TRADITIONAL_WIFI_HOTSPOT = "traditional wifi hotspot";
    private final StatusUpdater statusUpdater;

    public WifiHotspot(StatusUpdater statusUpdater) {
        this.statusUpdater = statusUpdater;
    }

    public static boolean isStarted() {
        try {
            return "TRUE".equals(HttpUtils.get("http://127.0.0.1:8318/wifi/started"));

        } catch (Exception e) {
            Log.e("fqrouter", "failed to check wifi hotspot is started", e);
            return false;
        }
    }

    public void start() {
        String wifiHotspotMode = isWifiConnected() ? MODE_WIFI_REPEATER : MODE_TRADITIONAL_WIFI_HOTSPOT;
        try {
            if (MODE_WIFI_REPEATER.equals(wifiHotspotMode)) {
                startWifiRepeater();
            } else {
                startTraditionalWifiHotspot();
            }
            statusUpdater.showWifiHotspotToggleButton(true);
        } catch (HttpUtils.Error e) {
            statusUpdater.appendLog("error: " + e.output);
            reportFailure(wifiHotspotMode, e);
        } catch (Exception e) {
            reportFailure(wifiHotspotMode, e);
        }
    }

    private void reportFailure(String wifiHotspotMode, Exception e) {
        statusUpdater.reportError("failed to start wifi hotspot as " + wifiHotspotMode, e);
        stop();
        statusUpdater.showWifiHotspotToggleButton(false);
    }

    private void startWifiRepeater() throws Exception {
        statusUpdater.updateStatus("Starting wifi hotspot");
        HttpUtils.post("http://127.0.0.1:8318/wifi/start");
        statusUpdater.updateStatus("Started wifi hotspot");
        statusUpdater.appendLog("SSID: spike");
        statusUpdater.appendLog("PASSWORD: 12345678");
    }

    private void startTraditionalWifiHotspot() {
        try {

            WifiManager wifiManager = getWifiManager();
            Method setWifiApEnabledMethod = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            WifiConfiguration wifiConfig = new WifiConfiguration();
            wifiConfig.SSID = "StockAp";
            wifiConfig.allowedKeyManagement.set(WifiConfiguration.AuthAlgorithm.SHARED);
            wifiConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            wifiConfig.preSharedKey = "12345678";
            wifiManager.setWifiEnabled(false);
            setWifiApEnabledMethod.invoke(wifiManager, wifiConfig, true);
            HttpUtils.post("http://127.0.0.1:8318/wifi/setup");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isWifiConnected() {
        Context context = statusUpdater.getBaseContext();
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected();
    }

    public void stop() {
        try {
            statusUpdater.updateStatus("Stopping wifi hotspot");
            HttpUtils.post("http://127.0.0.1:8318/wifi/stop");
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
