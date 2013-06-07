package fq.router;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import fq.router.utils.IOUtils;
import fq.router.utils.LogUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GoAgentSettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private final static File GOAGENT_JSON_FILE = new File("/data/data/fq.router/etc/goagent.json");
    private int index;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        index = getIntent().getExtras().getInt("index");
        addPreferencesFromResource(R.xml.goagent);
        findPreference("GoAgentDelete").setOnPreferenceClickListener(
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
        List<Server> servers = loadServers(this);
        Server server = loadServer(servers);
        if (server == null) {
            return;
        }
        ((EditTextPreference) findPreference("GoAgentAppId")).setText(server.appid);
        ((EditTextPreference) findPreference("GoAgentPath")).setText(server.path);
        ((EditTextPreference) findPreference("GoAgentPassword")).setText(server.password);

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
        List<Server> servers = loadServers(this);
        Server server = loadServer(servers);
        if (server == null) {
            return;
        }
        server.appid = ((EditTextPreference) findPreference("GoAgentAppId")).getText();
        server.path = ((EditTextPreference) findPreference("GoAgentPath")).getText();
        server.password = ((EditTextPreference) findPreference("GoAgentPassword")).getText();
        saveServers(this, servers);
    }

    private void onDeleteClicked() {
        LogUtils.i("delete clicked");
        List<Server> servers = loadServers(this);
        if (index < servers.size()) {
            servers.remove(index);
            saveServers(this, servers);
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

    public static List<Server> loadServers(Context context) {
        String serversText = IOUtils.readFromFile(GOAGENT_JSON_FILE);
        if (serversText.equals("")) {
            return new ArrayList<Server>();
        }
        try {
            ArrayList<Server> servers = new ArrayList<Server>();
            JSONArray serversJson = new JSONArray(serversText);
            for (int i = 0; i < serversJson.length(); i++) {
                final JSONObject serverJson = serversJson.getJSONObject(i);
                servers.add(new Server() {{
                    appid = serverJson.getString("appid");
                    path = serverJson.getString("path");
                    password = serverJson.getString("password");
                }});
            }
            return servers;
        } catch (Exception e) {
            LogUtils.e("failed to parse: " + serversText, e);
            return new ArrayList<Server>();
        }
    }

    public static void saveServers(Context context, List<Server> servers) {
        List<JSONObject> serverJsons = new ArrayList<JSONObject>();
        for (final Server server : servers) {
            serverJsons.add(new JSONObject(new HashMap() {{
                put("appid", server.appid);
                put("path", server.path);
                put("password", server.password);
            }}));
        }
        String serversText = new JSONArray(serverJsons).toString();
        LogUtils.i("save goagent servers: " + serversText);
        IOUtils.writeToFile(GOAGENT_JSON_FILE, serversText);
    }

    public static class Server {
        public String appid = "";
        public String path = "/2";
        public String password = "";
    }
}
