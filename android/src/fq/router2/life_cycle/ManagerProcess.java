package fq.router2.life_cycle;

import fq.router2.utils.IOUtils;
import fq.router2.utils.LogUtils;
import fq.router2.utils.ShellUtils;

import java.io.File;

public class ManagerProcess {

    public static void kill() throws Exception {
        try {
            if ("run-needs-su".equals(getRunMode())) {
                ShellUtils.execute(
                        ShellUtils.pythonEnv(), Deployer.PYTHON_LAUNCHER.getAbsolutePath(),
                        Deployer.MANAGER_MAIN_PY.getAbsolutePath(), "clean");
            } else {
                ShellUtils.sudo(
                        ShellUtils.pythonEnv(), Deployer.PYTHON_LAUNCHER.getAbsolutePath(),
                        Deployer.MANAGER_MAIN_PY.getAbsolutePath(), "clean");
            }
        } catch (Exception e) {
            LogUtils.e("failed to clean", e);
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
            if (ShellUtils.sudo("/data/data/fq.router2/busybox", "killall", "-0", "python").contains(
                    "no process killed")) {
                return false;
            } else {
                return true;
            }
        } catch (Exception e) {
            return false;
        }
    }

    public static String getRunMode() throws Exception {
        File runModeCacheFile = new File("/data/data/fq.router2/etc/run-mode");
        if (runModeCacheFile.exists()) {
            return IOUtils.readFromFile(runModeCacheFile);
        }
        // S4 will fail this test
        try {
            String output = ShellUtils.sudo(ShellUtils.pythonEnv(), Deployer.PYTHON_LAUNCHER +
                    " -c \"import subprocess; print(subprocess.check_output(['" +
                    ShellUtils.BUSYBOX_FILE.getCanonicalPath() + "', 'echo', 'hello']))\"").trim();
            LogUtils.i("get run mode: " + output);
            if (output.contains("hello")) {
                IOUtils.writeToFile(runModeCacheFile, "run-normally");
                return "run-normally";
            } else {
                IOUtils.writeToFile(runModeCacheFile, "run-needs-su");
                return "run-needs-su";
            }
        } catch (Exception e) {
            LogUtils.e("failed to test subprocess", e);
            IOUtils.writeToFile(runModeCacheFile, "run-needs-su");
            return "run-needs-su";
        }
    }
}
