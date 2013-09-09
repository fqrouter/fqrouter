package fq.router2.free_internet;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import fq.router2.life_cycle.LaunchService;
import fq.router2.utils.HttpUtils;
import fq.router2.utils.LogUtils;

public class CheckFreeInternetService extends IntentService {
    public CheckFreeInternetService() {
        super("CheckFreeInternet");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            boolean connectedInVpnMode = LaunchService.ping(this, true)
                    && LaunchService.isVpnRunning()
                    && "TRUE".equals(HttpUtils.get("http://127.0.0.1:8318/free-internet/is-connected"));
            boolean connectedInRootMode = LaunchService.ping(this, false)
                    && "TRUE".equals(HttpUtils.get("http://127.0.0.1:8318/free-internet/is-connected"));
            sendBroadcast(new FreeInternetChangedIntent(connectedInRootMode || connectedInVpnMode));
        } catch (Exception e) {
            LogUtils.e("failed to check free internet", e);
            sendBroadcast(new FreeInternetChangedIntent(false));
        }
    }

    public static void execute(Context context) {
        context.startService(new Intent(context, CheckFreeInternetService.class));
    }
}
