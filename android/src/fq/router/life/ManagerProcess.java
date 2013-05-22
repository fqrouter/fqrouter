package fq.router.life;

import fq.router.life.Deployer;
import fq.router.utils.LogUtils;
import fq.router.utils.ShellUtils;

public class ManagerProcess {

    public static void kill() throws Exception {
        kill("python");
        ShellUtils.sudo("PYTHONHOME=" + Deployer.PYTHON_DIR + " " +
                Deployer.BUSYBOX_FILE + " sh " + Deployer.PYTHON_LAUNCHER + " " + Deployer.MANAGER_MAIN_PY + " clean");
    }

    private static boolean kill(String executable) throws Exception {
        if (!exists(executable)) {
            LogUtils.i("no " + executable + " process to kill");
            return true;
        }
        LogUtils.i("killall " + executable);
        ShellUtils.sudo("/data/data/fq.router/busybox killall " + executable);
        for (int i = 0; i < 10; i++) {
            if (exists()) {
                Thread.sleep(3000);
            } else {
                LogUtils.i("killall " + executable + " done cleanly");
                return true;
            }
        }
        LogUtils.e("killall " + executable + " by force");
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
