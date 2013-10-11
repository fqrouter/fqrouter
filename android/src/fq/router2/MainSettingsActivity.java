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
        findPreference("OpenFullGooglePlay").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                openFullGooglePlay();
                return false;
            }
        });
        findPreference("OpenAbout").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                openAbout();
                return false;
            }
        });
        if (!ShellUtils.isRooted()) {
            getPreferenceScreen().removePreference(findPreference("AutoLaunchEnabled"));
            getPreferenceScreen().removePreference(findPreference("NotificationEnabled"));
        }
    }

    private void openAbout() {
        WebView web = new WebView(this);
        web.loadUrl("file:///android_asset/pages/about.html");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                return true;
            }
        });
        new AlertDialog.Builder(this)
                .setTitle(String.format(_(R.string.about_info_title), LaunchService.getMyVersion(this)))
                .setCancelable(false)
                .setNegativeButton(R.string.about_info_close, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                })
                .setView(web)
                .create()
                .show();
    }

    private void openFullGooglePlay() {
        if (ShellUtils.isRooted()) {
            try {
                ShellUtils.sudo("/data/data/fq.router2/busybox", "killall", "com.android.vending");
            } catch (Exception e) {
                LogUtils.e("failed to stop google play", e);
            }
            try {
                ShellUtils.sudo("/data/data/fq.router2/busybox", "rm", "-rf", "/data/data/com.android.vending/*");
            } catch (Exception e) {
                LogUtils.e("failed to clear google play data", e);
                showToast(R.string.failed_to_open_google_play);
                return;
            }
            openGooglePlay();
        } else {
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
    }

    private void openGooglePlay() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    HttpUtils.post("http://127.0.0.1:2515/force-us-ip");
                    Intent LaunchIntent = getPackageManager().getLaunchIntentForPackage("com.android.vending");
                    startActivity(LaunchIntent);
                } catch (Exception e) {
                    LogUtils.e("failed to open google play", e);
                    showToast(R.string.failed_to_open_google_play);
                }
            }
        }).start();
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
