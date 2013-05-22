package fq.router.life;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import fq.router.feedback.UpdateStatusIntent;
import fq.router.utils.LogUtils;
import fq.router.wifi.WifiHotspotHelper;

public class ExitService extends IntentService {

    public ExitService() {
        super("Exit");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        boolean shouldStopWifiHotspot = intent.getBooleanExtra("shouldStopWifiHotspot", false);
        exit(shouldStopWifiHotspot);
    }

    private void exit(boolean shouldStopWifiHotspot) {
        if (shouldStopWifiHotspot) {
            new WifiHotspotHelper(this).stop();
        }
        updateStatus("Exiting...");
        try {
            ManagerProcess.kill();
        } catch (Exception e) {
            LogUtils.e("failed to kill manager process", e);
        }
        sendBroadcast(new ExitedIntent());
    }

    private void updateStatus(String status) {
        sendBroadcast(new UpdateStatusIntent(status));
    }

    public static void execute(Context context, boolean shouldStopWifiHotspot) {
        Intent intent = new Intent(context, ExitService.class);
        intent.putExtra("shouldStopWifiHotspot", shouldStopWifiHotspot);
        context.startService(intent);
    }
}
