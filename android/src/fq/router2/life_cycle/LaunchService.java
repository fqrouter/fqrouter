package fq.router2.life_cycle;

import android.app.ActivityManager;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import fq.router2.R;
import fq.router2.feedback.HandleAlertIntent;
import fq.router2.feedback.HandleFatalErrorIntent;
import fq.router2.utils.*;

import java.io.File;
import java.io.OutputStreamWriter;
import java.util.*;
import fq.router2.SocksVpnService;

public class LaunchService extends IntentService {

    public static Class SOCKS_VPN_SERVICE_CLASS;
    private static final Set<String> WAP_APN_LIST = new HashSet<String>(){{
        add("cmwap");
        add("uniwap");
        add("3gwap");
        add("ctwap");
    }};

    static {
        try {
            SOCKS_VPN_SERVICE_CLASS = LaunchService.class.forName("fq.router2.SocksVpnService");
        } catch (ClassNotFoundException e) {
            LogUtils.e("failed to load SocksVpnService.class", e);
        }
    }

    public LaunchService() {
        super("Launch");
    }

    public static boolean isVpnRunning(Context context) {
        return SOCKS_VPN_SERVICE_CLASS != null && SocksVpnService.isRunning();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        LogUtils.i("ver: " + getMyVersion(this));
        sendBroadcast(new LaunchingIntent(_(R.string.status_check_root), 5));
        boolean rooted = ShellUtils.checkRooted();
        LogUtils.i("rooted: " + rooted);
        sendBroadcast(new LaunchingIntent(_(R.string.status_check_existing_process), 10));
        if (isVpnRunning(this)) {
            LogUtils.i("manager is already running in vpn mode");
            sendBroadcast(new LaunchedIntent(true));
            return;
        }
        String runningAs = isRunningAs();
        if ("ROOT".equals(runningAs)) {
            LogUtils.i("manager is already running in root mode");
            sendBroadcast(new LaunchedIntent(false));
            return;
        }
        if ("VPN".equals(runningAs)) {
            sendBroadcast(new LaunchingIntent(_(R.string.status_restart_vpn), 0));
            LogUtils.i("Restart manager");
            try {
                ManagerProcess.kill();
                Thread.sleep(1000);
                if (ManagerProcess.exists()) {
                    handleFatalError(_(R.string.status_failed_to_restart_vpn));
                    LogUtils.e("failed to restart manager", null);
                } else {
                    LaunchService.execute(this);
                }
            } catch (Exception e) {
                handleFatalError(LogUtils.e("failed to stop exiting process", e));
            }
            return;
        }
        if (rooted) {
            sendBroadcast(new LaunchingIntent(_(R.string.status_about_to_launch_in_root_mode), 20));
        } else {
            if (Build.VERSION.SDK_INT < 14) {
                handleFatalError(_(R.string.status_root_is_requried_below_4_0));
                return;
            }
            sendBroadcast(new LaunchingIntent(_(R.string.status_about_to_launch_in_vpn_mode), 20));
            File managerLogFile = new File("/data/data/fq.router2/log/fqsocks.log");
            if (managerLogFile.exists() && !managerLogFile.canWrite()) {
                handleFatalError(LogUtils.e(_(R.string.status_root_permission_lost)));
                return;
            }
        }
        try {
            if (StartedAtFlag.read() > 0) {
                StartedAtFlag.delete();
            }
            StartedAtFlag.create();
        } catch (Exception e) {
            LogUtils.e("failed to check started at flag", e);
        }
        deployAndLaunch();
    }

