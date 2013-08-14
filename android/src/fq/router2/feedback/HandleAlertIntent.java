package fq.router2.feedback;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import fq.router2.R;
import fq.router2.utils.LoggedBroadcastReceiver;

public class HandleAlertIntent extends Intent {
    public final static String ALERT_TYPE_ABNORMAL_EXIT = "AbnormalExit";
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
                if (ALERT_TYPE_ABNORMAL_EXIT.equals(alertType)) {
                    new AlertDialog.Builder(context)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setTitle(R.string.abnormal_exit_alert_title)
                            .setMessage(R.string.abnormal_exit_alert_message)
                            .setPositiveButton(R.string.abnormal_exit_alert_ok, null)
                            .show();
                }
            }
        }, new IntentFilter(ACTION_HANDLE_ALERT));
    }

    public static interface Handler {
        Context getBaseContext();
    }
}
