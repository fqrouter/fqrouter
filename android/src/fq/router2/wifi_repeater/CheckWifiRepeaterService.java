package fq.router2.wifi_repeater;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

public class CheckWifiRepeaterService extends IntentService {
    public CheckWifiRepeaterService() {
        super("CheckWifiRepeater");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        sendBroadcast(new WifiRepeaterChangedIntent(new WifiRepeater(this).isStarted()));
    }

    public static void execute(Context context) {
        context.startService(new Intent(context, CheckWifiRepeaterService.class));
    }
}
