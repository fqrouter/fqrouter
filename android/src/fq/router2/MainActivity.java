package fq.router2;

import android.app.*;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.VpnService;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.view.*;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.google.analytics.tracking.android.EasyTracker;
import fq.router2.feedback.*;
import fq.router2.life_cycle.*;
import fq.router2.utils.*;

import java.io.File;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;


public class MainActivity extends Activity implements
        LaunchedIntent.Handler,
        UpdateFoundIntent.Handler,
        ExitedIntent.Handler,
        DownloadingIntent.Handler,
        DownloadedIntent.Handler,
        DownloadFailedIntent.Handler,
        HandleFatalErrorIntent.Handler,
        DnsPollutedIntent.Handler,
        HandleAlertIntent.Handler,
        ExitingIntent.Handler,
        LaunchingIntent.Handler, SocksVpnConnectedIntent.Handler {

    public final static int SHOW_AS_ACTION_IF_ROOM = 1;
    private final static int ITEM_ID_EXIT = 1;
    private final static int ITEM_ID_REPORT_ERROR = 2;
    private final static int ITEM_ID_SETTINGS = 3;
    private final static int ITEM_ID_UPGRADE_MANUALLY = 4;
    private final static int ITEM_ID_CLEAN_DNS = 5;
    private final static int ITEM_ID_ABOUT = 6;
    private final static int ASK_VPN_PERMISSION = 1;
    private static boolean isReady;
    private Handler handler = new Handler();
    private String upgradeUrl;
    private boolean downloaded;
    private static boolean dnsPollutionAcked = false;
    private static final Set<String> WAP_APN_LIST = new HashSet<String>(){{
        add("cmwap");
        add("uniwap");
        add("3gwap");
        add("ctwap");
    }};

    static {
        IOUtils.createCommonDirs();
    }

    private GestureDetectorCompat gestureDetector;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, true);
        setTitle("fqrouter " + LaunchService.getMyVersion(this));
        LaunchedIntent.register(this);
        LaunchingIntent.register(this);
        UpdateFoundIntent.register(this);
        ExitedIntent.register(this);
        DownloadingIntent.register(this);
        DownloadedIntent.register(this);
        DownloadFailedIntent.register(this);
        HandleFatalErrorIntent.register(this);
        DnsPollutedIntent.register(this);
        HandleAlertIntent.register(this);
        ExitingIntent.register(this);
        SocksVpnConnectedIntent.register(this);
        gestureDetector = new GestureDetectorCompat(this, new MyGestureDetector());
        Button fullPowerButton = (Button) findViewById(R.id.fullPowerButton);
        fullPowerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showWebView();
            }
        });
        updateStatus(_(R.string.status_check_apn), 5);
        String apnName = getApnName();
        LogUtils.i("apn name: " + apnName);
        final File ignoredFile = new File("/data/data/fq.router2/etc/apn-alert-ignored");
        if (apnName != null && WAP_APN_LIST.contains(apnName.trim().toLowerCase()) && !ignoredFile.exists()) {
            new AlertDialog.Builder(MainActivity.this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(R.string.wap_apn_alert_title)
                    .setMessage(String.format(_(R.string.wap_apn_alert_message), apnName))
                    .setPositiveButton(R.string.wap_apn_alert_change_now, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Intent intent = new Intent(Settings.ACTION_APN_SETTINGS);
                            startActivity(intent);
                            clearNotification(MainActivity.this);
                            MainActivity.this.finish();
                        }
                    })
                    .setNegativeButton(R.string.wap_apn_alert_ignore, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            IOUtils.writeToFile(ignoredFile, "OK");
                            LaunchService.execute(MainActivity.this);
                        }
                    })
                    .show();
        } else {
            if (isReady) {
                loadWebView();
                showWebView();
            } else {
                LaunchService.execute(this);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        this.gestureDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    private String getApnName() {
        try {
            ConnectivityManager conManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo ni = conManager.getActiveNetworkInfo();
            if (null == ni) {
                return null;
            }
            return ni.getExtraInfo();
        } catch (Exception e) {
            LogUtils.e("failed to get apn name", e);
            return null;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        EasyTracker.getInstance().activityStart(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EasyTracker.getInstance().activityStop(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isReady) {
            WebView webView = (WebView) findViewById(R.id.webView);
            webView.loadUrl("javascript:onPause()");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isReady) {
            CheckDnsPollutionService.execute(this);
            WebView webView = (WebView) findViewById(R.id.webView);
            webView.loadUrl("javascript:onResume()");
        }
    }

    private void loadWebView() {
        WebView webView = (WebView) findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl("http://127.0.0.1:2515");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            if (ASK_VPN_PERMISSION == requestCode) {
                if (resultCode == RESULT_OK) {
                    if (LaunchService.SOCKS_VPN_SERVICE_CLASS == null) {
                        onHandleFatalError("vpn class not loaded");
                    } else {
                        updateStatus(_(R.string.status_launch_vpn), 80);
                        stopService(new Intent(this, LaunchService.SOCKS_VPN_SERVICE_CLASS));
                        startService(new Intent(this, LaunchService.SOCKS_VPN_SERVICE_CLASS));
                    }
                } else {
                    onHandleFatalError(_(R.string.status_vpn_rejected));
                    LogUtils.e("failed to start vpn service: " + resultCode);
                }
            } else {
                super.onActivityResult(requestCode, resultCode, data);
            }
        } catch (Exception e) {
            LogUtils.e("failed to handle onActivityResult", e);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (isReady) {
            menu.add(Menu.NONE, ITEM_ID_SETTINGS, Menu.NONE, R.string.menu_settings);
            menu.add(Menu.NONE, ITEM_ID_CLEAN_DNS, Menu.NONE, R.string.menu_clean_dns);
        }
        if (upgradeUrl != null) {
            menu.add(Menu.NONE, ITEM_ID_UPGRADE_MANUALLY, Menu.NONE, R.string.menu_upgrade_manually);
        }
        menu.add(Menu.NONE, ITEM_ID_REPORT_ERROR, Menu.NONE, R.string.menu_report_error);
        menu.add(Menu.NONE, ITEM_ID_ABOUT, Menu.NONE, R.string.menu_about);
        addMenuItem(menu, ITEM_ID_EXIT, _(R.string.menu_exit));
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
        } else if (ITEM_ID_CLEAN_DNS == item.getItemId()) {
            showDnsPollutedAlert();
        } else if (ITEM_ID_ABOUT == item.getItemId()) {
            openAbout();
        }
        return super.onMenuItemSelected(featureId, item);
    }

    private void openAbout() {
        WebView web = new WebView(this);
        web.loadUrl("file:///android_asset/pages/about.html");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                return true;
            }
        });
        new AlertDialog.Builder(this)
                .setTitle(String.format(_(R.string.about_info_title), LaunchService.getMyVersion(this)))
                .setCancelable(false)
                .setNegativeButton(R.string.about_info_close, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                })
                .setView(web)
                .create()
                .show();
    }

    public static void displayNotification(Context context, String text) {
        if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("NotificationEnabled", true)) {
            clearNotification(context);
            return;
        }
        if (LaunchService.isVpnRunning()) {
            clearNotification(context);
            return;
        }
        try {
            Intent openIntent = new Intent(context, MainActivity.class);
            Notification notification = new NotificationCompat.Builder(context)
                    .setSmallIcon(R.drawable.icon)
                    .setContentTitle(context.getResources().getString(R.string.notification_title))
                    .setContentText(text)
                    .setContentIntent(PendingIntent.getActivity(context, 0, openIntent, 0))
                    .addAction(
                            android.R.drawable.ic_menu_close_clear_cancel,
                            context.getResources().getString(R.string.menu_exit),
                            PendingIntent.getService(context, 0, new Intent(context, ExitService.class), 0))
                    .addAction(
                            android.R.drawable.ic_menu_manage,
                            context.getResources().getString(R.string.menu_status),
                            PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), 0))
                    .build();
            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
            notification.flags |= Notification.FLAG_ONGOING_EVENT;
            notificationManager.notify(1983, notification);
        } catch (Exception e) {
            LogUtils.e("failed to display notification " + text, e);
        }
    }

    public static void clearNotification(Context context) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(1983);
    }

    public void updateStatus(String status, int progress) {
        LogUtils.i(status);
        TextView textView = (TextView) findViewById(R.id.statusTextView);
        textView.setText(status);
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setProgress(progress);
    }

    public void exit() {
        if (LaunchService.isVpnRunning()) {
            Toast.makeText(this, R.string.vpn_exit_hint, 5000).show();
            return;
        }
        ExitService.execute(this);
        displayNotification(this, _(R.string.status_exiting));
    }

    private String _(int id) {
        return getResources().getString(id);
    }

    @Override
    public void onLaunched(boolean isVpnMode) {
        ActivityCompat.invalidateOptionsMenu(this);
        if (isVpnMode) {
            updateStatus(_(R.string.status_acquire_vpn_permission), 75);
            clearNotification(this);
            if (LaunchService.isVpnRunning()) {
                onReady();
            } else {
                startVpn();
            }
        } else {
            onReady();
        }
    }

    public void onReady() {
        isReady = true;
        ActivityCompat.invalidateOptionsMenu(this);
        updateStatus(_(R.string.status_ready), 100);
        displayNotification(this, _(R.string.status_ready));
        findViewById(R.id.progressBar).setVisibility(View.GONE);
        findViewById(R.id.hintTextView).setVisibility(View.VISIBLE);
        findViewById(R.id.fullPowerButton).setVisibility(View.VISIBLE);
        loadWebView();
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
                .setTitle(R.string.new_version_alert_title)
                .setMessage(R.string.new_version_alert_message)
                .setPositiveButton(R.string.new_version_alert_yes, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        updateStatus(_(R.string.status_downloading) + " " + Uri.parse(upgradeUrl).getLastPathSegment(), 5);
                        DownloadService.execute(
                                MainActivity.this, upgradeUrl, downloadTo);
                    }

                })
                .setNegativeButton(R.string.new_version_alert_no, null)
                .show();
    }

    @Override
    public void onExited() {
        clearNotification(this);
        finish();
    }

    @Override
    public void onDownloadFailed(final String url, String downloadTo) {
        ActivityCompat.invalidateOptionsMenu(this);
        onHandleFatalError(_(R.string.status_download_failed) + " " + Uri.parse(url).getLastPathSegment());
        Toast.makeText(this, R.string.upgrade_via_browser_hint, 3000).show();
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
        updateStatus(_(R.string.status_downloaded) + " " + Uri.parse(url).getLastPathSegment(), 5);
        onExiting();
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
            displayNotification(this, _(R.string.status_downloading) + " " + Uri.parse(url).getLastPathSegment() + ": " + percent + "%");
        }
        TextView textView = (TextView) findViewById(R.id.statusTextView);
        textView.setText(_(R.string.status_downloaded) + " " + percent + "%");
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

    @Override
    public void onHandleFatalError(String message) {
        findViewById(R.id.progressBar).setVisibility(View.GONE);
        TextView statusTextView = (TextView) findViewById(R.id.statusTextView);
        statusTextView.setTextColor(Color.RED);
        statusTextView.setText(message);
        checkUpdate();
    }

    @Override
    public void onDnsPolluted(final long pollutedAt) {
        if (!dnsPollutionAcked) {
            showDnsPollutedAlert();
        }
    }

    private void showDnsPollutedAlert() {
        new AlertDialog.Builder(MainActivity.this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.dns_polluted_alert_title)
                .setMessage(R.string.dns_polluted_alert_message)
                .setPositiveButton(R.string.dns_polluted_alert_fix, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dnsPollutionAcked = true;
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    AirplaneModeUtils.toggle(MainActivity.this);
                                    showToast(R.string.dns_polluted_alert_toggle_succeed);
                                } catch (Exception e) {
                                    LogUtils.e("failed to toggle airplane mode", e);
                                    showToast(R.string.dns_polluted_alert_toggle_failed);
                                }
                            }
                        }).start();
                    }
                })
                .setNegativeButton(R.string.dns_polluted_alert_ack, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dnsPollutionAcked = true;
                    }
                })
                .show();
    }

    private void showToast(final int message) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, message, 5000).show();
            }
        }, 0);
    }

    @Override
    public void onExiting() {
        displayNotification(this, _(R.string.status_exiting));
        isReady = false;
        ActivityCompat.invalidateOptionsMenu(this);
        findViewById(R.id.webView).setVisibility(View.GONE);
        findViewById(R.id.progressBar).setVisibility(View.GONE);
        findViewById(R.id.hintTextView).setVisibility(View.GONE);
        findViewById(R.id.fullPowerButton).setVisibility(View.GONE);
        findViewById(R.id.statusTextView).setVisibility(View.VISIBLE);
        TextView statusTextView = (TextView) findViewById(R.id.statusTextView);
        statusTextView.setText(_(R.string.status_exiting));
    }

    private class MyGestureDetector extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_MIN_DISTANCE = 120;
        private static final int SWIPE_MAX_OFF_PATH = 250;
        private static final int SWIPE_THRESHOLD_VELOCITY = 200;
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            try {
                if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
                    return false;
                // right to left swipe
                if(e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    showWebView();
                }
            } catch (Exception e) {
                LogUtils.e("failed to swipe", e);
            }
            return false;
        }
    }

    private void showWebView() {
        if (isReady) {
            final TextView statusTextView = (TextView) findViewById(R.id.statusTextView);
            statusTextView.setText(R.string.status_loading_page);
            statusTextView.setVisibility(View.VISIBLE);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    statusTextView.setVisibility(View.GONE);
                }
            }, 3000);
            findViewById(R.id.progressBar).setVisibility(View.GONE);
            findViewById(R.id.hintTextView).setVisibility(View.GONE);
            findViewById(R.id.fullPowerButton).setVisibility(View.GONE);
            findViewById(R.id.webView).setVisibility(View.VISIBLE);
        }
    }
}
