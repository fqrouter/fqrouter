package fq.router;

import android.util.Log;
import fq.router.utils.ShellUtils;

public class ManagerProcess {

    public static void kill() throws Exception {
        kill("python");
        kill("redsocks");
        ShellUtils.sudo("PYTHONHOME=" + Deployer.PYTHON_DIR + " " +
                Deployer.BUSYBOX_FILE + " sh " + Deployer.PYTHON_LAUNCHER + " " + Deployer.MANAGER_MAIN_PY + " clean");
    }

    private static boolean kill(String executable) throws Exception {
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
