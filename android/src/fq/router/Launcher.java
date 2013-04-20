package fq.router;

import android.util.Log;
import fq.router.utils.ShellUtils;

public class Launcher implements Runnable {
    private final StatusUpdater statusUpdater;
    private final Deployer deployer;

    public Launcher(StatusUpdater statusUpdater, Deployer deployer) {
        this.statusUpdater = statusUpdater;
        this.deployer = deployer;
    }

    @Override
    public void run() {
        statusUpdater.appendLog("launcher thread started");
        try {
            try {
                statusUpdater.updateStatus("Launching manager");
                launch();
            } catch (Exception e) {
                statusUpdater.appendLog("failed to launch manager");
                Log.e("fqrouter", "failed to launch manager", e);
                tryAgain();
            }
        } finally {
            statusUpdater.appendLog("launcher thread stopped");
        }
    }

    private void tryAgain() {
        try {
            statusUpdater.appendLog("link libraries to /system/lib");
            deployer.linkLibs();
        } catch (Exception e) {
            Log.e("fqrouter", "failed to link libs", e);
            return;
        }
        try {
            statusUpdater.appendLog("launch again");
            launch();
        } catch (Exception e) {
            statusUpdater.appendLog("failed to launch manager again");
            Log.e("fqrouter", "failed to launch manager again, after link libs", e);
        }
    }

    private void launch() throws Exception {
        ShellUtils.sudo("FQROUTER_VERSION=" + statusUpdater.getMyVersion() + " PYTHONHOME=" + Deployer.PYTHON_DIR + " " +
                Deployer.BUSYBOX_FILE + " sh " + Deployer.PYTHON_LAUNCHER + " " + Deployer.MANAGER_MAIN_PY);
    }
}
