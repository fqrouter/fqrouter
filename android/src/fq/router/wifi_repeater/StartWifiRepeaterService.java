package fq.router.wifi_repeater;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

public class StartWifiRepeaterService extends IntentService {
    public StartWifiRepeaterService() {
        super("StartWifiRepeater");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        boolean isStarted = new WifiRepeater(this).start();
        sendBroadcast(new WifiRepeaterChangedIntent(isStarted));
    }

    public static void execute(Context context) {
        context.startService(new Intent(context, StartWifiRepeaterService.class));
    }
}
