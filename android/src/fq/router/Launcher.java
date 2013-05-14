package fq.router;

import fq.router.utils.LogUtils;
import fq.router.utils.ShellUtils;

public class Launcher implements Runnable {
    private final StatusUpdater statusUpdater;

    public Launcher(StatusUpdater statusUpdater) {
        this.statusUpdater = statusUpdater;
    }

    @Override
    public void run() {
        statusUpdater.appendLog("launcher thread started");
        try {
            try {
                statusUpdater.updateStatus("Launching manager");
                executeManager();
            } catch (Exception e) {
                statusUpdater.appendLog("failed to launch manager");
                LogUtils.e("failed to launch manager", e);
            }
        } finally {
            statusUpdater.appendLog("launcher thread stopped");
        }
    }

    private String executeManager() throws Exception {
        String runCommand = Deployer.BUSYBOX_FILE + " sh " + Deployer.PYTHON_LAUNCHER + " " +
                Deployer.MANAGER_MAIN_PY.getAbsolutePath() + " > /data/data/fq.router/current-python.log 2>&1";
        return ShellUtils.sudo("FQROUTER_VERSION=" + statusUpdater.getMyVersion() +
                " PYTHONHOME=" + Deployer.PYTHON_DIR + " " + runCommand);
    }
}
