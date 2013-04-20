package fq.router;

import android.content.res.AssetManager;
import android.util.Log;
import fq.router.utils.HttpUtils;
import fq.router.utils.IOUtils;

import java.net.URL;

public class Supervisor implements Runnable {

    private final Deployer deployer;
    private final StatusUpdater statusUpdater;

    public Supervisor(AssetManager assetManager, StatusUpdater statusUpdater) {
        this.statusUpdater = statusUpdater;
        deployer = new Deployer(assetManager, statusUpdater);
    }

    private static void sleepOneSecond() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean ping() {
        try {
            String content = IOUtils.readAll(new URL("http://127.0.0.1:8318/ping").openStream());
            if ("PONG".equals(content)) {
                return true;
            } else {
                Log.e("fqrouter", "ping failed: " + content);
                return false;
            }
        } catch (Exception e) {
            Log.e("fqrouter", "ping failed");
            return false;
        }
    }

    @Override
    public void run() {
        statusUpdater.appendLog("supervisor thread started");
        try {
            try {
                statusUpdater.updateStatus("Kill existing manager process");
                statusUpdater.appendLog("try to kill manager process before relaunch");
                ManagerProcess.kill();
            } catch (Exception e) {
                Log.e("fqrouter", "failed to kill manager process before relaunch", e);
                statusUpdater.appendLog("failed to kill manager process before relaunch");
            }
            if (!deployer.deploy()) {
                return;
            }
            boolean shouldWait = launchManager(deployer);
            if (shouldWait && !waitForManager()) {
                return;
            }
            statusUpdater.updateStatus("Checking updates");
            checkUpdates(statusUpdater);
            statusUpdater.onStarted();
        } finally {
            statusUpdater.appendLog("supervisor thread stopped");
        }
    }

    private boolean launchManager(Deployer deployer) {
        if (ping()) {
            statusUpdater.appendLog("manager is already running");
            return false;
        }
        statusUpdater.appendLog("starting launcher thread");
        new Thread(new Launcher(statusUpdater, deployer)).start();
        return true;
    }

    private boolean waitForManager() {
        for (int i = 0; i < 30; i++) {
            sleepOneSecond();
            if (ping()) {
                return true;
            }
        }
        statusUpdater.reportError("Timed out", null);
        return false;
    }

    public static boolean checkUpdates(StatusUpdater statusUpdater) {
        try {
            String versionInfo = HttpUtils.get("http://127.0.0.1:8318/version/latest");
            String latestVersion = versionInfo.split("\\|")[0];
            String upgradeUrl = versionInfo.split("\\|")[1];
            if (isNewer(latestVersion, statusUpdater.getMyVersion())) {
                statusUpdater.notifyNewerVersion(latestVersion, upgradeUrl);
            } else {
                statusUpdater.appendLog("already running the latest version");
            }
            return true;
        } catch (Exception e) {
            statusUpdater.appendLog("check updates failed");
            Log.e("fqrouter", "check updates failed", e);
            return false;
        }
    }


    private static boolean isNewer(String latestVersion, String currentVersion) {
        int[] latestVersionParts = parseVersion(latestVersion);
        int[] currentVersionParts = parseVersion(currentVersion);
        if (latestVersionParts[0] > currentVersionParts[0]) {
            return true;
        }
        if (latestVersionParts[0] < currentVersionParts[0]) {
            return false;
        }
        if (latestVersionParts[1] > currentVersionParts[1]) {
            return true;
        }
        if (latestVersionParts[1] < currentVersionParts[1]) {
            return false;
        }
        return latestVersionParts[2] > currentVersionParts[2];
    }

    private static int[] parseVersion(String version) {
        String[] parts = version.split("\\.");
        return new int[]{
                Integer.parseInt(parts[0]),
                Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2])
        };
    }
}
