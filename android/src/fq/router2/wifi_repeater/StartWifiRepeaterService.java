package fq.router2.wifi_repeater;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.analytics.tracking.android.Tracker;
import fq.router2.feedback.HandleFatalErrorIntent;

public class StartWifiRepeaterService extends IntentService {
    public StartWifiRepeaterService() {
        super("StartWifiRepeater");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        GoogleAnalytics gaInstance = GoogleAnalytics.getInstance(this);
        Tracker gaTracker = gaInstance.getTracker("UA-37740383-2");
        gaTracker.setCustomDimension(1, Build.MODEL);
        try {
            gaTracker.sendEvent("wifi-repeater", "start", "", new Long(0));
            new WifiRepeater(this).start();
            sendBroadcast(new WifiRepeaterChangedIntent(true));
            gaTracker.sendEvent("wifi-repeater", "success", "", new Long(0));
        } catch (HandleFatalErrorIntent.Message e) {
            sendBroadcast(new HandleFatalErrorIntent(e.getMessage()));
            gaTracker.sendEvent("wifi-repeater", "failure", "", new Long(0));
        }
    }

    public static void execute(Context context) {
        context.startService(new Intent(context, StartWifiRepeaterService.class));
    }
}
