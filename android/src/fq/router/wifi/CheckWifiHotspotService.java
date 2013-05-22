package fq.router.wifi;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

public class CheckWifiHotspotService extends IntentService {
    public CheckWifiHotspotService() {
        super("CheckWifiHotspot");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        WifiHotspotHelper wifiHotspotHelper = new WifiHotspotHelper(this);
        boolean isStarted = wifiHotspotHelper.isStarted();
        if (isStarted) {
            wifiHotspotHelper.setup();
        }
        sendBroadcast(new WifiHotspotChangedIntent(isStarted));
    }

    public static void execute(Context context) {
        context.startService(new Intent(context, CheckWifiHotspotService.class));
    }
}
