package fq.router;

import android.util.Log;
import fq.router.utils.ShellUtils;

public class ManagerProcess {

    public static void kill() throws Exception {
        if (!exists()) {
            Log.i("fqrouter", "no python process to kill");
            return;
        }
        Log.i("fqrouter", "killall python");
        ShellUtils.sudo("/data/data/fq.router/busybox killall -TERM python");
        for (int i = 0; i < 10; i++) {
            if (exists()) {
                Thread.sleep(1000);
            } else {
                Log.i("fqrouter", "killall python done cleanly");
                return;
            }
        }
        Log.e("fqrouter", "killall python by force");
        ShellUtils.sudo("/data/data/fq.router/busybox killall -KILL python");
        ShellUtils.sudo("PYTHONHOME=" + Deployer.PYTHON_DIR + " " +
                Deployer.BUSYBOX_FILE + " sh " + Deployer.PYTHON_LAUNCHER + " " + Deployer.MANAGER_CLEAN_PY);
    }

    public static boolean exists() {
        try {
            ShellUtils.sudo("/data/data/fq.router/busybox killall -0 python");
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
