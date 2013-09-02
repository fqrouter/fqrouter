package fq.router2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
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
import fq.router2.wifi_repeater.*;

import java.io.File;

public class WifiRepeaterActivity extends Activity implements WifiRepeaterChangedIntent.Handler {

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

            WifiInfo wifiInfo = getWifiManager().getConnectionInfo();
            if (0 == wifiInfo.getIpAddress()) {
                new AlertDialog.Builder(this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle(R.string.wifi_repeater_in_3g_env_info_title)
                        .setMessage(R.string.wifi_repeater_in_3g_env_info_message)
                        .setPositiveButton(R.string.wifi_repeater_in_3g_env_info_use_sys, new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startActivity(new Intent(Settings.ACTION_SETTINGS));
                            }

                        })
                        .setNegativeButton(R.string.wifi_repeater_in_3g_env_info_ok, new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                alertRiskThenStartWifiRepeater(button);
                            }

                        })
                        .show();
                button.setChecked(false);
                return;
            }
            alertRiskThenStartWifiRepeater(button);
        } else {
            stopWifiRepeater();
        }
    }

    private void alertRiskThenStartWifiRepeater(final ToggleButton button) {
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
    }

    private void stopWifiRepeater() {
        findViewById(R.id.wifiRepeaterToggleButton).setEnabled(false);
        stopService(new Intent(this, WifiGuardService.class));
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
        findViewById(R.id.ssidEditText).setEnabled(false);
        findViewById(R.id.passwordEditText).setEnabled(false);
    }

    @Override
    public void onWifiRepeaterChanged(boolean isStarted) {
        if (isStarted) {
            startService(new Intent(this, WifiGuardService.class));
        } else {
            stopService(new Intent(this, WifiGuardService.class));
        }
        ToggleButton wifiRepeaterToggleButton = (ToggleButton) findViewById(R.id.wifiRepeaterToggleButton);
        wifiRepeaterToggleButton.setChecked(isStarted);
        wifiRepeaterToggleButton.setEnabled(true);
        if (isStarted) {
            findViewById(R.id.ssidEditText).setEnabled(false);
            findViewById(R.id.passwordEditText).setEnabled(false);
        } else {
            findViewById(R.id.ssidEditText).setEnabled(true);
            findViewById(R.id.passwordEditText).setEnabled(true);
        }
    }
}
