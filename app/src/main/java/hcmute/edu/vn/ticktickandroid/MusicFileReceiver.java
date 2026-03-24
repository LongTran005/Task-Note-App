package hcmute.edu.vn.ticktickandroid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import hcmute.edu.vn.ticktickandroid.Service.MusicService;

public class MusicFileReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent serviceIntent = new Intent(context, MusicService.class);
        serviceIntent.setAction(intent.getAction());
        serviceIntent.putExtras(intent);
        context.startService(serviceIntent);
    }
}
