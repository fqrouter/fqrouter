package fq.router2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.ImageView;
import fq.router2.life_cycle.ExitService;
import fq.router2.life_cycle.ExitedIntent;
import fq.router2.life_cycle.ExitingIntent;

public class ExitReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        ExitService.execute(context);
        MainActivity.displayNotification(context, context.getResources().getString(R.string.status_exiting));
        context.sendBroadcast(new ExitingIntent());
    }
}
