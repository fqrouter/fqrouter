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

public class HttpProxySettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private final static File HTTP_PROXY_CONFIG_FILE = new File("/data/data/fq.router/etc/http-proxy.json");
    private int index;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        index = getIntent().getExtras().getInt("index");
        addPreferencesFromResource(R.xml.http_proxy);
        findPreference("HttpProxyDelete").setOnPreferenceClickListener(
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
        ((EditTextPreference) findPreference("HttpProxyHost")).setText(server.host);
        ((EditTextPreference) findPreference("HttpProxyPort")).setText(server.port);
        ((EditTextPreference) findPreference("HttpProxyUsername")).setText(server.username);
        ((EditTextPreference) findPreference("HttpProxyPassword")).setText(server.password);
        ListPreference trafficTypePerf = (ListPreference) findPreference("HttpProxyTrafficType");
        CharSequence[] trafficTypes = new CharSequence[]{"http only", "https only", "http/https"};
        trafficTypePerf.setEntryValues(trafficTypes);
        trafficTypePerf.setEntries(trafficTypes);
        trafficTypePerf.setValue(server.traffic_type);
        ListPreference transportTypePerf = (ListPreference) findPreference("HttpProxyTransportType");
        CharSequence[] transportType = new CharSequence[]{"plain", "ssl"};
        transportTypePerf.setEntryValues(transportType);
        transportTypePerf.setEntries(transportType);
        transportTypePerf.setValue(server.transport_type);

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
        server.host = ((EditTextPreference) findPreference("HttpProxyHost")).getText();
        server.port = ((EditTextPreference) findPreference("HttpProxyPort")).getText();
        server.username = ((EditTextPreference) findPreference("HttpProxyUsername")).getText();
        server.password = ((EditTextPreference) findPreference("HttpProxyPassword")).getText();
        server.traffic_type = ((ListPreference) findPreference("HttpProxyTrafficType")).getValue();
        server.transport_type = ((ListPreference) findPreference("HttpProxyTransportType")).getValue();
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
        String serversText = IOUtils.readFromFile(HTTP_PROXY_CONFIG_FILE);
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
                    username = serverJson.getString("username");
                    password = serverJson.getString("password");
                    traffic_type = serverJson.getString("traffic_type");
                    transport_type = serverJson.getString("transport_type");
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
                put("traffic_type", server.traffic_type);
                put("transport_type", server.transport_type);
            }}));
        }
        String serversText = new JSONArray(serverJsons).toString();
        LogUtils.i("save http proxy servers: " + serversText);
        IOUtils.writeToFile(HTTP_PROXY_CONFIG_FILE, serversText);
    }

    public static class Server {
        public String host = "";
        public String port = "8889";
        public String username = "";
        public String password = "";
        public String traffic_type = "http/https";
        public String transport_type = "plain";
    }
}
