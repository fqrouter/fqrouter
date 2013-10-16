package fq.router2;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import fq.router2.utils.ConfigUtils;
import fq.router2.utils.HttpUtils;
import fq.router2.utils.LogUtils;

public class CheckDnsPollutionService extends IntentService {

    public CheckDnsPollutionService() {
        super("CheckDnsPollution");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            float dnsPollutedAt = Float.parseFloat(HttpUtils.get(
                    "http://127.0.0.1:" + ConfigUtils.getHttpManagerPort() + "/dns-polluted-at")) * 1000;
            if (dnsPollutedAt > 0) {
                sendBroadcast(new DnsPollutedIntent((long) dnsPollutedAt));
            }
        } catch (Exception e) {
            LogUtils.e("failed to check dns pollution", e);
        }
    }

    public static void execute(Context context) {
        context.startService(new Intent(context, CheckDnsPollutionService.class));
    }
}
