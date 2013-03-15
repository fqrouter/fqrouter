package fq.router;

import android.content.res.AssetManager;
import android.util.Log;
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

    private static boolean ping() {
        try {
            String content = IOUtils.readAll(new URL("http://127.0.0.1:8888/ping").openStream());
            if ("PONG".equals(content)) {
                return true;
            } else {
                Log.e("fqrouter", "ping failed: " + content);
                return false;
            }
        } catch (Exception e) {
            Log.e("fqrouter", "ping failed", e);
            return false;
        }
    }

    @Override
    public void run() {
        statusUpdater.appendLog("supervisor thread started");
        try {
            if (!deployer.deploy()) {
                return;
            }
            boolean shouldWait = launchManager();
            if (shouldWait && !waitForManager()) {
                return;
            }
            statusUpdater.activateManageButton();
        } finally {
            statusUpdater.appendLog("supervisor thread stopped");
        }
    }

    private boolean launchManager() {
        if (ping()) {
            statusUpdater.appendLog("manager is already running");
            return false;
        }
        try {
            statusUpdater.appendLog("try to kill manager process before relaunch");
            ManagerProcess.kill();
        } catch (Exception e) {
            Log.e("fqrouter", "failed to kill manager process before relaunch", e);
            statusUpdater.appendLog("failed to kill manager process before relaunch");
        }
        statusUpdater.appendLog("starting launcher thread");
        new Thread(new Launcher(statusUpdater)).start();
        return true;
    }

    private boolean waitForManager() {
        for (int i = 0; i < 10; i++) {
            sleepOneSecond();
            if (ping()) {
                return true;
            }
        }
        statusUpdater.reportError("Timed out", null);
        return false;
    }
}
