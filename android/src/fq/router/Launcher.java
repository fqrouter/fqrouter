package fq.router;

import android.util.Log;
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
                launch();
            } catch (Exception e) {
                Log.e("fqrouter", "failed to launch manager", e);
            }
        } finally {
            statusUpdater.appendLog("launcher thread stopped");
        }
    }

    private void launch() throws Exception {
        statusUpdater.updateStatus("Launching manager");
        ShellUtils.sudo("PYTHONHOME=" + Deployer.PYTHON_DIR + " " +
                Deployer.BUSYBOX_FILE + " sh " + Deployer.PYTHON_LAUNCHER + " " + Deployer.MANAGER_MAIN_PY);
    }
}
