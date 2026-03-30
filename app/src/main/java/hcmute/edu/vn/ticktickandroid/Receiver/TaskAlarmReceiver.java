package hcmute.edu.vn.ticktickandroid.Receiver;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import hcmute.edu.vn.ticktickandroid.R;

public class TaskAlarmReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "ReminderAlarmChannel";

    @Override
    public void onReceive(Context context, Intent intent) {
        String title = intent.getStringExtra("reminder_title");
        int reminderId = intent.getIntExtra("reminder_id", 0);

        if (title == null || title.isEmpty()) {
            title = "Nhắc nhở";
        }

        createNotificationChannel(context);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_on)
                .setContentTitle("⏰ Nhắc nhở")
                .setContentText(title)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(reminderId, builder.build());
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Nhắc nhở lịch",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Thông báo nhắc nhở lịch hẹn");
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
