package fq.router2.feedback;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.Toast;
import fq.router2.R;
import fq.router2.utils.IOUtils;
import fq.router2.utils.LogUtils;
import fq.router2.utils.LoggedBroadcastReceiver;
import fq.router2.utils.ShellUtils;

import java.io.File;

public class HandleAlertIntent extends Intent {
    public final static String ALERT_TYPE_HOSTS_MODIFIED = "HostsModified";
    public final static  String ALERT_TYPE_RUN_NEEDS_SU = "RunNeedsSu";
    public final static  String ALERT_TYPE_3G_APN = "3GAPN";
    private final static String ACTION_HANDLE_ALERT = "HandleAlert";

    public HandleAlertIntent(String alertType) {
        setAction(ACTION_HANDLE_ALERT);
        putExtra("alertType", alertType);
    }

    public static void register(final Handler handler) {
        Context context = handler.getBaseContext();
        context.registerReceiver(new LoggedBroadcastReceiver() {
            @Override
            protected void handle(Context context, Intent intent) {
                String alertType = intent.getStringExtra("alertType");
                if (ALERT_TYPE_HOSTS_MODIFIED.equals(alertType)) {
                    showHostsModifiedAlert(context);
                } else if (ALERT_TYPE_RUN_NEEDS_SU.equals(alertType)) {
                    showRunNeedsSuAlert(context);
                } else if (ALERT_TYPE_3G_APN.equals(alertType)) {
                    Toast.makeText(context, R.string.status_3g_apn_has_proxy, 8000).show();
                }
            }
        }, new IntentFilter(ACTION_HANDLE_ALERT));
    }

    private static void showHostsModifiedAlert(Context context) {
        final File ignoredFile = new File("/data/data/fq.router2/etc/hosts-modified-alert-ignored");
        if (ignoredFile.exists()) {
            return;
        }
        new AlertDialog.Builder(context)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.hosts_modified_alert_title)
                .setMessage(R.string.hosts_modified_alert_message)
                .setPositiveButton(R.string.hosts_modified_alert_revert, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    ShellUtils.sudo(ShellUtils.BUSYBOX_FILE + " mount -o remount,rw /system");
                                    ShellUtils.sudo(ShellUtils.BUSYBOX_FILE + " rm /system/etc/hosts");
                                    ShellUtils.sudo(ShellUtils.BUSYBOX_FILE + " mount -o remount,ro /system");
                                } catch (Exception e) {
                                    LogUtils.e("failed to delete hosts file", e);
                                }
                            }
                        }).start();
                    }
                })
                .setNegativeButton(R.string.hosts_modified_alert_ignore, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        IOUtils.writeToFile(ignoredFile, "OK");
                    }
                })
                .show();
    }

    private static void showRunNeedsSuAlert(Context context) {
        new AlertDialog.Builder(context)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.run_needs_su_alert_title)
                .setMessage(R.string.run_needs_su_alert_message)
                .setPositiveButton(R.string.run_needs_su_alert_ok, null)
                .show();
    }

    public static interface Handler {
        Context getBaseContext();
    }
}
