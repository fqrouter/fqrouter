package fq.router2.wifi_repeater;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

public class StopWifiRepeaterService extends IntentService {
    public StopWifiRepeaterService() {
        super("StopWifiRepeater");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        new WifiRepeater(this).stop();
        sendBroadcast(new WifiRepeaterChangedIntent(false));
    }

    public static void execute(Context context) {
        context.startService(new Intent(context, StopWifiRepeaterService.class));
    }
}
