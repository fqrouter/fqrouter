package fq.router.life;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import fq.router.feedback.AppendLogIntent;
import fq.router.feedback.UpdateStatusIntent;
import fq.router.utils.HttpUtils;
import fq.router.utils.LogUtils;
import fq.router.utils.ShellUtils;

public class LaunchService extends IntentService {
    public LaunchService() {
        super("Launch");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (launch()) {
            updateStatus("Started, f**k censorship");
            sendBroadcast(new LaunchedIntent());
        }
    }

    private boolean launch() {
        if (ping()) {
            appendLog("manager is already running");
            return true;
        }
        try {
            updateStatus("Kill existing manager process");
            appendLog("try to kill manager process before launch");
            ManagerProcess.kill();
        } catch (Exception e) {
            LogUtils.e("failed to kill manager process before launch", e);
            appendLog("failed to kill manager process before launch");
        }
        Deployer deployer = new Deployer(this);
        if (!deployer.deploy()) {
            if (deployer.isRooted()) {
                reportError("failed to deploy", null);
            } else {
                reportError("[ROOT] is required", null);
                appendLog("What is [ROOT]: http://en.wikipedia.org/wiki/Android_rooting");
            }
            return false;
        }
        updateStatus("Launching...");
        try {
            Process process = executeManager();
            for (int i = 0; i < 30; i++) {
                if (ping()) {
                    return true;
                }
                if (hasProcessExited(process)) {
                    reportError("manager quit", null);
                    return false;
                }
                sleepOneSecond();
            }
            reportError("timed out", null);
        } catch (Exception e) {
            reportError("failed to launch", e);
        }
        return false;
    }


    private boolean hasProcessExited(Process process) {
        try {
            process.exitValue();
            return true;
        } catch (IllegalThreadStateException e) {
            return false;
        }
    }

    private Process executeManager() throws Exception {
        String runCommand = Deployer.BUSYBOX_FILE + " sh " + Deployer.PYTHON_LAUNCHER + " " +
                Deployer.MANAGER_MAIN_PY.getAbsolutePath() + " > /data/data/fq.router/current-python.log 2>&1";
        return ShellUtils.sudoNotWait("FQROUTER_VERSION=" + getMyVersion(this) +
                " PYTHONHOME=" + Deployer.PYTHON_DIR + " " + runCommand);
    }

    public static String getMyVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionName;
        } catch (Exception e) {
            LogUtils.e("failed to get package info", e);
            return "Unknown";
        }
    }


    private static void sleepOneSecond() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean ping() {
        try {
            String content = HttpUtils.get("http://127.0.0.1:8318/ping");
            if ("PONG".equals(content)) {
                return true;
            } else {
                LogUtils.e("ping failed: " + content);
                return false;
            }
        } catch (HttpUtils.Error e) {
            LogUtils.e("ping failed: [" + e.responseCode + "] " + e.output);
            return false;
        } catch (Exception e) {
            LogUtils.e("ping failed: " + e);
            return false;
        }
    }

    private void reportError(final String msg, Exception e) {
        if (null == e) {
            LogUtils.e(msg);
        } else {
            LogUtils.e(msg, e);
        }
        updateStatus("Error: " + msg);
    }

    private void appendLog(String log) {
        sendBroadcast(new AppendLogIntent(log));
    }

    private void updateStatus(String status) {
        sendBroadcast(new UpdateStatusIntent(status));
    }

    public static void execute(Context context) {
        context.startService(new Intent(context, LaunchService.class));
    }
}
