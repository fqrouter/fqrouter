package fq.router;

import android.content.Context;
import android.content.res.AssetManager;

public interface StatusUpdater {
    void updateStatus(String status);

    void appendLog(String log);

    void onStarted();

    void reportError(String msg, Exception e);

    String getMyVersion();

    void notifyNewerVersion(String latestVersion, String upgradeUrl);

    void showWifiHotspotToggleButton(boolean checked);

    Context getBaseContext();

    AssetManager getAssets();
}
