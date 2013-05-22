package fq.router.wifi;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

public class StopWifiHotspotService extends IntentService {
    public StopWifiHotspotService() {
        super("StopWifiHotspot");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        new WifiHotspotHelper(this).stop();
        sendBroadcast(new WifiHotspotChangedIntent(false));
    }

    public static void execute(Context context) {
        context.startService(new Intent(context, StopWifiHotspotService.class));
    }
}
