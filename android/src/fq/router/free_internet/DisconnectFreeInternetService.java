package fq.router.free_internet;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import fq.router.feedback.HandleFatalErrorIntent;
import fq.router.utils.HttpUtils;
import fq.router.utils.LogUtils;

public class DisconnectFreeInternetService extends IntentService {
    public DisconnectFreeInternetService() {
        super("DisconnectFreeInternet");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            HttpUtils.post("http://127.0.0.1:8318/free-internet/disconnect");
            sendBroadcast(new FreeInternetChangedIntent(false));
        } catch (Exception e) {
            sendBroadcast(new HandleFatalErrorIntent(LogUtils.e("failed to disconnect from free internet", e)));
        }
    }

    public static void execute(Context context) {
        context.startService(new Intent(context, DisconnectFreeInternetService.class));
    }
}
