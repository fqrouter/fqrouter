package fq.router;

import android.app.*;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
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
import fq.router.utils.LogUtils;
import fq.router.wifi.*;

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
        DownloadFailedIntent.Handler {

    public final static int SHOW_AS_ACTION_IF_ROOM = 1;
    private final static int ITEM_ID_EXIT = 1;
    private final static int ITEM_ID_REPORT_ERROR = 2;
    private final static int ITEM_ID_CHECK_UPDATES = 3;
    private final static int ITEM_ID_SETTINGS = 4;
    private final static int ITEM_ID_PICK_AND_PLAY = 5;
    private boolean started;
    private WifiManager.WifiLock wifiLock;

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
        LaunchService.execute(this);
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
        menu.add(Menu.NONE, ITEM_ID_PICK_AND_PLAY, Menu.NONE, "Pick & Play");
        menu.add(Menu.NONE, ITEM_ID_SETTINGS, Menu.NONE, "Settings");
        menu.add(Menu.NONE, ITEM_ID_CHECK_UPDATES, Menu.NONE, "Check Updates");
        addMenuItem(menu, ITEM_ID_REPORT_ERROR, "Report Error");
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
            reportError();
        } else if (ITEM_ID_CHECK_UPDATES == item.getItemId()) {
            checkUpdate();
        } else if (ITEM_ID_SETTINGS == item.getItemId()) {
            startActivity(new Intent(this, SettingsActivity.class));
        } else if (ITEM_ID_PICK_AND_PLAY == item.getItemId()) {
            startActivity(new Intent(this, PickAndPlayActivity.class));
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

    private void checkUpdate() {
        CheckUpdateService.execute(this);
    }

    @Override
    public void updateStatus(String status) {
        appendLog("status updated to: " + status);
        TextView textView = (TextView) findViewById(R.id.statusTextView);
        textView.setText(status);
        showNotification(status);
    }

    public void appendLog(final String log) {
        LogUtils.i(log);
        TextView textView = (TextView) findViewById(R.id.logTextView);
        textView.setText(log + "\n" + textView.getText());
    }

    private void reportError() {
        try {
            startActivity(Intent.createChooser(new ErrorReportEmail(this).prepare(), "Send mail..."));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(MainActivity.this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
        }
    }

    private void exit() {
        started = false;
        hideWifiHotspotToggleButton();
        ExitService.execute(this);
    }

    private void toggleWifiHotspot(ToggleButton button) {
        if (button.isChecked()) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            String mode = preferences.getString("WifiHotspotMode", WifiHotspotHelper.MODE_WIFI_REPEATER);
            startWifiHotspot(mode);
        } else {
            hideWifiHotspotToggleButton();
            StopWifiHotspotService.execute(this);
        }
    }

    private void startWifiHotspot(String mode) {
        hideWifiHotspotToggleButton();
        StartWifiHotspotService.execute(this, mode);
    }

    public void hideWifiHotspotToggleButton() {
        findViewById(R.id.wifiHotspotPanel).setVisibility(View.INVISIBLE);
    }

    @Override
    public void onLaunched() {
        started = true;
        checkUpdate();
        CheckWifiHotspotService.execute(this);
    }

    @Override
    public void onUpdateFound(String latestVersion, final String upgradeUrl) {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("There are newer version")
                .setMessage("Do you want to upgrade now?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        updateStatus("Downloading " + Uri.parse(upgradeUrl).getLastPathSegment());
                        DownloadService.execute(
                                MainActivity.this, upgradeUrl.replace("http", "https"), "/sdcard/fqrouter-latest.apk");
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
    public void onWifiHotspotChanged(boolean isStarted, boolean wasStartingWifiRepeater) {
        updateWifiLock(isStarted);
        showWifiHotspotToggleButton(isStarted);
        if (!isStarted && wasStartingWifiRepeater) {
            askIfStartTraditionalWifiHotspot();
        }
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

    private void askIfStartTraditionalWifiHotspot() {
        new AlertDialog.Builder(MainActivity.this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Failed to start wifi repeater")
                .setMessage("Do you want to start traditional wifi hotspot sharing 3G connection? " +
                        "It will consume your 3G data traffic volume. " +
                        "Or you can try 'Pick & Play' from the menu, " +
                        "it can share free internet to devices in your current wifi network")

                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startWifiHotspot(WifiHotspotHelper.MODE_TRADITIONAL_WIFI_HOTSPOT);
                    }

                })
                .setNegativeButton("No, thanks", null)
                .show();
    }

    @Override
    public void onDownloadFailed(final String url, String downloadTo) {
        updateStatus("Error: download " + Uri.parse(url).getLastPathSegment() + " failed");
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
        updateStatus("Downloaded " + Uri.parse(url).getLastPathSegment());
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse("file://" + downloadTo), "application/vnd.android.package-archive");
        startActivity(intent);
    }

    @Override
    public void onDownloading(String url, String downloadTo, int percent) {
        showNotification("Downloading " + Uri.parse(url).getLastPathSegment() + ": " + percent + "%");
        TextView textView = (TextView) findViewById(R.id.statusTextView);
        textView.setText("Downloaded: " + percent + "%");
    }
}
