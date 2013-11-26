package fq.router2;

import android.app.*;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.view.*;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.analytics.tracking.android.Tracker;
import fq.router2.feedback.*;
import fq.router2.life_cycle.*;
import fq.router2.utils.*;

import java.io.File;
import java.lang.reflect.Method;


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
    private final static int ITEM_ID_OPEN_FULL_GOOGLE_PLAY = 7;
    private final static int ASK_VPN_PERMISSION = 1;
    public static boolean isReady;
    private Handler handler = new Handler();
    private String upgradeUrl;
    private boolean downloaded;
    private static boolean dnsPollutionAcked = false;

    static {
        IOUtils.createCommonDirs();
    }

    private GestureDetectorCompat gestureDetector;
    private String shareUrl;
    private Tracker gaTracker;


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
        GoogleAnalytics gaInstance = GoogleAnalytics.getInstance(MainActivity.this);
        gaTracker = gaInstance.getTracker("UA-37740383-2");
        CookieSyncManager.createInstance(this);
        fullPowerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gaTracker.sendEvent("more-power", "click", "", new Long(0));
                if (Build.VERSION.SDK_INT < 14) {
                    Uri uri = Uri.parse("http://127.0.0.1:" + ConfigUtils.getHttpManagerPort());
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                } else {
                    showWebView();
                }
            }
        });
        if (isReady) {
            onReady();
            showWebView();
        } else {
            LaunchService.execute(this);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        this.gestureDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
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
            CookieSyncManager.getInstance().sync();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isReady) {
            CheckDnsPollutionService.execute(this);
            WebView webView = (WebView) findViewById(R.id.webView);
            webView.loadUrl("javascript:onResume()");
            showWebView();
        }
    }

    private void loadWebView() {
        if (Build.VERSION.SDK_INT < 14) {
            return;
        }
        WebView webView = (WebView) findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAppCacheEnabled(false);
        webView.loadUrl("http://127.0.0.1:" + ConfigUtils.getHttpManagerPort() + "/home");
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                LogUtils.i("url: " + url);
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                CookieSyncManager.getInstance().sync();
            }
        });
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
            menu.add(Menu.NONE, ITEM_ID_OPEN_FULL_GOOGLE_PLAY, Menu.NONE, R.string.menu_open_full_google_play);
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
        } else if (ITEM_ID_OPEN_FULL_GOOGLE_PLAY == item.getItemId()) {
            openFullGooglePlay();
        }
        return super.onMenuItemSelected(featureId, item);
    }

    private void openAbout() {
        WebView web = new WebView(this);
        web.loadUrl(_(R.string.about_page));
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
                .setPositiveButton(R.string.about_info_share, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent = new Intent(android.content.Intent.ACTION_SEND);
                        intent.setType("text/plain");
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                        intent.putExtra(Intent.EXTRA_SUBJECT, _(R.string.share_subject));
                        if (null == shareUrl || shareUrl.trim().isEmpty()) {
                            shareUrl = "https://s3-ap-southeast-1.amazonaws.com/fqrouter/fqrouter-latest.apk";
                        }
                        intent.putExtra(Intent.EXTRA_TEXT, String.format(_(R.string.share_content), shareUrl));
                        startActivity(Intent.createChooser(intent, _(R.string.share_channel)));
                    }
                })
                .setNegativeButton(R.string.about_info_close, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                })
                .setView(web)
                .create()
                .show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    shareUrl = DnsUtils.resolveTXT(_(R.string.share_url_domain));
                } catch (Exception e) {
                    LogUtils.e("failed to resolve share url");
                }
            }
        }).start();
    }

    public static void displayNotification(Context context, String text) {
        if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("NotificationEnabled", true)) {
            clearNotification(context);
            return;
        }
        if (LaunchService.isVpnRunning(context)) {
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
        try {
            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
            notificationManager.cancel(1983);
        } catch (Exception e) {
            LogUtils.e("failed to clear notification", e);
        }
    }

    public void updateStatus(String status, int progress) {
        LogUtils.i(status);
        TextView textView = (TextView) findViewById(R.id.statusTextView);
        textView.setText(status);
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setProgress(progress);
    }

    public void exit() {
        if (LaunchService.isVpnRunning(this)) {
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
            if (LaunchService.isVpnRunning(this)) {
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
        checkUpdate();
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
        if (LaunchService.isVpnRunning(this)) {
            LogUtils.e("vpn is already running, do not start it again");
            return;
        }
        String[] fds = new File("/proc/self/fd").list();
        if (null == fds) {
            LogUtils.e("failed to list /proc/self/fd");
            onHandleFatalError(_(R.string.status_vpn_rejected));
            return;
        }
        if (fds.length > 500) {
            LogUtils.e("too many fds before start: " + fds.length);
            onHandleFatalError(_(R.string.status_vpn_rejected));
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
        LogUtils.e("fatal error: " + message);
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
                if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    gaTracker.sendEvent("more-power", "swipe", "", new Long(0));
                    showWebView();
                }
            } catch (Exception e) {
                LogUtils.e("failed to swipe", e);
            }
            return false;
        }
    }

    private void showWebView() {
        if (Build.VERSION.SDK_INT < 14) {
            return;
        }
        if (isReady) {
            final TextView statusTextView = (TextView) findViewById(R.id.statusTextView);
            statusTextView.setText(R.string.status_loading_page);
            statusTextView.setVisibility(View.VISIBLE);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    statusTextView.setVisibility(View.GONE);
                }
            }, 2000);
            findViewById(R.id.progressBar).setVisibility(View.GONE);
            findViewById(R.id.hintTextView).setVisibility(View.GONE);
            findViewById(R.id.fullPowerButton).setVisibility(View.GONE);
            findViewById(R.id.webView).setVisibility(View.VISIBLE);
        }
    }

    private void openFullGooglePlay() {
        if (ShellUtils.isRooted()) {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .setTitle(R.string.full_google_play_title)
                    .setMessage(R.string.full_google_play_message)
                    .setPositiveButton(R.string.full_google_play_open, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            killGooglePlay();
                            openGooglePlay();
                        }
                    })
                    .setNegativeButton(R.string.full_google_play_clean_and_open, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            killGooglePlay();
                            try {
                                ShellUtils.sudo("/data/data/fq.router2/busybox", "rm", "-rf", "/data/data/com.android.vending/*");
                            } catch (Exception e) {
                                LogUtils.e("failed to clear google play data", e);
                                showToast(R.string.failed_to_open_google_play);
                                return;
                            }
                            openGooglePlay();
                        }
                    })
                    .show();
        } else {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .setTitle(R.string.stop_google_play_title)
                    .setMessage(R.string.stop_google_play_message)
                    .setPositiveButton(R.string.stop_google_play_done, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            openGooglePlay();
                        }
                    })
                    .setNegativeButton(R.string.stop_google_play_do, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ApkUtils.showInstalledAppDetails(MainActivity.this, "com.android.vending");
                        }
                    })
                    .show();
        }
    }

    private void killGooglePlay() {
        try {
            ShellUtils.sudo("/data/data/fq.router2/busybox", "killall", "com.android.vending");
        } catch (Exception e) {
            LogUtils.e("failed to stop google play", e);
        }
    }

    private void openGooglePlay() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    HttpUtils.post("http://127.0.0.1:" + ConfigUtils.getHttpManagerPort() + "/force-us-ip");
                    Intent LaunchIntent = getPackageManager().getLaunchIntentForPackage("com.android.vending");
                    startActivity(LaunchIntent);
                } catch (Exception e) {
                    LogUtils.e("failed to open google play", e);
                    showToast(R.string.failed_to_open_google_play);
                }
            }
        }).start();
    }
}
