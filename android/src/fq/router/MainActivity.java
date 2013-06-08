package fq.router;

import android.app.*;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.net.VpnService;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import fq.router.feedback.*;
import fq.router.life.*;
import fq.router.utils.ApkUtils;
import fq.router.utils.IOUtils;
import fq.router.utils.LogUtils;
import fq.router.utils.ShellUtils;
import fq.router.wifi.CheckWifiHotspotService;
import fq.router.wifi.StartWifiHotspotService;
import fq.router.wifi.StopWifiHotspotService;
import fq.router.wifi.WifiHotspotChangedIntent;

import java.lang.reflect.Method;


public class MainActivity extends Activity implements
        UpdateStatusIntent.Handler,
        AppendLogIntent.Handler,
        LaunchedIntent.Handler,
        UpdateFoundIntent.Handler,
        ExitedIntent.Handler,
        WifiHotspotChangedIntent.Handler,
        DownloadingIntent.Handler,
        DownloadedIntent.Handler,
        DownloadFailedIntent.Handler,
        StartVpnIntent.Handler,
        HandleFatalErrorIntent.Handler {

    public final static int SHOW_AS_ACTION_IF_ROOM = 1;
    private final static int ITEM_ID_EXIT = 1;
    private final static int ITEM_ID_REPORT_ERROR = 2;
    private final static int ITEM_ID_SETTINGS = 3;
    private final static int ITEM_ID_PICK_AND_PLAY = 4;
    private final static int ITEM_ID_UPGRADE_MANUALLY = 5;
    private final static int ASK_VPN_PERMISSION = 1;
    private static boolean started;
    private String upgradeUrl;
    private boolean downloaded;
    private WifiManager.WifiLock wifiLock;

    static {
        IOUtils.createCommonDirs();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, true);
        setupUI();
        UpdateStatusIntent.register(this);
        AppendLogIntent.register(this);
        LaunchedIntent.register(this);
        UpdateFoundIntent.register(this);
        ExitedIntent.register(this);
        WifiHotspotChangedIntent.register(this);
        DownloadingIntent.register(this);
        DownloadedIntent.register(this);
        DownloadFailedIntent.register(this);
        StartVpnIntent.register(this);
        HandleFatalErrorIntent.register(this);
        LaunchService.execute(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (ASK_VPN_PERMISSION == requestCode) {
            if (resultCode == RESULT_OK) {
                if (LaunchService.SOCKS_VPN_SERVICE_CLASS == null) {
                    onHandleFatalError("vpn class not loaded");
                } else {
                    updateStatus("Launch Socks Vpn Service");
                    stopService(new Intent(this, LaunchService.SOCKS_VPN_SERVICE_CLASS));
                    startService(new Intent(this, LaunchService.SOCKS_VPN_SERVICE_CLASS));
                }
            } else {
                onHandleFatalError("vpn permission rejected");
                LogUtils.e("failed to start vpn service: " + resultCode);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!started) {
            return;
        }
        LaunchService.execute(this);
    }

    private void setupUI() {
        TextView textView = (TextView) findViewById(R.id.logTextView);
        textView.setMovementMethod(new ScrollingMovementMethod());
        final ToggleButton button = (ToggleButton) findViewById(R.id.wifiHotspotToggleButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleWifiHotspot((ToggleButton) view);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (started) {
            menu.add(Menu.NONE, ITEM_ID_SETTINGS, Menu.NONE, "Settings");
        }
        if (ShellUtils.isRooted()) {
            addMenuItem(menu, ITEM_ID_PICK_AND_PLAY, "Pick & Play");
        }
        if (upgradeUrl != null) {
            menu.add(Menu.NONE, ITEM_ID_UPGRADE_MANUALLY, Menu.NONE, "Upgrade Manually");
        }
        menu.add(Menu.NONE, ITEM_ID_REPORT_ERROR, Menu.NONE, "Report Error");
        addMenuItem(menu, ITEM_ID_EXIT, "Exit");
        return super.onCreateOptionsMenu(menu);
    }

    private MenuItem addMenuItem(Menu menu, int menuItemId, String caption) {
        MenuItem menuItem = menu.add(Menu.NONE, menuItemId, Menu.NONE, caption);
        try {
            Method method = MenuItem.class.getMethod("setShowAsAction", int.class);
            try {
                method.invoke(menuItem, SHOW_AS_ACTION_IF_ROOM);
            } catch (Exception e) {
            }
        } catch (NoSuchMethodException e) {
        }
        return menuItem;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (ITEM_ID_EXIT == item.getItemId()) {
            exit();
        } else if (ITEM_ID_REPORT_ERROR == item.getItemId()) {
            new ErrorReportEmail(this).send();
        } else if (ITEM_ID_SETTINGS == item.getItemId()) {
            startActivity(new Intent(this, MainSettingsActivity.class));
        } else if (ITEM_ID_PICK_AND_PLAY == item.getItemId()) {
            startActivity(new Intent(this, PickAndPlayActivity.class));
        } else if (ITEM_ID_UPGRADE_MANUALLY == item.getItemId()) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(upgradeUrl)));
        }
        return super.onMenuItemSelected(featureId, item);
    }


    public void showWifiHotspotToggleButton(final boolean isStarted) {
        final ToggleButton wifiHotspotToggleButton = (ToggleButton) findViewById(R.id.wifiHotspotToggleButton);
        wifiHotspotToggleButton.setChecked(isStarted);
        final View wifiHotspotPanel = findViewById(R.id.wifiHotspotPanel);
        wifiHotspotPanel.setVisibility(View.VISIBLE);
    }

    private void showNotification(String text) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, 0);
        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.wall_green)
                .setContentTitle("fqrouter is running")
                .setContentText(text)
                .setContentIntent(pIntent)
                .build();
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notification.flags |= Notification.FLAG_ONGOING_EVENT;
        notificationManager.notify(0, notification);
    }

    private void clearNotification() {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(0);
    }

    @Override
    public void updateStatus(String status) {
        appendLog("status updated to: " + status);
        TextView textView = (TextView) findViewById(R.id.statusTextView);
        textView.setText(status);
        if (LaunchService.isVpnRunning()) {
            clearNotification();
        } else {
            showNotification(status);
        }
    }

    @Override
    public void appendLog(final String log) {
        LogUtils.i(log);
        TextView textView = (TextView) findViewById(R.id.logTextView);
        textView.setText(log + "\n" + textView.getText());
    }

    private void exit() {
        if (LaunchService.isVpnRunning()) {
            Toast.makeText(this, "Use notification bar to stop VPN", 5000).show();
            return;
        }
        started = false;
        hideWifiHotspotToggleButton();
        ExitService.execute(this);
    }

    private void toggleWifiHotspot(ToggleButton button) {
        if (button.isChecked()) {
            startWifiHotspot();
        } else {
            hideWifiHotspotToggleButton();
            StopWifiHotspotService.execute(this);
        }
    }

    private void startWifiHotspot() {
        hideWifiHotspotToggleButton();
        StartWifiHotspotService.execute(this);
    }

    public void hideWifiHotspotToggleButton() {
        findViewById(R.id.wifiHotspotPanel).setVisibility(View.INVISIBLE);
    }

    @Override
    public void onLaunched(boolean isVpnMode) {
        started = true;
        checkUpdate();
        if (!isVpnMode) {
            CheckWifiHotspotService.execute(this);
        }
        ActivityCompat.invalidateOptionsMenu(this);
    }

    private void checkUpdate() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isAutoUpdateEnabled = preferences.getBoolean("IsAutoUpdateEnabled", true);
        if (isAutoUpdateEnabled && upgradeUrl == null) {
            CheckUpdateService.execute(this);
        }
    }

    @Override
    public void onUpdateFound(String latestVersion, final String upgradeUrl) {
        final String downloadTo = "/sdcard/fqrouter-latest.apk";
        if (downloaded) {
            onDownloaded(upgradeUrl, downloadTo);
            return;
        }
        this.upgradeUrl = upgradeUrl;
        ActivityCompat.invalidateOptionsMenu(this);
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("There are newer version")
                .setMessage("Do you want to upgrade now?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        updateStatus("Downloading " + Uri.parse(upgradeUrl).getLastPathSegment());
                        DownloadService.execute(
                                MainActivity.this, upgradeUrl, downloadTo);
                    }

                })
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    public void onExited() {
        clearNotification();
        finish();
    }

    @Override
    public void onWifiHotspotChanged(boolean isStarted) {
        updateWifiLock(isStarted);
        showWifiHotspotToggleButton(isStarted);
    }

    private void updateWifiLock(boolean isStarted) {
        WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        if (null == wifiLock) {
            wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "fqrouter wifi hotspot");
        }
        if (isStarted) {
            wifiLock.acquire();
        } else {
            if (wifiLock.isHeld()) {
                wifiLock.release();
            }
        }
    }

    @Override
    public void onDownloadFailed(final String url, String downloadTo) {
        ActivityCompat.invalidateOptionsMenu(this);
        onHandleFatalError("download " + Uri.parse(url).getLastPathSegment() + " failed");
        Toast.makeText(this, "Open browser and download the update manually", 3000).show();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            }
        }, 3000);
    }

    @Override
    public void onDownloaded(String url, String downloadTo) {
        downloaded = true;
        ActivityCompat.invalidateOptionsMenu(this);
        updateStatus("Downloaded " + Uri.parse(url).getLastPathSegment());
        setExiting();
        try {
            ManagerProcess.kill();
        } catch (Exception e) {
            LogUtils.e("failed to kill manager", e);
        }
        ApkUtils.installApk(this, downloadTo);
    }

    @Override
    public void onDownloading(String url, String downloadTo, int percent) {
        if (System.currentTimeMillis() % (2 * 1000) == 0) {
            showNotification("Downloading " + Uri.parse(url).getLastPathSegment() + ": " + percent + "%");
        }
        TextView textView = (TextView) findViewById(R.id.statusTextView);
        textView.setText("Downloaded: " + percent + "%");
    }

    @Override
    public void onStartVpn() {
        if (LaunchService.isVpnRunning()) {
            LogUtils.e("vpn is already running, do not start it again");
            return;
        }
        Intent intent = VpnService.prepare(MainActivity.this);
        if (intent == null) {
            onActivityResult(ASK_VPN_PERMISSION, RESULT_OK, null);
        } else {
            startActivityForResult(intent, ASK_VPN_PERMISSION);
        }
    }

    public static void setExiting() {
        started = false;
    }

    @Override
    public void onHandleFatalError(String message) {
        updateStatus("Error: " + message);
        checkUpdate();
    }
}
