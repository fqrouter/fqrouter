package fq.router2;

import android.app.*;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.net.Uri;
import android.net.VpnService;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import fq.router2.feedback.*;
import fq.router2.free_internet.CheckFreeInternetService;
import fq.router2.free_internet.ConnectFreeInternetService;
import fq.router2.free_internet.DisconnectFreeInternetService;
import fq.router2.free_internet.FreeInternetChangedIntent;
import fq.router2.life_cycle.*;
import fq.router2.pick_and_play.CheckPickAndPlayService;
import fq.router2.pick_and_play.PickAndPlayChangedIntent;
import fq.router2.utils.ApkUtils;
import fq.router2.utils.IOUtils;
import fq.router2.utils.LogUtils;
import fq.router2.utils.ShellUtils;
import fq.router2.wifi_repeater.CheckWifiRepeaterService;
import fq.router2.wifi_repeater.StartWifiRepeaterService;
import fq.router2.wifi_repeater.StopWifiRepeaterService;
import fq.router2.wifi_repeater.WifiRepeaterChangedIntent;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;


public class MainActivity extends Activity implements
        LaunchedIntent.Handler,
        UpdateFoundIntent.Handler,
        ExitedIntent.Handler,
        WifiRepeaterChangedIntent.Handler,
        PickAndPlayChangedIntent.Handler,
        FreeInternetChangedIntent.Handler,
        DownloadingIntent.Handler,
        DownloadedIntent.Handler,
        DownloadFailedIntent.Handler,
        HandleFatalErrorIntent.Handler {

    public final static int SHOW_AS_ACTION_IF_ROOM = 1;
    private final static int ITEM_ID_EXIT = 1;
    private final static int ITEM_ID_REPORT_ERROR = 2;
    private final static int ITEM_ID_SETTINGS = 3;
    private final static int ITEM_ID_UPGRADE_MANUALLY = 4;
    private final static int ASK_VPN_PERMISSION = 1;
    private static boolean isLaunched;
    private Handler handler = new Handler();
    private Set<Integer> blinkingImageViews = new HashSet<Integer>();
    private String blinkingStatus = "";
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
        LaunchedIntent.register(this);
        UpdateFoundIntent.register(this);
        ExitedIntent.register(this);
        WifiRepeaterChangedIntent.register(this);
        PickAndPlayChangedIntent.register(this);
        FreeInternetChangedIntent.register(this);
        DownloadingIntent.register(this);
        DownloadedIntent.register(this);
        DownloadFailedIntent.register(this);
        HandleFatalErrorIntent.register(this);
        blinkStatus(0);
        launch();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isLaunched) {
            checkAll();
        }
    }

    private void checkAll() {
        checkWifiRepeater();
        checkPickAndPlay();
        checkFreeInternet();
    }

    private void checkWifiRepeater() {
        if (ShellUtils.isRooted() && Build.VERSION.SDK_INT >= 14) {
            CheckWifiRepeaterService.execute(this);
        }
    }

    private void checkPickAndPlay() {
        if (ShellUtils.isRooted()) {
            CheckPickAndPlayService.execute(this);
        }
    }

    private void checkFreeInternet() {
        if (ShellUtils.isRooted()) {
            CheckFreeInternetService.execute(this);
        } else {
            if (LaunchService.isVpnRunning()) {
                onFreeInternetChanged(true);
            }
        }
    }

    private void launch() {
        disableAll();
        startBlinkingImage((ImageView) findViewById(R.id.star));
        startBlinkingStatus("Launching");
        LaunchService.execute(this);
    }

    private void disableAll() {
        disableImage((ImageView) findViewById(R.id.freeInternetArrow));
        disableImage((ImageView) findViewById(R.id.wifiRepeaterArrow));
        disableImage((ImageView) findViewById(R.id.pickAndPlayArrow));
        disableFreeInternetButton();
        disableWifiRepeaterButton();
        disablePickAndPlayButton();
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
                    uninstallOldVersion();
                }
            } else {
                onHandleFatalError("vpn permission rejected");
                LogUtils.e("failed to start vpn service: " + resultCode);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void setupUI() {
        findViewById(R.id.wifiRepeaterButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleWifiRepeater((ToggleButton) view);
            }
        });
        findViewById(R.id.freeInternetButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleFreeInternet((ToggleButton) view);
            }
        });
        findViewById(R.id.pickAndPlayButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, PickAndPlayActivity.class));
            }
        });
    }


    private void startBlinkingStatus(String status) {
        blinkingStatus = status;
    }

    private void stopBlinkingStatus() {
        blinkingStatus = "";
    }

    private void blinkStatus(final int count) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                TextView statusTextView = (TextView) findViewById(R.id.statusTextView);
                if (!blinkingStatus.isEmpty()) {
                    String text = blinkingStatus;
                    for (int i = 0; i < count; i++) {
                        text += ".";
                    }
                    statusTextView.setText(text);
                }
                blinkStatus((count + 1) % 4);
            }
        }, 500);
    }

    private void startBlinkingImage(ImageView imageView) {
        blinkingImageViews.add(imageView.getId());
        blinkImage(imageView, true);
    }

    private void stopBlinkingImage(ImageView imageView) {
        blinkingImageViews.remove(imageView.getId());
    }

    private void blinkImage(final ImageView imageView, final boolean setsGrayScale) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (blinkingImageViews.contains(imageView.getId())) {
                    if (setsGrayScale) {
                        disableImage(imageView);
                    } else {
                        enableImage(imageView);
                    }
                    blinkImage(imageView, !setsGrayScale);
                }
            }
        }, 1000);
    }

    private void enableImage(ImageView imageView) {
        imageView.clearColorFilter();
    }

    private void disableImage(ImageView imageView) {
        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0); //0 means grayscale
        ColorMatrixColorFilter cf = new ColorMatrixColorFilter(matrix);
        imageView.setColorFilter(cf);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (isLaunched) {
            menu.add(Menu.NONE, ITEM_ID_SETTINGS, Menu.NONE, "Settings");
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
        } else if (ITEM_ID_UPGRADE_MANUALLY == item.getItemId()) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(upgradeUrl)));
        }
        return super.onMenuItemSelected(featureId, item);
    }

    private void showNotification(String text) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, 0);
        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.small_star)
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

    public void updateStatus(String status) {
        LogUtils.i(status);
        TextView textView = (TextView) findViewById(R.id.statusTextView);
        if (!textView.getText().toString().startsWith("Error:")) {
            textView.setText(status);
            if (LaunchService.isVpnRunning()) {
                clearNotification();
            } else {
                showNotification(status);
            }
        }
    }

    private void clearStatus() {
        TextView textView = (TextView) findViewById(R.id.statusTextView);
        if (!textView.getText().toString().startsWith("Error:")) {
            textView.setText("");
        }
    }

    private void exit() {
        if (LaunchService.isVpnRunning()) {
            Toast.makeText(this, "Use notification bar to stop VPN", 5000).show();
            return;
        }
        isLaunched = false;
        disableAll();
        ExitService.execute(this);
        startBlinkingImage((ImageView) findViewById(R.id.star));
        startBlinkingStatus("Exiting");
    }

    private void toggleFreeInternet(ToggleButton button) {
        startBlinkingImage((ImageView) findViewById(R.id.freeInternetArrow));
        if (button.isChecked()) {
            startBlinkingStatus("Connecting to free internet");
            disableFreeInternetButton();
            ConnectFreeInternetService.execute(this);
        } else {
            startBlinkingStatus("Disconnecting from free internet");
            disableFreeInternetButton();
            DisconnectFreeInternetService.execute(this);
        }
    }

    private void toggleWifiRepeater(ToggleButton button) {
        startBlinkingImage((ImageView) findViewById(R.id.wifiRepeaterArrow));
        if (button.isChecked()) {
            startBlinkingStatus("Starting wifi repeater");
            disableWifiRepeaterButton();
            StartWifiRepeaterService.execute(this);
        } else {
            startBlinkingStatus("Stopping wifi repeater");
            disableWifiRepeaterButton();
            StopWifiRepeaterService.execute(this);
        }
    }

    public void disableFreeInternetButton() {
        findViewById(R.id.freeInternetButton).setEnabled(false);
    }


    public void enableFreeInternetButton(final boolean isConnected) {
        ToggleButton button = (ToggleButton) findViewById(R.id.freeInternetButton);
        button.setChecked(isConnected);
        button.setEnabled(true);
    }

    public void disableWifiRepeaterButton() {
        findViewById(R.id.wifiRepeaterButton).setEnabled(false);
    }


    public void enableWifiRepeaterButton(final boolean isStarted) {
        ToggleButton button = (ToggleButton) findViewById(R.id.wifiRepeaterButton);
        button.setChecked(isStarted);
        button.setEnabled(true);
    }

    public void disablePickAndPlayButton() {
        findViewById(R.id.pickAndPlayButton).setEnabled(false);
    }


    @Override
    public void onLaunched(boolean isVpnMode) {
        isLaunched = true;
        ActivityCompat.invalidateOptionsMenu(this);

        ImageView star = (ImageView) findViewById(R.id.star);
        stopBlinkingImage(star);
        enableImage(star);
        stopBlinkingStatus();

        startBlinkingImage((ImageView) findViewById(R.id.freeInternetArrow));
        startBlinkingStatus("Connecting to free internet");
        if (isVpnMode) {
            if (LaunchService.isVpnRunning()) {
                onFreeInternetChanged(true);
            } else {
                startVpn();
            }
        } else {
            ApkUtils.uninstall(MainActivity.this, "fq.router");
            checkWifiRepeater();
            checkPickAndPlay();
            ConnectFreeInternetService.execute(this);
        }
    }

    private void uninstallOldVersion() {
        boolean isOldVersionInstalled = ApkUtils.isInstalled(this, "fq.router");
        LogUtils.i("old version is installed: " + isOldVersionInstalled);
        if (isOldVersionInstalled) {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle("fqrouter has been upgraded to 2.x.x version")
                    .setMessage("The old 1.x.x version will be uninstalled")
                    .setPositiveButton("Uninstall", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ApkUtils.uninstall(MainActivity.this, "fq.router");
                        }

                    })
                    .show();
        }
    }

    private void checkUpdate() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean AutoUpdateEnabled = preferences.getBoolean("AutoUpdateEnabled", true);
        if (AutoUpdateEnabled && upgradeUrl == null) {
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
        handler.postDelayed(new Runnable() {
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
        ApkUtils.install(this, downloadTo);
    }

    @Override
    public void onDownloading(String url, String downloadTo, int percent) {
        if (System.currentTimeMillis() % (2 * 1000) == 0) {
            showNotification("Downloading " + Uri.parse(url).getLastPathSegment() + ": " + percent + "%");
        }
        TextView textView = (TextView) findViewById(R.id.statusTextView);
        textView.setText("Downloaded: " + percent + "%");
    }

    private void startVpn() {
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
        isLaunched = false;
    }

    @Override
    public void onHandleFatalError(String message) {
        blinkingImageViews.clear();
        stopBlinkingStatus();
        disableAll();
        updateStatus("Error: " + message);
        checkAll();
        checkUpdate();
    }

    @Override
    public void onWifiRepeaterChanged(boolean isStarted) {
        updateWifiLock(isStarted);
        stopBlinkingImage((ImageView) findViewById(R.id.wifiRepeaterArrow));
        stopBlinkingStatus();
        clearStatus();
        if (isStarted) {
            enableImage((ImageView) findViewById(R.id.wifiRepeaterArrow));
        } else {
            disableImage((ImageView) findViewById(R.id.wifiRepeaterArrow));
        }
        enableWifiRepeaterButton(isStarted);
    }

    @Override
    public void onFreeInternetChanged(boolean isConnected) {
        ImageView freeInternetArrow = (ImageView) findViewById(R.id.freeInternetArrow);
        stopBlinkingImage(freeInternetArrow);
        stopBlinkingStatus();
        if (isConnected) {
            updateStatus("You may try youtube/twitter now");
            enableImage(freeInternetArrow);
            checkUpdate();
        } else {
            clearStatus();
            disableImage(freeInternetArrow);
        }
        if (LaunchService.isVpnRunning()) {
            ToggleButton button = (ToggleButton) findViewById(R.id.freeInternetButton);
            button.setChecked(isConnected);
        } else {
            enableFreeInternetButton(isConnected);
        }
    }

    @Override
    public void onPickAndPlayChanged(boolean isStarted) {
        ImageView pickAndPlayArrow = (ImageView) findViewById(R.id.pickAndPlayArrow);
        stopBlinkingImage(pickAndPlayArrow);
        stopBlinkingStatus();
        clearStatus();
        if (isStarted) {
            enableImage(pickAndPlayArrow);
        } else {
            disableImage(pickAndPlayArrow);
        }
        findViewById(R.id.pickAndPlayButton).setEnabled(true);
    }
}
