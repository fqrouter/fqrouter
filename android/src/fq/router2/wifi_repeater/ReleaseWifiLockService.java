package fq.router2.wifi_repeater;

import android.app.IntentService;
import android.content.Intent;

public class ReleaseWifiLockService extends IntentService {
    public ReleaseWifiLockService() {
        super("ReleaseWifiLockService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        stopService(new Intent(this, AcquireWifiLockService.class));
    }
}
