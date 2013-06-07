package fq.router;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.widget.Toast;
import fq.router.life.LaunchService;

import java.util.List;
import java.util.Map;

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
    }

    private void initGoAgent() {
        final ListPreference picker = (ListPreference) findPreference("GoAgentPrivateServersPicker");
        List<GoAgentSettingsActivity.Server> servers = GoAgentSettingsActivity.loadServers(this);
        CharSequence[] entries = new CharSequence[servers.size() + 1];
        entries[servers.size()] = ">> Add";
        CharSequence[] entryValues = new CharSequence[servers.size() + 1];
        entryValues[servers.size()] = ">> Add";
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


    @Override
    protected void onResume() {
        super.onResume();
        PreferenceManager
                .getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);
        initGoAgent();
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
        if (key.startsWith("WifiHotspot")) {
            showToast("You need to restart wifi hotspot to apply the changes");
        } else {
            showToast("You need to restart fqrouter to apply the changes");
        }
        Map<String, ?> settings = sharedPreferences.getAll();
        LaunchService.updateConfigFile(settings);
    }

    private void onGoAgentPrivateServerPicked(String value) {
        if (">> Add".equals(value)) {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle("P2P Agreement")
                    .setMessage(P2P_AGREEMENT)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(MainSettingsActivity.this, GoAgentSettingsActivity.class);
                            List<GoAgentSettingsActivity.Server> servers = GoAgentSettingsActivity.loadServers(MainSettingsActivity.this);
                            servers.add(new GoAgentSettingsActivity.Server());
                            GoAgentSettingsActivity.saveServers(MainSettingsActivity.this, servers);
                            intent.putExtra("index", servers.size() - 1);
                            startActivity(intent);
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();
        } else {
            Intent intent = new Intent(this, GoAgentSettingsActivity.class);
            intent.putExtra("index", Integer.valueOf(value));
            startActivity(intent);
        }
    }

    private void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }
}
