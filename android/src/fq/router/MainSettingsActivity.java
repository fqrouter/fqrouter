package fq.router;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.*;
import android.widget.EditText;
import android.widget.Toast;
import fq.router.life.LaunchService;
import fq.router.utils.ShellUtils;

import java.util.List;

public class MainSettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private final static String P2P_AGREEMENT = "" +
            "fqrouter will not upload the details of your private server to anywhere. " +
            "However, by the nature of P2P network, " +
            "you must allow fqrouter to use your mobile phone as a P2P network node in wifi network. " +
            "fqrouter@gmail.com";

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
                            showToast("Password must be 8 characters or longer");
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
        if (!ShellUtils.isRooted()) {
            getPreferenceScreen().removePreference(findPreference("WifiHotspot"));
            PreferenceCategory bypassCategoryPref = (PreferenceCategory) findPreference("Bypass");
            bypassCategoryPref.removePreference(bypassCategoryPref.findPreference("TcpScramblerEnabled"));
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        PreferenceManager
                .getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);
        initGoAgent();
        initShadowsocks();
        initHttpProxy();
        initSshProxy();
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
        entries[servers.size()] = ">> Add";
        entries[servers.size() + 1] = ">> Batch Add";
        CharSequence[] entryValues = new CharSequence[servers.size() + 2];
        entryValues[servers.size()] = ">> Add";
        entryValues[servers.size() + 1] = ">> Batch Add";
        for (int i = 0; i < servers.size(); i++) {
            GoAgentSettingsActivity.Server server = servers.get(i);
            if (server.appid.equals("")) {
                entries[i] = "APPID NOT SET";
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
        entries[servers.size()] = ">> Add";
        CharSequence[] entryValues = new CharSequence[servers.size() + 1];
        entryValues[servers.size()] = ">> Add";
        for (int i = 0; i < servers.size(); i++) {
            ShadowsocksSettingsActivity.Server server = servers.get(i);
            if (server.host.equals("")) {
                entries[i] = "HOST NOT SET";
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
        entries[servers.size()] = ">> Add";
        CharSequence[] entryValues = new CharSequence[servers.size() + 1];
        entryValues[servers.size()] = ">> Add";
        for (int i = 0; i < servers.size(); i++) {
            HttpProxySettingsActivity.Server server = servers.get(i);
            if (server.host.equals("")) {
                entries[i] = "HOST NOT SET";
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
        entries[servers.size()] = ">> Add";
        CharSequence[] entryValues = new CharSequence[servers.size() + 1];
        entryValues[servers.size()] = ">> Add";
        for (int i = 0; i < servers.size(); i++) {
            SshSettingsActivity.Server server = servers.get(i);
            if (server.host.equals("")) {
                entries[i] = "HOST NOT SET";
            } else {
                entries[i] = server.host;
            }
            entryValues[i] = String.valueOf(i);
        }
        picker.setEntries(entries);
        picker.setEntryValues(entryValues);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.startsWith("WifiHotspot")) {
            showToast("You need to restart wifi hotspot to apply the changes");
        } else {
            showToast("You need to restart fqrouter to apply the changes");
        }
        LaunchService.updateConfigFile(this);
    }

    private void onGoAgentPrivateServerPicked(String value) {
        if (">> Add".equals(value)) {
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
        } else if (">> Batch Add".equals(value)) {
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

        alert.setTitle("Add one a time");
        alert.setMessage("AppId");
        final EditText input = new EditText(this);
        alert.setView(input);

        alert.setPositiveButton("Add more", new DialogInterface.OnClickListener() {
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

        alert.setNegativeButton("Done", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                addGoAgentPrivateServer(input.getText().toString());
            }
        });
        alert.show();
    }

    private void addGoAgentPrivateServer(final String value) {
        if (value.trim().isEmpty()) {
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
        if (">> Add".equals(value)) {
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
        if (">> Add".equals(value)) {
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
        if (">> Add".equals(value)) {
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
                .setTitle("P2P Agreement")
                .setMessage(P2P_AGREEMENT)
                .setPositiveButton("Yes", onAgreed)
                .setNegativeButton("No", null)
                .show();
    }

    private void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }
}
