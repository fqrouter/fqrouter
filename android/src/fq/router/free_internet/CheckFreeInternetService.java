package fq.router.free_internet;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import fq.router.life_cycle.LaunchService;
import fq.router.utils.HttpUtils;
import fq.router.utils.LogUtils;

public class CheckFreeInternetService extends IntentService {
    public CheckFreeInternetService() {
        super("CheckFreeInternet");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            boolean connectedInVpnMode = LaunchService.ping(true) &&
                    LaunchService.isVpnRunning();
            boolean connectedInRootMode = LaunchService.ping(false) &&
                    "TRUE".equals(HttpUtils.get("http://127.0.0.1:8318/free-internet/is-connected"));
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
