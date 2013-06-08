package fq.router;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import fq.router.feedback.*;
import fq.router.life.DownloadFailedIntent;
import fq.router.life.DownloadService;
import fq.router.life.DownloadedIntent;
import fq.router.life.DownloadingIntent;
import fq.router.utils.IOUtils;
import fq.router.utils.LogUtils;

import java.lang.reflect.Method;

public class InstallerActivity extends Activity implements
        UpdateStatusIntent.Handler,
        AppendLogIntent.Handler,
        UpdateFoundIntent.Handler,
        DownloadingIntent.Handler,
        DownloadedIntent.Handler,
        DownloadFailedIntent.Handler,
        HandleFatalErrorIntent.Handler {


    public final static int SHOW_AS_ACTION_IF_ROOM = 1;
    private final static int ITEM_ID_EXIT = 1;
    private final static int ITEM_ID_REPORT_ERROR = 2;
    private String upgradeUrl;
    private boolean downloaded;

    static {
        IOUtils.createCommonDirs();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.installer);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, true);
        TextView textView = (TextView) findViewById(R.id.logTextView);
        textView.setMovementMethod(new ScrollingMovementMethod());
        checkUpdate();
        UpdateStatusIntent.register(this);
        AppendLogIntent.register(this);
        UpdateFoundIntent.register(this);
        DownloadingIntent.register(this);
        DownloadedIntent.register(this);
        DownloadFailedIntent.register(this);
        HandleFatalErrorIntent.register(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        addMenuItem(menu, ITEM_ID_REPORT_ERROR, "Report Error");
        addMenuItem(menu, ITEM_ID_EXIT, "Exit");
        return super.onCreateOptionsMenu(menu);
    }

    private MenuItem addMenuItem(Menu menu, int menuItemId, String caption) {
        MenuItem menuItem = menu.add(Menu.NONE, menuItemId, Menu.NONE, caption);
        try {
            Method method = MenuItem.class.getMethod("setShowAsAction", int.class);
            try {
                method.invoke(menuItem, SHOW_AS_ACTION_IF_ROOM);
            } catch (Exception e) {
            }
        } catch (NoSuchMethodException e) {
        }
        return menuItem;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (ITEM_ID_EXIT == item.getItemId()) {
            exit();
        } else if (ITEM_ID_REPORT_ERROR == item.getItemId()) {
            reportError();
        }
        return super.onMenuItemSelected(featureId, item);
    }

    private void exit() {
        finish();
    }

    private void reportError() {
        try {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle("Hey, dude")
                    .setMessage("This is just a installer, you need to wait for " +
                            "the download completed then install the downloaded apk file. Get it?")
                    .setPositiveButton("No", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startActivity(Intent.createChooser(
                                    new ErrorReportEmail(InstallerActivity.this).prepare(), "Send mail..."));
                        }

                    })
                    .setNegativeButton("Got It", null)
                    .show();
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkUpdate() {
        if (upgradeUrl == null) {
            CheckUpdateService.execute(this);
        }
    }

    @Override
    public void updateStatus(String status) {
        appendLog("status updated to: " + status);
        TextView textView = (TextView) findViewById(R.id.statusTextView);
        textView.setText(status);
    }

    @Override
    public void appendLog(final String log) {
        LogUtils.i(log);
        TextView textView = (TextView) findViewById(R.id.logTextView);
        textView.setText(log + "\n" + textView.getText());
    }

    @Override
    public void onDownloadFailed(final String url, String downloadTo) {
        ActivityCompat.invalidateOptionsMenu(this);
        onHandleFatalError("download " + Uri.parse(url).getLastPathSegment() + " failed");
    }

    @Override
    public void onUpdateFound(String latestVersion, final String upgradeUrl) {
        final String downloadTo = "/sdcard/fqrouter-latest.apk";
        if (downloaded) {
            onDownloaded(upgradeUrl, downloadTo);
            return;
        }
        this.upgradeUrl = upgradeUrl;
        ActivityCompat.invalidateOptionsMenu(this);
        updateStatus("Downloading " + Uri.parse(upgradeUrl).getLastPathSegment());
        DownloadService.execute(
                this, upgradeUrl, downloadTo);
    }

    @Override
    public void onDownloaded(String url, String downloadTo) {
        downloaded = true;
        updateStatus("Downloaded " + Uri.parse(url).getLastPathSegment());
        CheckUpdateService.installApk(this, downloadTo);
    }

    @Override
    public void onDownloading(String url, String downloadTo, int percent) {
        TextView textView = (TextView) findViewById(R.id.statusTextView);
        textView.setText("Downloaded: " + percent + "%");
    }

    @Override
    public void onHandleFatalError(String message) {
        updateStatus("Error: " + message);
        checkUpdate();
    }
}
