package fq.router.life;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import fq.router.feedback.UpdateStatusIntent;
import fq.router.utils.LogUtils;

public class ExitService extends IntentService {

    public ExitService() {
        super("Exit");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        exit();
    }

    private void exit() {
        updateStatus("Exiting...");
        try {
            ManagerProcess.kill();
        } catch (Exception e) {
            LogUtils.e("failed to kill manager process", e);
        }
        sendBroadcast(new ExitedIntent());
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(0);
    }

    private void updateStatus(String status) {
        sendBroadcast(new UpdateStatusIntent(status));
    }

    public static void execute(Context context) {
        context.startService(new Intent(context, ExitService.class));
    }
}
