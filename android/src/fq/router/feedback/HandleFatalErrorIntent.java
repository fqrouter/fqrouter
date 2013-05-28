package fq.router.feedback;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import fq.router.utils.LogUtils;
import fq.router.utils.LoggedBroadcastReceiver;

public class HandleFatalErrorIntent extends Intent {
    private final static String ACTION_HANDLE_FATAL_ERROR = "HandleFatalError";

    public HandleFatalErrorIntent(String message, Exception e) {
        if (null == e) {
            LogUtils.e(message);
        } else {
            LogUtils.e(message, e);
        }
        setAction(ACTION_HANDLE_FATAL_ERROR);
        putExtra("message", message);
    }

    public static void register(final Handler handler) {
        handler.getBaseContext().registerReceiver(new LoggedBroadcastReceiver() {
            @Override
            protected void handle(Context context, Intent intent) {
                handler.onHandleFatalError(intent.getStringExtra("message"));
            }
        }, new IntentFilter(ACTION_HANDLE_FATAL_ERROR));
    }

    public static interface Handler {
        void onHandleFatalError(String message);

        Context getBaseContext();
    }
}
