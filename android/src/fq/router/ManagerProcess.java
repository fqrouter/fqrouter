package fq.router;

import android.util.Log;
import fq.router.utils.ShellUtils;

public class ManagerProcess {

    public static void kill() throws Exception {
        boolean killedPython = _kill("python");
        boolean killedRedsocks = _kill("redsocks");
        boolean killedAll = killedPython && killedRedsocks;
        if (!killedAll) {
            ShellUtils.sudo("PYTHONHOME=" + Deployer.PYTHON_DIR + " " +
                    Deployer.BUSYBOX_FILE + " sh " + Deployer.PYTHON_LAUNCHER + " " + Deployer.MANAGER_CLEAN_PY);
        }
    }

    private static boolean _kill(String executable) throws Exception {
        if (!exists(executable)) {
            Log.i("fqrouter", "no " + executable + " process to kill");
            return true;
        }
        Log.i("fqrouter", "killall " + executable);
        ShellUtils.sudo("/data/data/fq.router/busybox killall " + executable);
        for (int i = 0; i < 6; i++) {
            if (exists()) {
                Thread.sleep(5000);
            } else {
                Log.i("fqrouter", "killall " + executable + " done cleanly");
                return true;
            }
        }
        Log.e("fqrouter", "killall " + executable + " by force");
        ShellUtils.sudo("/data/data/fq.router/busybox killall -KILL " + executable);
        return false;
    }

    public static boolean exists() {
        return exists("python");
    }

    public static boolean exists(String executable) {
        try {
            ShellUtils.sudo("/data/data/fq.router/busybox killall -0 " + executable);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
