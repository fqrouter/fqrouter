package fq.router.life;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.preference.PreferenceManager;
import fq.router.feedback.AppendLogIntent;
import fq.router.feedback.HandleFatalErrorIntent;
import fq.router.feedback.UpdateStatusIntent;
import fq.router.utils.HttpUtils;
import fq.router.utils.IOUtils;
import fq.router.utils.LogUtils;
import fq.router.utils.ShellUtils;
import fq.router.vpn.SocksVpnService;

import java.io.File;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

public class LaunchService extends IntentService {
    private final static String CONFIG_FILE_PATH = "/data/data/fq.router/config";

    public static Class SOCKS_VPN_SERVICE_CLASS;

    static {
        try {
            SOCKS_VPN_SERVICE_CLASS = LaunchService.class.forName("fq.router.vpn.SocksVpnService");
        } catch (ClassNotFoundException e) {
            LogUtils.e("failed to load SocksVpnService.class", e);
        }
    }

    public LaunchService() {
        super("Launch");
    }

    public static boolean isVpnRunning() {
        return SOCKS_VPN_SERVICE_CLASS != null && SocksVpnService.isRunning();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        appendLog("ver: " + getMyVersion(this));
        appendLog("rooted: " + ShellUtils.checkRooted());
        if (isVpnRunning()) {
            appendLog("manager is already running");
            reportStated(true);
            return;
        }
        if (ping(false)) {
            appendLog("manager is already running");
            reportStated(false);
            return;
        }
        if (ping(true)) {
            updateStatus("Restart manager");
            try {
                ManagerProcess.kill();
                Thread.sleep(1000);
                LaunchService.execute(this);
            } catch (Exception e) {
                handleFatalError("failed to stop exiting process", e);
            }
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
            return false;
        }
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        updateConfigFile(preferences.getAll());
        updateStatus("Launching...");
        if (ShellUtils.checkRooted()) {
            return launch(false);
        } else {
            if (Build.VERSION.SDK_INT < 14) {
                handleFatalError("[ROOT] is required", null);
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
                    handleFatalError("manager quit", null);
                    return false;
                }
                sleepOneSecond();
            }
            handleFatalError("timed out", null);
        } catch (Exception e) {
            handleFatalError("failed to launch", e);
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
        }};
        env.putAll(ShellUtils.pythonEnv());
        if (isVpnMode) {
            Process process = ShellUtils.executeNoWait(env, ShellUtils.BUSYBOX_FILE.getCanonicalPath(), "sh");
            OutputStreamWriter stdin = new OutputStreamWriter(process.getOutputStream());
            try {
                String command = Deployer.PYTHON_LAUNCHER + " " + Deployer.MANAGER_VPN_PY.getAbsolutePath() +
                        " > /data/data/fq.router/current-python.log 2>&1";
                LogUtils.i("write to stdin: " + command);
                stdin.write(command);
                stdin.write("\nexit\n");
            } finally {
                stdin.close();
            }
            return process;
        } else {
            return ShellUtils.sudoNoWait(env, Deployer.PYTHON_LAUNCHER + " " +
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

    private void handleFatalError(String message, Exception e) {
        sendBroadcast(new HandleFatalErrorIntent(message, e));
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
