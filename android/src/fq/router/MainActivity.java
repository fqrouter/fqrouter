package fq.router;

import android.app.*;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import fq.router.utils.HttpUtils;

import java.io.File;
import java.lang.reflect.Method;


public class MainActivity extends Activity implements StatusUpdater {

    public final static int SHOW_AS_ACTION_IF_ROOM = 1;
    private final static int ITEM_ID_EXIT = 1;
    private final static int ITEM_ID_REPORT_ERROR = 2;
    private final static int ITEM_ID_CHECK_UPDATES = 3;
    private final static int ITEM_ID_SETTINGS = 4;
    private final static int ITEM_ID_PICK_AND_PLAY = 5;
    private final static File EXITING_FLAG = new File("/data/data/fq.router/.exiting");
    private Handler handler = new Handler();
    private boolean started = false;
    private final Supervisor supervisor = new Supervisor(this);
    private final WifiHotspot wifiHotspot = new WifiHotspot(this);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        setupUI();
        new Thread(new Runnable() {
            @Override
            public void run() {
                appendLog("ver: " + getMyVersion());
                if (!shouldStart()) {
                    finish();
                    return;
                }
                if (Supervisor.ping() || Supervisor.ping() || Supervisor.ping()) {
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
        int threeMinutes = 3 * 60 * 1000;
        if (delta > threeMinutes) {
            appendLog("exiting flag expired: " + (delta - threeMinutes));
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
                applyWifiHotspotIsStarted(checked);
            }
        });
    }

    public void hideWifiHotspotToggleButton() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.wifiHotspotPanel).setVisibility(View.INVISIBLE);
            }
        }, 0);
    }

    private void applyWifiHotspotIsStarted(final boolean isStarted) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (isStarted) {
                    startWifiHotspot();
                } else {
                    wifiHotspot.stop();
                }
            }
        }).start();
    }

    private void startWifiHotspot() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String mode = preferences.getString("WifiHotspotMode", WifiHotspot.MODE_WIFI_REPEATER);
        boolean started = wifiHotspot.start(mode);
        if (!started) {
            if (hasP2pFirmware()) {
                askIfDownloadP2pFirmware();
            } else if (WifiHotspot.MODE_WIFI_REPEATER.equals(mode)) {
                askIfStartTraditionalWifiHotspot();
            }
        }
    }

    private boolean hasP2pFirmware() {
        try {
            return "TRUE".equals(HttpUtils.get("http://127.0.0.1:8318/wifi/has-p2p-firmware"));
        } catch (Exception e) {
            Log.e("fqrouter", "failed to check if p2p firmware is available to download", e);
            return false;
        }
    }

    private void askIfDownloadP2pFirmware() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(MainActivity.this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle("Failed to start wifi repeater")
                        .setMessage("Do you want to download wifi chipset firmware that might fix the problem?")

                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (downloadP2pFirmware()) {
                                            wifiHotspot.start(WifiHotspot.MODE_WIFI_REPEATER);
                                        }
                                    }
                                }).start();
                            }

                        })
                        .setNegativeButton("No, thanks", null)
                        .show();
            }
        }, 2000);
    }

    private boolean downloadP2pFirmware() {
        updateStatus("Downloading wifi chipset firmware");
        try {
            HttpUtils.post("http://127.0.0.1:8318/wifi/download-p2p-firmware");
            updateStatus("Downloaded wifi chipset firmware");
            return true;
        } catch (Exception e) {
            reportError("failed to download wifi chipset firmware", e);
            return false;
        }
    }

    private void askIfStartTraditionalWifiHotspot() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
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
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        wifiHotspot.start(WifiHotspot.MODE_TRADITIONAL_WIFI_HOTSPOT);
                                    }
                                }).start();
                            }

                        })
                        .setNegativeButton("No, thanks", null)
                        .show();
            }
        }, 2000);
    }

    public void showWifiHotspotToggleButton(final boolean isStarted) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                final ToggleButton wifiHotspotToggleButton = (ToggleButton) findViewById(R.id.wifiHotspotToggleButton);
                wifiHotspotToggleButton.setChecked(isStarted);
                final View wifiHotspotPanel = findViewById(R.id.wifiHotspotPanel);
                wifiHotspotPanel.setVisibility(View.VISIBLE);
            }
        }, 0);
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
            onExitClicked();
        } else if (ITEM_ID_REPORT_ERROR == item.getItemId()) {
            onReportErrorClicked();
        } else if (ITEM_ID_CHECK_UPDATES == item.getItemId()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    appendLog("checking updates");
                    Supervisor.checkUpdates(MainActivity.this);
                }
            }).start();
        } else if (ITEM_ID_SETTINGS == item.getItemId()) {
            startActivity(new Intent(this, SettingsActivity.class));
        } else if (ITEM_ID_PICK_AND_PLAY == item.getItemId()) {
            startActivity(new Intent(this, PickAndPlayActivity.class));
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
        try {
            startActivity(Intent.createChooser(new ErrorReportEmail(this).prepare(), "Send mail..."));
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
}
