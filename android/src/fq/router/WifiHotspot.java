package fq.router;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;
import fq.router.utils.HttpUtils;

public class WifiHotspot {

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
        try {
            statusUpdater.updateStatus("Starting wifi hotspot");
            HttpUtils.post("http://127.0.0.1:8318/wifi/start");
            statusUpdater.updateStatus("Started wifi hotspot");
            statusUpdater.appendLog("SSID: spike");
            statusUpdater.appendLog("PASSWORD: 12345678");
            statusUpdater.showWifiHotspotToggleButton(true);
        } catch (Exception e) {
            statusUpdater.reportError("failed to start wifi hotspot", e);
            stop();
            statusUpdater.showWifiHotspotToggleButton(false);
        }
    }

    public void stop() {
        try {
            statusUpdater.updateStatus("Stopping wifi hotspot");
            HttpUtils.post("http://127.0.0.1:8318/wifi/stop");
        } catch (Exception e) {
            statusUpdater.reportError("failed to stop wifi hotspot", e);
        }
        WifiManager wifiManager = (WifiManager) statusUpdater.getBaseContext().getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(false);
        wifiManager.setWifiEnabled(true);
        statusUpdater.updateStatus("Stopped wifi hotspot");
        statusUpdater.showWifiHotspotToggleButton(false);
    }
}
