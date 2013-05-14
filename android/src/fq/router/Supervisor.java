package fq.router;

import fq.router.utils.HttpUtils;
import fq.router.utils.LogUtils;

public class Supervisor implements Runnable {

    private final Deployer deployer;
    private final StatusUpdater statusUpdater;

    public Supervisor(StatusUpdater statusUpdater) {
        this.statusUpdater = statusUpdater;
        deployer = new Deployer(statusUpdater);
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
            String content = HttpUtils.get("http://127.0.0.1:8318/ping");
            if ("PONG".equals(content)) {
                return true;
            } else {
                LogUtils.e("ping failed: " + content);
                return false;
            }
        } catch (HttpUtils.Error e) {
            LogUtils.e("ping failed: [" + e.responseCode + "] " + e.output);
            return false;
        } catch (Exception e) {
            LogUtils.e("ping failed");
            return false;
        }
    }

    @Override
    public void run() {
        statusUpdater.appendLog("supervisor thread started");
        try {
            try {
                statusUpdater.updateStatus("Kill existing manager process");
                statusUpdater.appendLog("try to kill manager process before launch");
                ManagerProcess.kill();
            } catch (Exception e) {
                LogUtils.e("failed to kill manager process before launch", e);
                statusUpdater.appendLog("failed to kill manager process before launch");
            }
            if (!deployer.deploy()) {
                if (deployer.isRooted()) {
                    statusUpdater.reportError("failed to deploy", null);
                } else {
                    statusUpdater.reportError("[ROOT] is required", null);
                    statusUpdater.appendLog("What is [ROOT]: http://en.wikipedia.org/wiki/Android_rooting");
                }
                return;
            }
            boolean shouldWait = launchManager();
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

    private boolean launchManager() {
        if (ping()) {
            statusUpdater.appendLog("manager is already running");
            return false;
        }
        statusUpdater.appendLog("starting launcher thread");
        new Thread(new Launcher(statusUpdater)).start();
        return true;
    }

    private boolean waitForManager() {
        for (int i = 0; i < 30; i++) {
            sleepOneSecond();
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
            LogUtils.e("check updates failed", e);
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
