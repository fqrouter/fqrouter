package fq.router2.life_cycle;

import android.app.AlertDialog;
import android.app.IntentService;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import fq.router2.feedback.DownloadService;
import fq.router2.feedback.HandleFatalErrorIntent;
import fq.router2.free_internet.SocksVpnService;
import fq.router2.utils.*;
import org.json.JSONObject;

import java.io.File;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

public class LaunchService extends IntentService {
    private final static File FQROUTER_CONFIG_FILE = new File("/data/data/fq.router2/etc/fqrouter.json");

    public static Class SOCKS_VPN_SERVICE_CLASS;

    static {
        try {
            SOCKS_VPN_SERVICE_CLASS = LaunchService.class.forName("fq.router2.free_internet.SocksVpnService");
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
        LogUtils.i("ver: " + getMyVersion(this));
        boolean rooted = ShellUtils.checkRooted();
        LogUtils.i("rooted: " + rooted);
        if (isVpnRunning()) {
            LogUtils.i("manager is already running in vpn mode");
            sendBroadcast(new LaunchedIntent(true));
            return;
        }
        if (ping(false)) {
            LogUtils.i("manager is already running in root mode");
            sendBroadcast(new LaunchedIntent(false));
            return;
        }
        if (ping(true)) {
            LogUtils.i("Restart manager");
            try {
                ManagerProcess.kill();
                Thread.sleep(1000);
                if (ManagerProcess.exists()) {
                    LogUtils.e("failed to restart manager", null);
                } else {
                    LaunchService.execute(this);
                }
            } catch (Exception e) {
                handleFatalError(LogUtils.e("failed to stop exiting process", e));
            }
            return;
        }
        deployAndLaunch();
    }

    private void deployAndLaunch() {
        try {
            LogUtils.i("Kill existing manager process");
            LogUtils.i("try to kill manager process before launch");
            ManagerProcess.kill();
        } catch (Exception e) {
            LogUtils.e("failed to kill manager process before launch", e);
        }
        Deployer deployer = new Deployer(this);
        String fatalError = deployer.deploy();
        if (!fatalError.isEmpty()) {
            handleFatalError(fatalError);
            return;
        }
        updateConfigFile(this);
        LogUtils.i("Launching...");
        if (ShellUtils.checkRooted()) {
            fatalError = launch(false);
            if (fatalError.isEmpty()) {
                sendBroadcast(new LaunchedIntent(false));
            } else {
                handleFatalError(fatalError);
            }
        } else {
            if (Build.VERSION.SDK_INT < 14) {
                handleFatalError("[ROOT] is required");
                return;
            }
            fatalError = launch(true);
            if (fatalError.isEmpty()) {
                sendBroadcast(new LaunchedIntent(true));
            } else {
                handleFatalError(fatalError);
            }
        }
    }

    private String launch(boolean isVpnMode) {
        try {
            Process process = executeManager(isVpnMode);
            for (int i = 0; i < 30; i++) {
                if (ping(isVpnMode)) {
                    return "";
                }
                if (hasProcessExited(process)) {
                    return "manager quit";
                }
                sleepOneSecond();
            }
            return "timed out";
        } catch (Exception e) {
            return LogUtils.e("failed to launch", e);
        }
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
                        " > /data/data/fq.router2/log/current-python.log 2>&1";
                LogUtils.i("write to stdin: " + command);
                stdin.write(command);
                stdin.write("\nexit\n");
            } finally {
                stdin.close();
            }
            return process;
        } else {
            try {
                LogUtils.i(ShellUtils.sudo(
                        env, Deployer.PYTHON_LAUNCHER +
                        " -c \"import os; print 'current user id: %s' % os.getuid()\"").trim());
            } catch (Exception e) {
                LogUtils.e("log current user id failed", e);
            }
            return ShellUtils.sudoNoWait(env, Deployer.PYTHON_LAUNCHER + " " +
                    Deployer.MANAGER_MAIN_PY.getAbsolutePath() + " > /data/data/fq.router2/log/current-python.log 2>&1");
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

    private void handleFatalError(String message) {
        sendBroadcast(new HandleFatalErrorIntent(message));
    }

    public static void updateConfigFile(Context context) {
        try {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            JSONObject configJson = new JSONObject();
            configJson.put("wifi_hotspot_ssid", preferences.getString("WifiHotspotSSID", "fqrouter"));
            configJson.put("wifi_hotspot_password", preferences.getString("WifiHotspotPassword", "p@55word"));
            configJson.put("tcp_scrambler_enabled", preferences.getBoolean("TcpScramblerEnabled", true));
            configJson.put("youtube_scrambler_enabled", preferences.getBoolean("YoutubeScramblerEnabled", true));
            configJson.put("goagent_public_servers_enabled",
                    preferences.getBoolean("GoAgentPublicServersEnabled", true));
            configJson.put("shadowsocks_public_servers_enabled",
                    preferences.getBoolean("ShadowsocksPublicServersEnabled", true));
            configJson.put("http_proxy_public_servers_enabled",
                    preferences.getBoolean("HttpProxyPublicServersEnabled", true));
            IOUtils.writeToFile(FQROUTER_CONFIG_FILE, configJson.toString());
        } catch (Exception e) {
            LogUtils.e("failed to update config file", e);
        }
    }

    public static void execute(Context context) {
        context.startService(new Intent(context, LaunchService.class));
    }
}
