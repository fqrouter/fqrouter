package fq.router.life;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.preference.PreferenceManager;
import fq.router.feedback.AppendLogIntent;
import fq.router.feedback.UpdateStatusIntent;
import fq.router.utils.HttpUtils;
import fq.router.utils.IOUtils;
import fq.router.utils.LogUtils;
import fq.router.utils.ShellUtils;

import java.io.File;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

public class LaunchService extends IntentService {
    private final static String CONFIG_FILE_PATH = "/data/data/fq.router/config";

    public LaunchService() {
        super("Launch");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        appendLog("ver: " + getMyVersion(this));
        if (ping(false)) {
            appendLog("manager is already running");
            reportStated(false);
            return;
        }
        if (ping(true)) {
            appendLog("manager is already running");
            reportStated(true);
            return;
        }
        if (deployAndLaunch()) {
            reportStated(false);
        }
    }

    private void reportStated(boolean isVpnMode) {
        if (isVpnMode) {
            updateStatus("Started in VPN mode");
        } else {
            updateStatus("Started, f**k censorship");
        }
        sendBroadcast(new LaunchedIntent(isVpnMode));
    }

    private boolean deployAndLaunch() {
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
            reportError("failed to deploy", null);
            return false;
        }
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        updateConfigFile(preferences.getAll());
        updateStatus("Launching...");
        if (ShellUtils.CheckRooted()) {
            return launch(false);
        } else {
            if (Build.VERSION.SDK_INT < 14) {
                reportError("[ROOT] is required", null);
                appendLog("What is [ROOT]: http://en.wikipedia.org/wiki/Android_rooting");
                return false;
            }
            if (launch(true)) {
                sendBroadcast(new StartVpnIntent());
            }
            return false;
        }
    }

    private boolean launch(boolean isVpnMode) {
        try {
            Process process = executeManager(isVpnMode);
            for (int i = 0; i < 30; i++) {
                if (ping(isVpnMode)) {
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

    private Process executeManager(boolean isVpnMode) throws Exception {
        Map<String, String> env = new HashMap<String, String>() {{
            put("FQROUTER_VERSION", getMyVersion(LaunchService.this));
            put("PYTHONHOME", Deployer.PYTHON_DIR.getCanonicalPath());
        }};
        if (isVpnMode) {
            Process process = ShellUtils.executeNoWait(env, Deployer.BUSYBOX_FILE.getCanonicalPath(), "sh");
            OutputStreamWriter stdin = new OutputStreamWriter(process.getOutputStream());
            try {
                stdin.write(Deployer.PYTHON_LAUNCHER + " " + Deployer.MANAGER_VPN_PY.getAbsolutePath() +
                        " > /data/data/fq.router/current-python.log 2>&1");
                stdin.write("\nexit\n");
            } finally {
                stdin.close();
            }
            return process;
        } else {
            return ShellUtils.sudoNoWait(env, Deployer.BUSYBOX_FILE + " sh " + Deployer.PYTHON_LAUNCHER + " " +
                    Deployer.MANAGER_MAIN_PY.getAbsolutePath() + " > /data/data/fq.router/current-python.log 2>&1");
        }
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

    public static boolean ping(boolean isVpnMode) {
        try {
            String content = HttpUtils.get("http://127.0.0.1:8318/ping");
            if (isVpnMode ? "VPN PONG".equals(content) : "PONG".equals(content)) {
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

    public static void updateConfigFile(Map<String, ?> settings) {
        try {
            File configFile = new File(CONFIG_FILE_PATH);
            if (configFile.exists()) {
                configFile.delete();
            }
            StringBuilder s = new StringBuilder();
            s.append("[fqrouter]\r\n");
            for (String k : settings.keySet()) {
                s.append(k).append("=").append(settings.get(k)).append("\r\n");
            }
            IOUtils.writeToFile(configFile, s.toString());
        } catch (Exception e) {
            LogUtils.e("failed to update config file", e);
        }
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
