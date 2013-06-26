package fq.router2.life_cycle;

import fq.router2.utils.LogUtils;
import fq.router2.utils.ShellUtils;

public class ManagerProcess {

    public static void kill() throws Exception {
        try {
            ShellUtils.sudo(ShellUtils.pythonEnv(), Deployer.PYTHON_LAUNCHER + " " + Deployer.MANAGER_MAIN_PY + " clean");
        } catch (Exception e) {
            LogUtils.e("failed to clean", e);
        }
        if (!exists()) {
            LogUtils.i("no python process to kill");
            return;
        }
        LogUtils.i("killall python");
        ShellUtils.sudo("/data/data/fq.router2/busybox", "killall", "python");
        for (int i = 0; i < 10; i++) {
            if (exists()) {
                Thread.sleep(3000);
            } else {
                LogUtils.i("killall python done cleanly");
                return;
            }
        }
        LogUtils.e("killall python by force");
        ShellUtils.sudo("/data/data/fq.router2/busybox", "killall", "-KILL", "python");
    }

    public static boolean exists() {
        try {
            ShellUtils.sudo("/data/data/fq.router2/busybox", "killall", "-0", "python");
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
