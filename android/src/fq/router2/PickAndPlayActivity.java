package fq.router2;

import android.app.ListActivity;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.analytics.tracking.android.Tracker;
import fq.router2.utils.HttpUtils;
import fq.router2.utils.IOUtils;
import fq.router2.utils.LogUtils;

import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;

public class PickAndPlayActivity extends ListActivity {

    private static final int ITEM_ID_RESCAN = 0;
    private android.os.Handler handler = new android.os.Handler();
    private ArrayAdapter arrayAdapter;
    private static final ArrayList<String> devices = new ArrayList<String>();
    private static final HashMap<String, String> macs = new HashMap<String, String>();
    private WifiManager.WifiLock wifiLock;
    private GoogleAnalytics gaInstance;
    private Tracker gaTracker;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pick_and_play);
        gaInstance = GoogleAnalytics.getInstance(this);
        gaTracker = gaInstance.getTracker("UA-37740383-2");
        gaTracker.setCustomDimension(1, Build.MODEL);
        gaTracker.sendEvent("pick-and-play", "enter", "", new Long(0));
        devices.clear();
        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_multiple_choice, devices);
        setListAdapter(arrayAdapter);
        scan(1);
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

    private void scan(final int factor) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    updateStatus(_(R.string.status_scanning));
                    HttpUtils.get("http://127.0.0.1:2515/pick-and-play/scan?factor=" + factor, new IOUtils.LineRead() {
                        @Override
                        public void onLineRead(String line) {
                            try {
                                String[] parts = line.split(",", -1);
                                if (4 == parts.length) {
                                    onNewDeviceDiscovered(parts[0], parts[1], parts[2], "TRUE".equals(parts[3]));
                                } else {
                                    LogUtils.e("unexpected line[" + parts.length + "]: " + line);
                                }
                            } catch (Exception e) {
                                LogUtils.e("failed to read line", e);
                            }
                        }
                    }, 0);
                    updateStatus(_(R.string.status_scan_completed));
                } catch (Exception e) {
                    updateStatus("scan failed");
                    LogUtils.e("failed to scan lan", e);
                }
            }
        }).start();
    }

    private String _(int id) {
        return getResources().getString(id);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        addMenuItem(menu, ITEM_ID_RESCAN, _(R.string.menu_rescan));
        return super.onCreateOptionsMenu(menu);
    }

    private MenuItem addMenuItem(Menu menu, int menuItemId, String caption) {
        MenuItem menuItem = menu.add(Menu.NONE, menuItemId, Menu.NONE, caption);
        try {
            Method method = MenuItem.class.getMethod("setShowAsAction", int.class);
            try {
                method.invoke(menuItem, MainActivity.SHOW_AS_ACTION_IF_ROOM);
            } catch (Exception e) {
            }
        } catch (NoSuchMethodException e) {
        }
        return menuItem;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (ITEM_ID_RESCAN == item.getItemId()) {
            devices.clear();
            arrayAdapter.notifyDataSetChanged();
            scan(2);
        }
        return super.onMenuItemSelected(featureId, item);
    }

    private void updateStatus(final String text) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                TextView statusTextBox = (TextView) findViewById(R.id.statusTextBox);
                statusTextBox.setText(text);
            }
        }, 0);

    }

    private void onNewDeviceDiscovered(final String ip, final String mac, final String hostName, final boolean isPicked) {
        updateStatus(R.string.status_scanning + " " + ip + "...");
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mac.length() > 0) {
                    macs.put(ip, mac);
                    if (hostName.length() == 0) {
                        devices.add(ip);
                    } else {
                        devices.add(ip + " " + hostName);
                    }
                    arrayAdapter.notifyDataSetChanged();
                    if (isPicked) {
                        getListView().setItemChecked(devices.size() - 1, true);
                    }
                }
            }
        }, 0);
    }

    public void onListItemClick(ListView parent, View v, int position, long id) {
        final boolean isChecked = parent.isItemChecked(position);
        if (isChecked) {
            gaTracker.sendEvent("pick-and-play", "pick", "", new Long(0));
        } else {
            gaTracker.sendEvent("pick-and-play", "unpick", "", new Long(0));
        }
        final String ip = devices.get(position).split(" ")[0].trim();
        final String mac = PickAndPlayActivity.macs.get(ip);
        if (null == wifiLock) {
            wifiLock = getWifiManager().createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "fqrouter pick and play");
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (isChecked) {
                        wifiLock.acquire();
                        HttpUtils.post("http://127.0.0.1:2515/pick-and-play/forge-default-gateway", "ip=" + URLEncoder.encode(ip, "UTF-8") +
                                "&mac=" + URLEncoder.encode(mac, "UTF-8"));
                        updateStatus(String.format(_(R.string.pick_and_play_picked_hint), ip));
                    } else {
                        if ("0".equals(HttpUtils.post("http://127.0.0.1:2515/pick-and-play/restore-default-gateway", "ip=" + URLEncoder.encode(ip, "UTF-8")))) {
                            wifiLock.release();
                        }
                        updateStatus(String.format(_(R.string.pick_and_play_unpicked_hint), ip));
                    }
                } catch (Exception e) {
                    LogUtils.e("failed to handle item click: " + ip, e);
                }
            }
        }).start();
    }


    private WifiManager getWifiManager() {
        return (WifiManager) getBaseContext().getSystemService(Context.WIFI_SERVICE);
    }
}