    private String isRunningAs() {
        if (!ManagerProcess.exists()) {
            return "";
        }
        try {
            String content = HttpUtils.get("http://127.0.0.1:" + ConfigUtils.getHttpManagerPort() + "/ping");
            String myVersion = getMyVersion(this);
            if (("VPN PONG/" + myVersion).equals(content)) {
                return "VPN";
            }
            if (("PONG/" + myVersion).equals(content)) {
                return "ROOT";
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    private String _(int id) {
        return getResources().getString(id);
    }

    private void deployAndLaunch() {
        sendBroadcast(new LaunchingIntent(_(R.string.status_clean_environment), 25));
        try {
            LogUtils.i("Kill existing manager process");
            LogUtils.i("try to kill manager process before launch");
            ManagerProcess.kill();
        } catch (Exception e) {
            LogUtils.e("failed to kill manager process before launch", e);
        }
        Deployer deployer = new Deployer(this);
        String fatalError = deployer.deploy();
        if (fatalError.length() > 0) {
            handleFatalError(fatalError);
            return;
        }
        sendBroadcast(new LaunchingIntent(_(R.string.status_starting_manager), 45));
        LogUtils.i("Launching...");
        if (ShellUtils.checkRooted()) {
            fatalError = launch(false);
            if (fatalError.length() == 0) {
                sendBroadcast(new LaunchedIntent(false));
            } else {
                handleFatalError(fatalError);
            }
        } else {
            fatalError = launch(true);
            if (fatalError.length() == 0) {
                sendBroadcast(new LaunchedIntent(true));
            } else {
                handleFatalError(fatalError);
            }
        }
    }

    private String launch(boolean isVpnMode) {
        try {
            Process process = executeManager(isVpnMode);
            String apnName = getApnName();
            LogUtils.i("apn name: " + apnName);
            if (apnName != null && WAP_APN_LIST.contains(apnName.trim().toLowerCase())) {
                sendBroadcast(new HandleAlertIntent(HandleAlertIntent.ALERT_TYPE_3G_APN));
                Thread.sleep(3000);
            }
            for (int i = 0; i < 30; i++) {
                if (ping(this, isVpnMode)) {
                    return "";
                }
                if (hasProcessExited(process)) {
                    return _(R.string.status_failed_to_launch);
                }
                sendBroadcast(new LaunchingIntent(_(R.string.status_starting_manager), 45 + i));
                sleepOneSecond();
            }
            if (apnName != null && WAP_APN_LIST.contains(apnName.trim().toLowerCase())) {
                return _(R.string.status_3g_apn_has_proxy);
            }
            return _(R.string.status_timed_out);
        } catch (Exception e) {
            return LogUtils.e("failed to launch", e);
        }
    }

    private String getApnName() {
        try {
            ConnectivityManager conManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo ni = conManager.getActiveNetworkInfo();
            if (null == ni) {
                return "";
            }
            return ni.getExtraInfo();
        } catch (Exception e) {
            LogUtils.e("failed to get apn name", e);
            return "";
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
            Process process = ShellUtils.executeNoWait(env, ShellUtils.BUSYBOX_FILE.getAbsolutePath(), "sh");
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
            String runMode = ManagerProcess.getRunMode();
            if ("run-needs-su".equals(runMode)) {
                Process process = ShellUtils.executeNoWait(env, ShellUtils.BUSYBOX_FILE.getAbsolutePath(), "sh");
                OutputStreamWriter stdin = new OutputStreamWriter(process.getOutputStream());
                try {
                    String command = Deployer.PYTHON_LAUNCHER + " " + Deployer.MANAGER_MAIN_PY.getAbsolutePath() +
                            " run > /data/data/fq.router2/log/current-python.log 2>&1";
                    LogUtils.i("write to stdin: " + command);
                    stdin.write(command);
                    stdin.write("\nexit\n");
                } finally {
                    stdin.close();
                }
                return process;
            } else {
                return ShellUtils.sudoNoWait(env, Deployer.PYTHON_LAUNCHER + " " +
                        Deployer.MANAGER_MAIN_PY.getAbsolutePath() +
                        " run > /data/data/fq.router2/log/current-python.log 2>&1");
            }
        }
    }

    public static String getMyVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            if (null == packageInfo.versionName) {
                return "Unknown";
            } else {
                return packageInfo.versionName;
            }
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

    public static boolean ping(Context context, boolean isVpnMode) {
        return ping(context, isVpnMode, 3000);
    }

    public static boolean ping(Context context, boolean isVpnMode, int timeout) {
        try {
            String myVersion = getMyVersion(context);
            String content = HttpUtils.get("http://127.0.0.1:" + ConfigUtils.getHttpManagerPort() + "/ping", null, timeout);
            if (isVpnMode ? ("VPN PONG/" + myVersion).equals(content) : ("PONG/" + myVersion).equals(content)) {
                LogUtils.e("ping " + isVpnMode + " succeeded");
                return true;
            } else {
                LogUtils.e("ping " + isVpnMode + " failed: " + content);
                return false;
            }
        } catch (HttpUtils.Error e) {
            LogUtils.e("ping " + isVpnMode + " failed: [" + e.responseCode + "] " + e.output);
            return false;
        } catch (Exception e) {
            LogUtils.e("ping " + isVpnMode + " failed: " + e, e);
            return false;
        }
    }

    private void handleFatalError(String message) {
        if (ShellUtils.isRooted() && "run-needs-su".equals(ManagerProcess.getRunMode())) {
            sendBroadcast(new HandleAlertIntent(HandleAlertIntent.ALERT_TYPE_RUN_NEEDS_SU));
        }
        sendBroadcast(new HandleFatalErrorIntent(message));
    }

    public static void execute(Context context) {
        context.startService(new Intent(context, LaunchService.class));
    }
}
