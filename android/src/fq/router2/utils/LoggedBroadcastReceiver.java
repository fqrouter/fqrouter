package fq.router2.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public abstract class LoggedBroadcastReceiver extends BroadcastReceiver {
    @Override
    public final void onReceive(Context context, Intent intent) {
        try {
            handle(context, intent);
        } catch (Throwable e) {
            LogUtils.e("failed to handle", e);
        }
    }

    protected abstract void handle(Context context, Intent intent);
}
