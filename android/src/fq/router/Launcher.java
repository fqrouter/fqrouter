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
                statusUpdater.updateStatus("Launching manager");
                executePython(false, Deployer.MANAGER_MAIN_PY.getAbsolutePath());
            } catch (Exception e) {
                statusUpdater.appendLog("failed to launch manager");
                Log.e("fqrouter", "failed to launch manager", e);
            }
        } finally {
            statusUpdater.appendLog("launcher thread stopped");
        }
    }

    public String executePython(boolean returnsOutput, String command) throws Exception {
        return ShellUtils.sudo(returnsOutput, "FQROUTER_VERSION=" + statusUpdater.getMyVersion() + " PYTHONHOME=" + Deployer.PYTHON_DIR + " " +
                Deployer.BUSYBOX_FILE + " sh " + Deployer.PYTHON_LAUNCHER + " " + command);
    }
}
