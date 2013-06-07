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
        String mode = intent.getStringExtra("mode");
        appendLog("wifi hotspot mode: " + mode);
        boolean isStarted = new WifiHotspotHelper(this).start(mode);
        sendBroadcast(new WifiHotspotChangedIntent(isStarted, WifiHotspotHelper.MODE_WIFI_REPEATER.equals(mode)));
    }

    private void appendLog(String log) {
        sendBroadcast(new AppendLogIntent(log));
    }

    public static void execute(Context context, String mode) {
        Intent intent = new Intent(context, StartWifiHotspotService.class);
        intent.putExtra("mode", mode);
        context.startService(intent);
    }
}
