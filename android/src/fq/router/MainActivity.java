package fq.router;

import android.app.*;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
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
    private final static int ITEM_ITEM_CHECK_UPDATES = 3;
    private final static File EXITING_FLAG = new File("/data/data/fq.router/.exiting");
    private Handler handler = new Handler();
    private boolean started = false;
    private final Supervisor supervisor = new Supervisor(this);
    private final WifiHotspot wifiHotspot = new WifiHotspot(this);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        setupUI();
        new Thread(new Runnable() {
            @Override
            public void run() {
                appendLog("ver: " + getMyVersion());
                if (!shouldStart()) {
                    finish();
                    return;
                }
                if (Supervisor.ping()) {
                    appendLog("found manager is already running");
                    onStarted();
                } else {
                    appendLog("starting supervisor thread");
                    new Thread(supervisor).start();
                }
            }
        }).start();
    }

    private boolean shouldStart() {
        if (!EXITING_FLAG.exists()) {
            appendLog("exiting flag not found");
            return true;
        }
        long delta = System.currentTimeMillis() - EXITING_FLAG.lastModified();
        int tenMinutes = 60 * 10 * 1000;
        if (delta > tenMinutes) {
            appendLog("exiting flag expired: " + (delta - tenMinutes));
            EXITING_FLAG.delete();
            return true;
        }
        if (ManagerProcess.exists()) {
            appendLog("exiting flag and manager process found");
            return false;
        }
        appendLog("exiting flag found and ignored due to manager process missing");
        EXITING_FLAG.delete();
        return true;
    }

    private void setupUI() {
        TextView textView = (TextView) findViewById(R.id.logTextView);
        textView.setMovementMethod(new ScrollingMovementMethod());
        final ToggleButton wifiHotspotToggleButton = (ToggleButton) findViewById(R.id.wifiHotspotToggleButton);
        wifiHotspotToggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final boolean checked = wifiHotspotToggleButton.isChecked();
                findViewById(R.id.wifiHotspotPanel).setVisibility(View.INVISIBLE);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (checked) {
                            startWifiHotspot();
                        } else {
                            wifiHotspot.stop();
                        }
                    }
                }).start();
            }
        });
    }

    private void startWifiHotspot() {
        boolean wasConnected = wifiHotspot.isConnected();
        boolean started = wifiHotspot.start(WifiHotspot.MODE_WIFI_REPEATER);
        if (!started) {
            if (wasConnected) {
                askIfStartTraditionalWifiHotspot();
            } else {
                wifiHotspot.start(WifiHotspot.MODE_TRADITIONAL_WIFI_HOTSPOT);
            }
        }
    }

    private void askIfStartTraditionalWifiHotspot() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(MainActivity.this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle("Failed to start wifi repeater")
                        .setMessage("Do you want to start wifi hotspot sharing 3G connection? " +
                                "It will consume your 3G data traffic volume.")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        wifiHotspot.start(WifiHotspot.MODE_TRADITIONAL_WIFI_HOTSPOT);
                                    }
                                }).start();
                            }

                        })
                        .setNegativeButton("No", null)
                        .show();
            }
        }, 2000);
    }

    public void showWifiHotspotToggleButton(final boolean checked) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                final ToggleButton wifiHotspotToggleButton = (ToggleButton) findViewById(R.id.wifiHotspotToggleButton);
                wifiHotspotToggleButton.setChecked(checked);
                final View wifiHotspotPanel = findViewById(R.id.wifiHotspotPanel);
                wifiHotspotPanel.setVisibility(View.VISIBLE);
            }
        }, 0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, ITEM_ITEM_CHECK_UPDATES, Menu.NONE, "Check Updates");
        addMenuItem(menu, ITEM_ID_REPORT_ERROR, "Report Error");
        addMenuItem(menu, ITEM_ID_EXIT, "Exit");
        return super.onCreateOptionsMenu(menu);
    }

    private void addMenuItem(Menu menu, int menuItemId, String caption) {
        MenuItem menuItem = menu.add(Menu.NONE, menuItemId, Menu.NONE, caption);
        try {
            Method method = MenuItem.class.getMethod("setShowAsAction", int.class);
            try {
                method.invoke(menuItem, SHOW_AS_ACTION_IF_ROOM);
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
        } else if (ITEM_ITEM_CHECK_UPDATES == item.getItemId()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    appendLog("checking updates");
                    Supervisor.checkUpdates(MainActivity.this);
                }
            }).start();
        }
        return super.onMenuItemSelected(featureId, item);
    }

    private void onExitClicked() {
        try {
            EXITING_FLAG.createNewFile();
        } catch (Exception e) {
            Log.e("fqrouter", "failed to create .exit flag", e);
        }
        EXITING_FLAG.setLastModified(System.currentTimeMillis());
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            startActivity(intent);
        } catch (Exception e) {
            Log.e("fqrouter", "failed to go back home screen", e);
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (started && WifiHotspot.isStarted()) {
                    wifiHotspot.stop();
                }
                updateStatus("Exiting...", false);
                try {
                    ManagerProcess.kill();
                } catch (Exception e) {
                    Log.e("fqrouter", "failed to kill manager process", e);
                }
                EXITING_FLAG.delete();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        clearNotification();
                    }

                }, 0);
                finish();
            }
        }).start();
    }

    private void showNotification(String text, boolean hasIntent) {
        Intent intent = new Intent(this, MainActivity.class);
        if (!hasIntent) {
            intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
        }
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
        updateStatus(status, true);
    }

    public void updateStatus(final String status, final boolean hasIntent) {
        appendLog("status updated to: " + status);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                TextView textView = (TextView) findViewById(R.id.statusTextView);
                textView.setText(status);
                try {
                    showNotification(status, hasIntent);
                } catch (Exception e) {
                    Log.e("fqrouter", "failed to show notification", e);
                }
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
        started = true;
        updateStatus("Checking if wifi hotspot is started");
        boolean isStarted = WifiHotspot.isStarted();
        showWifiHotspotToggleButton(isStarted);
        if (isStarted) {
            wifiHotspot.setup();
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

    public String getMyVersion() {
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            return packageInfo.versionName;
        } catch (Exception e) {
            Log.e("fqrouter", "failed to get package info", e);
            return "Unknown";
        }
    }

    @Override
    public void notifyNewerVersion(String latestVersion, final String upgradeUrl) {
        appendLog("latest version is: " + latestVersion);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(MainActivity.this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle("There are newer version")
                        .setMessage("Do you want to upgrade now?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(upgradeUrl)));
                            }

                        })
                        .setNegativeButton("No", null)
                        .show();
            }
        }, 0);
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
