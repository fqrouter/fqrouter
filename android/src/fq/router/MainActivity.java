package fq.router;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import fq.router.utils.HttpUtils;
import fq.router.utils.IOUtils;
import fq.router.utils.ShellUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;


public class MainActivity extends Activity implements StatusUpdater {

    private final static int SHOW_AS_ACTION_IF_ROOM = 1;
    private final static int ITEM_ID_EXIT = 1;
    private final static int ITEM_ID_REPORT_ERROR = 2;
    private Handler handler = new Handler();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        setupUI();
        new Thread(new Runnable() {
            @Override
            public void run() {
                appendLog("ver: " + getMyVersion());
                if (Supervisor.ping()) {
                    appendLog("found manager is already running");
                    onStarted();
                } else {
                    appendLog("starting supervisor thread");
                    new Thread(new Supervisor(getAssets(), MainActivity.this)).start();
                }
            }
        }).start();
    }

    private void setupUI() {
        TextView textView = (TextView) findViewById(R.id.logTextView);
        textView.setMovementMethod(new ScrollingMovementMethod());
        final CheckBox wifiHotspotCheckBox = (CheckBox) findViewById(R.id.wifiHotspotCheckBox);
        wifiHotspotCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final boolean checked = wifiHotspotCheckBox.isChecked();
                wifiHotspotCheckBox.setVisibility(View.INVISIBLE);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (checked) {
                            startWifiHotspot();
                        } else {
                            stopWifiHotspot();
                        }
                    }
                }).start();
            }
        });
    }

    private void showWifiHotspotCheckbox(final boolean checked) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                final CheckBox wifiHotspotCheckBox = (CheckBox) findViewById(R.id.wifiHotspotCheckBox);
                wifiHotspotCheckBox.setChecked(checked);
                wifiHotspotCheckBox.setVisibility(View.VISIBLE);
            }
        }, 0);
    }

    private void startWifiHotspot() {
        try {
            updateStatus("Starting wifi hotspot");
            HttpUtils.post("http://127.0.0.1:8318/wifi/start");
            updateStatus("Started wifi hotspot");
            showWifiHotspotCheckbox(true);
        } catch (Exception e) {
            reportError("failed to start wifi hotspot", e);
            stopWifiHotspot();
            showWifiHotspotCheckbox(false);
        }
    }

    private void stopWifiHotspot() {
        try {
            updateStatus("Stopping wifi hotspot");
            HttpUtils.post("http://127.0.0.1:8318/wifi/stop");
        } catch (Exception e) {
            reportError("failed to stop wifi hotspot", e);
        }
        WifiManager wifiManager = (WifiManager) getBaseContext().getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(false);
        wifiManager.setWifiEnabled(true);
        updateStatus("Stopped wifi hotspot");
        showWifiHotspotCheckbox(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        addMenuItem(menu, ITEM_ID_REPORT_ERROR, "Report Error");
        addMenuItem(menu, ITEM_ID_EXIT, "Exit");
        return super.onCreateOptionsMenu(menu);
    }

    private void addMenuItem(Menu menu, int menuItemId, String caption) {
        MenuItem exitMenuItem = menu.add(Menu.NONE, menuItemId, Menu.NONE, caption);
        try {
            Method method = MenuItem.class.getMethod("setShowAsAction", int.class);
            try {
                method.invoke(exitMenuItem, SHOW_AS_ACTION_IF_ROOM);
            } catch (Exception e) {
            }
        } catch (NoSuchMethodException e) {
        }
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (ITEM_ID_EXIT == item.getItemId()) {
            onExitClicked();
        } else if (ITEM_ID_REPORT_ERROR == item.getItemId()) {
            onReportErrorClicked();
        }
        return super.onMenuItemSelected(featureId, item);
    }

    private void onExitClicked() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                stopWifiHotspot();
                updateStatus("Exiting...");
                try {
                    ManagerProcess.kill();
                } catch (Exception e) {
                    Log.e("fqrouter", "failed to kill manager process", e);
                }
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        clearNotification();
                        finish();
                    }

                }, 0);
            }
        }).start();
    }

    private void showNotification(String text) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, 0);
        Notification noti = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.wall_green)
                .setContentTitle("fqrouter is running")
                .setContentText(text)
                .setContentIntent(pIntent)
                .build();
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        noti.flags |= Notification.FLAG_ONGOING_EVENT;
        notificationManager.notify(0, noti);
    }

    private void clearNotification() {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(0);
    }

    private void onReportErrorClicked() {
        Intent i = new Intent(Intent.ACTION_SEND_MULTIPLE);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_EMAIL, new String[]{"fqrouter@gmail.com"});
        i.putExtra(Intent.EXTRA_SUBJECT, "android fqrouter error report for version " + getMyVersion());
        i.putExtra(Intent.EXTRA_TEXT, getErrorMailBody());
        createLogFiles();
        attachLogFiles(i, "/sdcard/manager.log", "/sdcard/logcat.log",
                "/sdcard/getprop.log", "/sdcard/dmesg.log");
        try {
            startActivity(Intent.createChooser(i, "Send mail..."));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(MainActivity.this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
        }
    }

    public void updateStatus(final String status) {
        appendLog("status updated to: " + status);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                TextView textView = (TextView) findViewById(R.id.statusTextView);
                textView.setText(status);
                showNotification(status);
            }
        }, 0);
    }

    public void appendLog(final String log) {
        Log.i("fqrouter", log);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                TextView textView = (TextView) findViewById(R.id.logTextView);
                textView.setText(log + "\n" + textView.getText());
            }
        }, 0);
    }

    @Override
    public void onStarted() {
        updateStatus("Checking if wifi hotspot is started");
        try {
            boolean isStarted = "TRUE".equals(HttpUtils.get("http://127.0.0.1:8318/wifi/started"));
            showWifiHotspotCheckbox(isStarted);
        } catch (Exception e) {
            Log.e("fqrouter", "failed to check if wifi hotspot is started", e);
            showWifiHotspotCheckbox(false);
        }
        updateStatus("Started, f**k censorship");
    }

    @Override
    public void reportError(final String msg, Exception e) {
        if (null == e) {
            Log.e("fqrouter", msg);
        } else {
            Log.e("fqrouter", msg, e);
        }
        updateStatus("Error: " + msg);
    }

    private String getErrorMailBody() {
        StringBuilder body = new StringBuilder();
        body.append("phone model: " + Build.MODEL + "\n");
        body.append("android version: " + Build.VERSION.RELEASE + "\n");
        body.append("kernel version: " + System.getProperty("os.version") + "\n");
        body.append("fqrouter version: " + getMyVersion() + "\n");
        return body.toString();
    }

    private String getMyVersion() {
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            return packageInfo.versionName;
        } catch (Exception e) {
            Log.e("fqrouter", "failed to get package info", e);
            return "Unknown";
        }
    }

    private void createLogFiles() {
        try {
            deployCaptureLogSh();
            ShellUtils.sudo("/system/bin/sh", "/data/data/fq.router/capture-log.sh");
        } catch (Exception e) {
            Log.e("fqrouter", "failed to execute capture-log.sh", e);
            try {
                ShellUtils.sudo(false, "/system/bin/getprop", ">", "/sdcard/getprop.log");
            } catch (Exception e2) {
                Log.e("fqrouter", "failed to execute getprop", e2);
            }
            try {
                ShellUtils.sudo(false, "/system/bin/dmesg", ">", "/sdcard/dmesg.log");
            } catch (Exception e2) {
                Log.e("fqrouter", "failed to execute dmesg", e2);
            }
            try {
                ShellUtils.sudo(false, "/system/bin/logcat", "-d", "-v", "time", "-s", "fqrouter:V", ">", "/sdcard/logcat.log");
            } catch (Exception e2) {
                Log.e("fqrouter", "failed to execute logcat", e2);
            }
        }
    }

    private void deployCaptureLogSh() {
        try {
            InputStream inputStream = getAssets().open("capture-log.sh");
            try {
                OutputStream outputStream = new FileOutputStream("/data/data/fq.router/capture-log.sh");
                try {
                    IOUtils.copy(inputStream, outputStream);
                } finally {
                    outputStream.close();
                }
            } finally {
                inputStream.close();
            }
        } catch (Exception e) {
            Log.e("fqrouter", "failed to deploy capture-log.sh", e);
        }
    }

    private void attachLogFiles(Intent i, String... logFilePaths) {
        ArrayList<Uri> logFiles = new ArrayList<Uri>();
        for (String logFilePath : logFilePaths) {
            File logFile = new File(logFilePath);
            if (logFile.exists()) {
                logFiles.add(Uri.fromFile(logFile));
            }
        }
        try {
            i.putParcelableArrayListExtra(Intent.EXTRA_STREAM, logFiles);
        } catch (Exception e) {
            Log.e("fqrouter", "failed to attach log", e);
        }
    }
}
