package fq.router2;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.*;
import android.widget.EditText;
import android.widget.Toast;
import fq.router2.life_cycle.LaunchService;
import fq.router2.utils.ApkUtils;
import fq.router2.utils.HttpUtils;
import fq.router2.utils.LogUtils;
import fq.router2.utils.ShellUtils;

import java.util.List;

public class MainSettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private Handler handler = new Handler();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        findPreference("WifiHotspotPassword").setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {

                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        String password = (String) newValue;
                        if (password.length() < 8) {
                            showToast(_(R.string.pref_wifi_hotspot_password_constraint));
                            return false;
                        }
                        return true;
                    }

                });
        findPreference("GoAgentPrivateServersPicker").setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        onGoAgentPrivateServerPicked((String) newValue);
                        return false;
                    }
                });
        findPreference("ShadowsocksPrivateServersPicker").setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        onShadowsocksPrivateServerPicked((String) newValue);
                        return false;
                    }
                });
        findPreference("HttpProxyPrivateServersPicker").setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        onHttpProxyPrivateServerPicked((String) newValue);
                        return false;
                    }
                });
        findPreference("SshPrivateServersPicker").setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        onSshPrivateServerPicked((String) newValue);
                        return false;
                    }
                });
        findPreference("WifiHotspotReset").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                resetWifi();
                return false;
            }
        });
        findPreference("OpenFullGooglePlay").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                openFullGooglePlay();
                return false;
            }
        });
        if (!ShellUtils.isRooted()) {
            getPreferenceScreen().removePreference(findPreference("WifiHotspot"));
            PreferenceCategory generalCategoryPref = (PreferenceCategory) findPreference("General");
            generalCategoryPref.removePreference(generalCategoryPref.findPreference("AutoLaunchEnabled"));
            PreferenceCategory bypassCategoryPref = (PreferenceCategory) findPreference("Bypass");
            bypassCategoryPref.removePreference(bypassCategoryPref.findPreference("TcpScramblerEnabled"));
        }
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

    private void openFullGooglePlay() {
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
                        ApkUtils.showInstalledAppDetails(MainSettingsActivity.this, "com.android.vending");
                    }
                })
                .show();
    }

    private void openGooglePlay() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    HttpUtils.post("http://127.0.0.1:8319/force-us-ip");
                    Intent LaunchIntent = getPackageManager().getLaunchIntentForPackage("com.android.vending");
                    startActivity(LaunchIntent);
                } catch (Exception e) {
                    LogUtils.e("failed to open google play", e);
                    showToast(R.string.failed_to_open_google_play);
                }
            }
        }).start();
    }

    private WifiManager getWifiManager() {
        return (WifiManager) getSystemService(Context.WIFI_SERVICE);
    }

    private void showToast(final int message) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainSettingsActivity.this, message, 5000).show();
            }
        }, 0);
    }


    @Override
    protected void onResume() {
        super.onResume();
        initGoAgent();
        initShadowsocks();
        initHttpProxy();
        initSshProxy();
        PreferenceManager
                .getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        PreferenceManager
                .getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    private void initGoAgent() {
        final ListPreference picker = (ListPreference) findPreference("GoAgentPrivateServersPicker");
        List<GoAgentSettingsActivity.Server> servers = GoAgentSettingsActivity.loadServers();
        CharSequence[] entries = new CharSequence[servers.size() + 2];
        entries[servers.size()] = _(R.string.pref_add);
        entries[servers.size() + 1] = _(R.string.pref_batch_add);
        CharSequence[] entryValues = new CharSequence[servers.size() + 2];
        entryValues[servers.size()] = _(R.string.pref_add);
        entryValues[servers.size() + 1] = _(R.string.pref_batch_add);
        for (int i = 0; i < servers.size(); i++) {
            GoAgentSettingsActivity.Server server = servers.get(i);
            if (server.appid.equals("")) {
                entries[i] = _(R.string.pref_host_not_set);
            } else {
                entries[i] = server.appid;
            }
            entryValues[i] = String.valueOf(i);
        }
        picker.setEntries(entries);
        picker.setEntryValues(entryValues);
    }

    private void initShadowsocks() {
        final ListPreference picker = (ListPreference) findPreference("ShadowsocksPrivateServersPicker");
        List<ShadowsocksSettingsActivity.Server> servers = ShadowsocksSettingsActivity.loadServers();
        CharSequence[] entries = new CharSequence[servers.size() + 1];
        entries[servers.size()] = _(R.string.pref_add);
        CharSequence[] entryValues = new CharSequence[servers.size() + 1];
        entryValues[servers.size()] = _(R.string.pref_add);
        for (int i = 0; i < servers.size(); i++) {
            ShadowsocksSettingsActivity.Server server = servers.get(i);
            if (server.host.equals("")) {
                entries[i] = _(R.string.pref_host_not_set);
            } else {
                entries[i] = server.host;
            }
            entryValues[i] = String.valueOf(i);
        }
        picker.setEntries(entries);
        picker.setEntryValues(entryValues);
    }

    private void initHttpProxy() {
        final ListPreference picker = (ListPreference) findPreference("HttpProxyPrivateServersPicker");
        List<HttpProxySettingsActivity.Server> servers = HttpProxySettingsActivity.loadServers();
        CharSequence[] entries = new CharSequence[servers.size() + 1];
        entries[servers.size()] = _(R.string.pref_add);
        CharSequence[] entryValues = new CharSequence[servers.size() + 1];
        entryValues[servers.size()] = _(R.string.pref_add);
        for (int i = 0; i < servers.size(); i++) {
            HttpProxySettingsActivity.Server server = servers.get(i);
            if (server.host.equals("")) {
                entries[i] = _(R.string.pref_host_not_set);
            } else {
                entries[i] = server.host;
            }
            entryValues[i] = String.valueOf(i);
        }
        picker.setEntries(entries);
        picker.setEntryValues(entryValues);
    }

    private void initSshProxy() {
        final ListPreference picker = (ListPreference) findPreference("SshPrivateServersPicker");
        List<SshSettingsActivity.Server> servers = SshSettingsActivity.loadServers();
        CharSequence[] entries = new CharSequence[servers.size() + 1];
        entries[servers.size()] = _(R.string.pref_add);
        CharSequence[] entryValues = new CharSequence[servers.size() + 1];
        entryValues[servers.size()] = _(R.string.pref_add);
        for (int i = 0; i < servers.size(); i++) {
            SshSettingsActivity.Server server = servers.get(i);
            if (server.host.equals("")) {
                entries[i] = _(R.string.pref_host_not_set);
            } else {
                entries[i] = server.host;
            }
            entryValues[i] = String.valueOf(i);
        }
        picker.setEntries(entries);
        picker.setEntryValues(entryValues);
    }

    private String _(int id) {
        return getResources().getString(id);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.contains("Shortcut") || key.contains("Scrambler") || key.contains("DirectAccess")) {
            if (!sharedPreferences.getBoolean(key, true)) {
                new AlertDialog.Builder(this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle(R.string.internet_speed_alert_title)
                        .setMessage(R.string.internet_speed_alert_message)
                        .setPositiveButton(R.string.internet_speed_alert_ok, null)
                        .show();
            }
        }
        if (key.startsWith("WifiHotspot")) {
            showToast(_(R.string.pref_restart_wifi_repeater));
        } else {
            showToast(_(R.string.pref_restart_app));
        }
        LaunchService.updateConfigFile(this);
    }

    private void onGoAgentPrivateServerPicked(String value) {
        if (_(R.string.pref_add).equals(value)) {
            showP2PAgreement(new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(MainSettingsActivity.this, GoAgentSettingsActivity.class);
                    List<GoAgentSettingsActivity.Server> servers = GoAgentSettingsActivity.loadServers();
                    servers.add(new GoAgentSettingsActivity.Server());
                    GoAgentSettingsActivity.saveServers(servers);
                    intent.putExtra("index", servers.size() - 1);
                    startActivity(intent);
                }
            });
        } else if (_(R.string.pref_batch_add).equals(value)) {
            showP2PAgreement(new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    showGoAgentBatchAdd();
                }
            });
        } else {
            Intent intent = new Intent(this, GoAgentSettingsActivity.class);
            intent.putExtra("index", Integer.valueOf(value));
            startActivity(intent);
        }
    }

    private void showGoAgentBatchAdd() {
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle(R.string.pref_goagent_batch_add_input_title);
        alert.setMessage(R.string.pref_goagent_batch_add_input_message);
        final EditText input = new EditText(this);
        alert.setView(input);

        alert.setPositiveButton(R.string.pref_goagent_batch_add_input_add_more, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                addGoAgentPrivateServer(input.getText().toString());
                input.setText("");
                new android.os.Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        showGoAgentBatchAdd();
                    }
                }, 500);
            }
        });

        alert.setNegativeButton(R.string.pref_goagent_batch_add_input_done, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                addGoAgentPrivateServer(input.getText().toString());
            }
        });
        alert.show();
    }

    private void addGoAgentPrivateServer(final String value) {
        if (value.trim().length() == 0) {
            return;
        }
        List<GoAgentSettingsActivity.Server> servers = GoAgentSettingsActivity.loadServers();
        servers.add(new GoAgentSettingsActivity.Server() {{
            appid = value.trim();
        }});
        GoAgentSettingsActivity.saveServers(servers);
        initGoAgent();
    }

    private void onShadowsocksPrivateServerPicked(String value) {
        if (_(R.string.pref_add).equals(value)) {
            showP2PAgreement(new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(MainSettingsActivity.this, ShadowsocksSettingsActivity.class);
                    List<ShadowsocksSettingsActivity.Server> servers = ShadowsocksSettingsActivity.loadServers();
                    servers.add(new ShadowsocksSettingsActivity.Server());
                    ShadowsocksSettingsActivity.saveServers(servers);
                    intent.putExtra("index", servers.size() - 1);
                    startActivity(intent);
                }
            });
        } else {
            Intent intent = new Intent(this, ShadowsocksSettingsActivity.class);
            intent.putExtra("index", Integer.valueOf(value));
            startActivity(intent);
        }
    }

    private void onHttpProxyPrivateServerPicked(String value) {
        if (_(R.string.pref_add).equals(value)) {
            showP2PAgreement(new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(MainSettingsActivity.this, HttpProxySettingsActivity.class);
                    List<HttpProxySettingsActivity.Server> servers = HttpProxySettingsActivity.loadServers();
                    servers.add(new HttpProxySettingsActivity.Server());
                    HttpProxySettingsActivity.saveServers(servers);
                    intent.putExtra("index", servers.size() - 1);
                    startActivity(intent);
                }
            });
        } else {
            Intent intent = new Intent(this, HttpProxySettingsActivity.class);
            intent.putExtra("index", Integer.valueOf(value));
            startActivity(intent);
        }
    }

    private void onSshPrivateServerPicked(String value) {
        if (_(R.string.pref_add).equals(value)) {
            showP2PAgreement(new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(MainSettingsActivity.this, SshSettingsActivity.class);
                    List<SshSettingsActivity.Server> servers = SshSettingsActivity.loadServers();
                    servers.add(new SshSettingsActivity.Server());
                    SshSettingsActivity.saveServers(servers);
                    intent.putExtra("index", servers.size() - 1);
                    startActivity(intent);
                }
            });
        } else {
            Intent intent = new Intent(this, SshSettingsActivity.class);
            intent.putExtra("index", Integer.valueOf(value));
            startActivity(intent);
        }
    }

    private void showP2PAgreement(DialogInterface.OnClickListener onAgreed) {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.p2p_agreement_alert_title)
                .setMessage(R.string.p2p_agreement_alert_message)
                .setPositiveButton(R.string.p2p_agreement_alert_yes, onAgreed)
                .setNegativeButton(R.string.p2p_agreement_alert_no, null)
                .show();
    }

    private void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }
}
