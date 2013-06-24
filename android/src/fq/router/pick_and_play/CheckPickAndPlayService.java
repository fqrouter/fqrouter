package fq.router.pick_and_play;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import fq.router.utils.HttpUtils;
import fq.router.utils.LogUtils;

public class CheckPickAndPlayService extends IntentService {
    public CheckPickAndPlayService() {
        super("CheckPickAndPlay");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        sendBroadcast(new PickAndPlayChangedIntent(isStarted()));
    }

    private static boolean isStarted() {
        try {
            return "TRUE".equals(HttpUtils.get("http://127.0.0.1:8318/pick-and-play/is-started"));
        } catch (Exception e) {
            LogUtils.e("failed to check pick & play is started", e);
            return false;
        }
    }

    public static void execute(Context context) {
        context.startService(new Intent(context, CheckPickAndPlayService.class));
    }
}
