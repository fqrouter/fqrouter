package fq.router2;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import fq.router2.utils.IOUtils;
import fq.router2.utils.LogUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SshSettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private final static File SSH_CONFIG_FILE = new File("/data/data/fq.router2/etc/ssh.json");
    private int index;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        index = getIntent().getExtras().getInt("index");
        addPreferencesFromResource(R.xml.ssh);
        findPreference("SshDelete").setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        onDeleteClicked();
                        return true;
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        List<Server> servers = loadServers();
        Server server = loadServer(servers);
        if (server == null) {
            return;
        }
        ((EditTextPreference) findPreference("SshHost")).setText(server.host);
        ((EditTextPreference) findPreference("SshPort")).setText(Integer.toString(server.port));
        ((EditTextPreference) findPreference("SshUsername")).setText(server.username);
        ((EditTextPreference) findPreference("SshPassword")).setText(server.password);
        ((EditTextPreference) findPreference("SshConnectionsCount")).setText(Integer.toString(server.connectionsCount));
        updatePrivateKeyHint();
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

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        List<Server> servers = loadServers();
        Server server = loadServer(servers);
        if (server == null) {
            return;
        }
        server.host = ((EditTextPreference) findPreference("SshHost")).getText();
        server.port = Integer.valueOf(((EditTextPreference) findPreference("SshPort")).getText());
        server.username = ((EditTextPreference) findPreference("SshUsername")).getText();
        server.password = ((EditTextPreference) findPreference("SshPassword")).getText();
        server.connectionsCount = Integer.valueOf(
                ((EditTextPreference) findPreference("SshConnectionsCount")).getText());
        saveServers(servers);
        updatePrivateKeyHint();
    }

    private void updatePrivateKeyHint() {
        String sshHost = ((EditTextPreference) findPreference("SshHost")).getText();
        if (!sshHost.trim().isEmpty()) {
            findPreference("SshPassword").setSummary(
                    getResources().getString(R.string.pref_ssh_private_key_hint) +  " " +
                            "/data/data/fq.router2/etc/ssh/" + sshHost);
        }
    }

    private void onDeleteClicked() {
        LogUtils.i("delete clicked");
        List<Server> servers = loadServers();
        if (index < servers.size()) {
            servers.remove(index);
            saveServers(servers);
        }
        finish();
    }

    private Server loadServer(List<Server> servers) {
        if (index < servers.size()) {
            return servers.get(index);
        } else {
            return null;
        }
    }

    public static List<Server> loadServers() {
        String serversText = IOUtils.readFromFile(SSH_CONFIG_FILE);
        if (serversText.equals("")) {
            return new ArrayList<Server>();
        }
        try {
            ArrayList<Server> servers = new ArrayList<Server>();
            JSONArray serversJson = new JSONArray(serversText);
            for (int i = 0; i < serversJson.length(); i++) {
                final JSONObject serverJson = serversJson.getJSONObject(i);
                servers.add(new Server() {{
                    host = serverJson.getString("host");
                    port = Integer.valueOf(serverJson.getString("port"));
                    username = serverJson.getString("username");
                    password = serverJson.getString("password");
                    connectionsCount = Integer.valueOf(serverJson.getString("connections_count"));
                }});
            }
            return servers;
        } catch (Exception e) {
            LogUtils.e("failed to parse: " + serversText, e);
            return new ArrayList<Server>();
        }
    }

    public static void saveServers(List<Server> servers) {
        List<JSONObject> serverJsons = new ArrayList<JSONObject>();
        for (final Server server : servers) {
            serverJsons.add(new JSONObject(new HashMap() {{
                put("host", server.host);
                put("port", server.port);
                put("username", server.username);
                put("password", server.password);
                put("connections_count", server.connectionsCount);
            }}));
        }
        String serversText = new JSONArray(serverJsons).toString();
        LogUtils.i("save ssh servers: " + serversText);
        IOUtils.writeToFile(SSH_CONFIG_FILE, serversText);
    }

    public static class Server {
        public String host = "";
        public int port = 22;
        public String username = "";
        public String password = "";
        public int connectionsCount = 4;
    }
}
