package fq.router2;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.widget.Toast;
import com.google.analytics.tracking.android.EasyTracker;
import fq.router2.life_cycle.LaunchService;
import fq.router2.utils.ShellUtils;

public class AdvancedSettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.advanced);
        if (!ShellUtils.isRooted()) {
            PreferenceCategory bypassCategoryPref = (PreferenceCategory) findPreference("Bypass");
            bypassCategoryPref.removePreference(bypassCategoryPref.findPreference("TcpScramblerEnabled"));
        }
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

    @Override
    protected void onResume() {
        super.onResume();
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
        showToast(_(R.string.pref_restart_app));
        LaunchService.updateConfigFile(this);
    }

    private String _(int id) {
        return getResources().getString(id);
    }

    private void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }
}
