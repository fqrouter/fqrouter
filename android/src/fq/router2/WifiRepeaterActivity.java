package fq.router2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ToggleButton;
import fq.router2.life_cycle.LaunchService;
import fq.router2.utils.HttpUtils;
import fq.router2.utils.IOUtils;
import fq.router2.utils.LogUtils;
import fq.router2.wifi_repeater.CheckWifiRepeaterService;
import fq.router2.wifi_repeater.StartWifiRepeaterService;
import fq.router2.wifi_repeater.StopWifiRepeaterService;
import fq.router2.wifi_repeater.WifiRepeaterChangedIntent;

import java.io.File;

public class WifiRepeaterActivity extends Activity implements WifiRepeaterChangedIntent.Handler {

    private WifiManager.WifiLock wifiLock;
    private Handler handler = new Handler();
    private final static int ITEM_ID_RESET_WIFI = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wifi_repeater);
        try {
            setupUI();
        } catch (Exception e) {
            LogUtils.e("failed to setup ui", e);
        }
        WifiRepeaterChangedIntent.register(this);
        CheckWifiRepeaterService.execute(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, ITEM_ID_RESET_WIFI, Menu.NONE, R.string.menu_reset_wifi);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (ITEM_ID_RESET_WIFI == item.getItemId()) {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(R.string.reset_wifi_alert_title)
                    .setMessage(R.string.reset_wifi_alert_message)
                    .setPositiveButton(R.string.reset_wifi_alert_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            resetWifi();
                        }
                    })
                    .setNegativeButton(R.string.reset_wifi_alert_cancel, null)
                    .show();
        }
        return super.onMenuItemSelected(featureId, item);
    }

    private void resetWifi() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    HttpUtils.post("http://127.0.0.1:8318/wifi-repeater/reset");
                    WifiManager wifiManager = getWifiManager();
                    wifiManager.setWifiEnabled(false);
                    wifiManager.setWifiEnabled(true);
                    showToast(R.string.reset_wifi_succeeded);
                } catch (Exception e) {
                    LogUtils.e("failed to reset wifi", e);
                    showToast(R.string.reset_wifi_failed);
                }
            }
        }).start();
    }

    private void showToast(final int message) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(WifiRepeaterActivity.this, message, 5000).show();
            }
        }, 0);
    }

    private WifiManager getWifiManager() {
        return (WifiManager) getSystemService(Context.WIFI_SERVICE);
    }

    private void setupUI() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        EditText ssidEditText = (EditText) findViewById(R.id.ssidEditText);
        ssidEditText.setText(preferences.getString("WifiHotspotSSID", "fqrouter"));
        EditText passwordEditText = (EditText) findViewById(R.id.passwordEditText);
        passwordEditText.setText(preferences.getString("WifiHotspotPassword", "12345678"));
        final ToggleButton wifiRepeaterToggleButton = (ToggleButton) findViewById(R.id.wifiRepeaterToggleButton);
        wifiRepeaterToggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleWifiRepeater(wifiRepeaterToggleButton);
            }
        });
    }

    private void toggleWifiRepeater(final ToggleButton button) {
        if (button.isChecked()) {
            final File flagFile = new File("/data/data/fq.router2/etc/wifi-repeater-risk-notified");
            if (flagFile.exists()) {
                startWifiRepeater();
            } else {
                new AlertDialog.Builder(this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle(R.string.wifi_repeater_risk_alert_title)
                        .setMessage(R.string.wifi_repeater_risk_alert_message)
                        .setPositiveButton(R.string.wifi_repeater_risk_alert_ok, new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                IOUtils.writeToFile(flagFile, "OK");
                                startWifiRepeater();
                                button.setChecked(true);
                            }

                        })
                        .setNegativeButton(R.string.wifi_repeater_risk_alert_cancel, null)
                        .show();
                button.setChecked(false);
            }
        } else {
            stopWifiRepeater();
        }
    }

    private void stopWifiRepeater() {
        findViewById(R.id.wifiRepeaterToggleButton).setEnabled(false);
        StopWifiRepeaterService.execute(this);
    }

    private void startWifiRepeater() {
        findViewById(R.id.wifiRepeaterToggleButton).setEnabled(false);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String ssid = ((EditText) findViewById(R.id.ssidEditText)).getText().toString();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("WifiHotspotSSID", ssid);
        String password = ((EditText) findViewById(R.id.passwordEditText)).getText().toString();
        editor.putString("WifiHotspotPassword", password);
        editor.commit();
        LaunchService.updateConfigFile(this);
        StartWifiRepeaterService.execute(this);
    }

    @Override
    public void onWifiRepeaterChanged(boolean isStarted) {
        updateWifiLock(isStarted);
        ToggleButton wifiRepeaterToggleButton = (ToggleButton) findViewById(R.id.wifiRepeaterToggleButton);
        wifiRepeaterToggleButton.setChecked(isStarted);
        wifiRepeaterToggleButton.setEnabled(true);
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
}
