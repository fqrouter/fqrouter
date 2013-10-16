package fq.router2;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.*;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.Toast;
import com.google.analytics.tracking.android.EasyTracker;
import fq.router2.life_cycle.LaunchService;
import fq.router2.utils.ApkUtils;
import fq.router2.utils.HttpUtils;
import fq.router2.utils.LogUtils;
import fq.router2.utils.ShellUtils;

import java.util.List;

public class MainSettingsActivity extends PreferenceActivity {

    private Handler handler = new Handler();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        if (!ShellUtils.isRooted()) {
            getPreferenceScreen().removePreference(findPreference("AutoLaunchEnabled"));
            getPreferenceScreen().removePreference(findPreference("NotificationEnabled"));
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

    private String _(int id) {
        return getResources().getString(id);
    }
}
