package fq.router.wifi;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import fq.router.feedback.AppendLogIntent;
import fq.router.feedback.UpdateStatusIntent;

public class StartWifiHotspotService extends IntentService {
    public StartWifiHotspotService() {
        super("StartWifiHotspot");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        boolean isStarted = new WifiHotspotHelper(this).start();
        sendBroadcast(new WifiHotspotChangedIntent(isStarted));
    }

    private void appendLog(String log) {
        sendBroadcast(new AppendLogIntent(log));
    }

    public static void execute(Context context) {
        context.startService(new Intent(context, StartWifiHotspotService.class));
    }
}
