package fq.router;

import android.util.Log;
import fq.router.utils.IOUtils;
import fq.router.utils.ShellUtils;

import java.io.File;

public class ManagerProcess {

    public static void kill() throws Exception {
        int processId = findProcessId();
        if (processId > 0) {
            Log.i("fqrouter", "kill manager process " + processId);
            ShellUtils.sudo("/system/bin/kill " + processId);
        }
    }

    private static int findProcessId() {
        File PROC = new File("/proc");
        for (File file : PROC.listFiles()) {
            String commandline = getCommandline(file);
            if (null != commandline && commandline.contains("manager/main.py")) {
                int processId = Integer.parseInt(file.getName());
                Log.i("fqrouter", "found manager process: " + processId);
                return processId;
            }
        }
        Log.i("fqrouter", "manager process not found");
        return 0;
    }

    private static String getCommandline(File file) {
        if (!file.isDirectory()) {
            return null;
        }
        File cmdlineFile = new File(file, "cmdline");
        if (cmdlineFile.exists()) {
            return IOUtils.readFromFile(cmdlineFile);
        }
        return null;
    }
}
