package fq.router;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.*;
import fq.router.utils.IOUtils;
import fq.router.utils.LogUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ShadowsocksSettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private final static File SHADOWSOCKS_CONFIG_FILE = new File("/data/data/fq.router/etc/shadowsocks.json");
    private int index;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        index = getIntent().getExtras().getInt("index");
        addPreferencesFromResource(R.xml.shadowsocks);
        findPreference("ShadowsocksDelete").setOnPreferenceClickListener(
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
        PreferenceManager
                .getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);
        List<Server> servers = loadServers();
        Server server = loadServer(servers);
        if (server == null) {
            return;
        }
        ((EditTextPreference) findPreference("ShadowsocksHost")).setText(server.host);
        ((EditTextPreference) findPreference("ShadowsocksPort")).setText(server.port);
        ((EditTextPreference) findPreference("ShadowsocksPassword")).setText(server.password);
        ListPreference encryptionMethodPref = (ListPreference) findPreference("ShadowsocksEncryptionMethod");
        CharSequence[] encryptionMethods = new CharSequence[]{
                "table", "rc4", "aes-128-cfb", "aes-192-cfb", "aes-256-cfb", "bf-cfb",
                "camellia-128-cfb", "camellia-192-cfb", "camellia-256-cfb", "cast5-cfb",
                "des-cfb", "idea-cfb", "rc2-cfb", "seed-cfb"};
        encryptionMethodPref.setEntryValues(encryptionMethods);
        encryptionMethodPref.setEntries(encryptionMethods);
        encryptionMethodPref.setValue(server.encryption_method);

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
        server.host = ((EditTextPreference) findPreference("ShadowsocksHost")).getText();
        server.port = ((EditTextPreference) findPreference("ShadowsocksPort")).getText();
        server.password = ((EditTextPreference) findPreference("ShadowsocksPassword")).getText();
        server.encryption_method = ((ListPreference) findPreference("ShadowsocksEncryptionMethod")).getValue();
        saveServers(servers);
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
        String serversText = IOUtils.readFromFile(SHADOWSOCKS_CONFIG_FILE);
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
                    port = serverJson.getString("port");
                    password = serverJson.getString("password");
                    encryption_method = serverJson.getString("encryption_method");
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
                put("password", server.password);
                put("encryption_method", server.encryption_method);
            }}));
        }
        String serversText = new JSONArray(serverJsons).toString();
        LogUtils.i("save shadowsocks servers: " + serversText);
        IOUtils.writeToFile(SHADOWSOCKS_CONFIG_FILE, serversText);
    }

    public static class Server {
        public String host = "";
        public String port = "8889";
        public String password = "";
        public String encryption_method = "table";
    }
}
