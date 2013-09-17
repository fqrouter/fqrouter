package fq.router2.free_internet;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import fq.router2.feedback.HandleFatalErrorIntent;
import fq.router2.utils.HttpUtils;
import fq.router2.utils.LogUtils;

public class ConnectFreeInternetService extends IntentService {
    public ConnectFreeInternetService() {
        super("ConnectFreeInternet");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        connect(this);
    }

    public static void connect(Context context) {
        try {
            HttpUtils.post("http://127.0.0.1:2515/free-internet/connect");
            context.sendBroadcast(new FreeInternetChangedIntent(true));
        } catch (Exception e) {
            context.sendBroadcast(new HandleFatalErrorIntent(LogUtils.e("failed to connect to free internet", e)));
        }
    }

    public static void execute(Context context) {
        context.startService(new Intent(context, ConnectFreeInternetService.class));
    }
}
