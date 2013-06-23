package fq.router.free_internet;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import fq.router.utils.HttpUtils;
import fq.router.utils.LogUtils;

public class ConnectFreeInternetService extends IntentService {
    public ConnectFreeInternetService() {
        super("ConnectFreeInternet");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            HttpUtils.post("http://127.0.0.1:8318/free-internet/connect");
            sendBroadcast(new FreeInternetChangedIntent(true));
        } catch (Exception e) {
            LogUtils.e("failed to connect to free internet", e);
            sendBroadcast(new FreeInternetChangedIntent(false));
        }
    }

    public static void execute(Context context) {
        context.startService(new Intent(context, ConnectFreeInternetService.class));
    }
}
