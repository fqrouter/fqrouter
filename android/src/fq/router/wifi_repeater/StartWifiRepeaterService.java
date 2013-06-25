package fq.router.wifi_repeater;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import fq.router.feedback.HandleFatalErrorIntent;

public class StartWifiRepeaterService extends IntentService {
    public StartWifiRepeaterService() {
        super("StartWifiRepeater");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            new WifiRepeater(this).start();
            sendBroadcast(new WifiRepeaterChangedIntent(true));
        } catch (HandleFatalErrorIntent.Message e) {
            sendBroadcast(new HandleFatalErrorIntent(e.getMessage()));
        }
    }

    public static void execute(Context context) {
        context.startService(new Intent(context, StartWifiRepeaterService.class));
    }
}
