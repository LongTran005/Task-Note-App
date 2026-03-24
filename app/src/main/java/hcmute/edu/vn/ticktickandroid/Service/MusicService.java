package hcmute.edu.vn.ticktickandroid.Service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import hcmute.edu.vn.ticktickandroid.MainActivity;
import hcmute.edu.vn.ticktickandroid.R;

public class MusicService extends Service {

    public static final String ACTION_PLAY = "hcmute.edu.vn.ticktickandroid.ACTION_PLAY";
    public static final String ACTION_PAUSE = "hcmute.edu.vn.ticktickandroid.ACTION_PAUSE";
    public static final String ACTION_RESUME = "hcmute.edu.vn.ticktickandroid.ACTION_RESUME";
    public static final String ACTION_STOP = "hcmute.edu.vn.ticktickandroid.ACTION_STOP";
    public static final String ACTION_STATE_CHANGED = "hcmute.edu.vn.ticktickandroid.ACTION_STATE_CHANGED";

    public static final String EXTRA_RES_ID = "extra_res_id";
    public static final String EXTRA_URI = "extra_uri";
    public static final String EXTRA_MUSIC_NAME = "extra_music_name";
    public static final String EXTRA_STATE = "extra_state";

    public static final int STATE_IDLE = 0;
    public static final int STATE_PLAYING = 1;
    public static final int STATE_PAUSED = 2;

    private static final String CHANNEL_ID = "music_channel";
    private static final int NOTIFICATION_ID = 2001;

    private MediaPlayer mediaPlayer;
    private int currentState = STATE_IDLE;
    private String currentMusicName = "";
    private int currentResId = -1;

    private final IBinder binder = new MusicBinder();

    public class MusicBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    private final BroadcastReceiver musicReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;

            switch (action) {
                case ACTION_PLAY:
                    int resId = intent.getIntExtra(EXTRA_RES_ID, -1);
                    String uriString = intent.getStringExtra(EXTRA_URI);
                    String name = intent.getStringExtra(EXTRA_MUSIC_NAME);

                    if (uriString != null) {
                        playMusicFromUri(Uri.parse(uriString), name);
                    } else if (resId != -1) {
                        playMusic(resId, name);
                    }
                    break;
                case ACTION_PAUSE:
                    pauseMusic();
                    break;
                case ACTION_RESUME:
                    resumeMusic();
                    break;
                case ACTION_STOP:
                    stopMusic();
                    break;
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_PLAY);
        filter.addAction(ACTION_PAUSE);
        filter.addAction(ACTION_RESUME);
        filter.addAction(ACTION_STOP);

        // Sử dụng RECEIVER_NOT_EXPORTED để tuân thủ quy định của Android 14+ cho các broadcast nội bộ
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(musicReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(musicReceiver, filter);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Nhạc nền",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Điều khiển nhạc nền");
            channel.setSound(null, null);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    public void playMusic(int resId, String name) {
        stopMusicInternal();
        currentResId = resId;
        mediaPlayer = MediaPlayer.create(this, resId);
        currentMusicName = name != null ? name : "Unknown";
        startMediaPlayer();
    }

    public void playMusicFromUri(Uri uri, String name) {
        stopMusicInternal();
        currentResId = -1;
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(this, uri);
            mediaPlayer.prepare();
            currentMusicName = name != null ? name : "Unknown";
            startMediaPlayer();
        } catch (Exception e) {
            Toast.makeText(this, "Không thể phát file này", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void startMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.setLooping(true);
            mediaPlayer.start();
            currentState = STATE_PLAYING;
            broadcastState();
            showNotification();
        }
    }

    public void pauseMusic() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            currentState = STATE_PAUSED;
            broadcastState();
            showNotification();
        }
    }

    public void resumeMusic() {
        if (mediaPlayer != null && currentState == STATE_PAUSED) {
            mediaPlayer.start();
            currentState = STATE_PLAYING;
            broadcastState();
            showNotification();
        }
    }

    private void stopMusicInternal() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    public void stopMusic() {
        stopMusicInternal();
        currentState = STATE_IDLE;
        currentMusicName = "";
        currentResId = -1;
        broadcastState();
        stopForeground(true);
    }

    private void broadcastState() {
        Intent intent = new Intent(ACTION_STATE_CHANGED);
        intent.setPackage(getPackageName());
        intent.putExtra(EXTRA_STATE, currentState);
        intent.putExtra(EXTRA_MUSIC_NAME, currentMusicName);
        intent.putExtra(EXTRA_RES_ID, currentResId);
        sendBroadcast(intent);
    }

    private void showNotification() {
        Intent openIntent = new Intent(this, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent openPending = PendingIntent.getActivity(
                this, 0, openIntent, PendingIntent.FLAG_IMMUTABLE);

        // Pause/Resume action
        Intent pauseResumeIntent = new Intent(currentState == STATE_PLAYING ? ACTION_PAUSE : ACTION_RESUME);
        pauseResumeIntent.setPackage(getPackageName());
        PendingIntent pauseResumePending = PendingIntent.getBroadcast(
                this, 1, pauseResumeIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        // Stop action
        Intent stopIntent = new Intent(ACTION_STOP);
        stopIntent.setPackage(getPackageName());
        PendingIntent stopPending = PendingIntent.getBroadcast(
                this, 2, stopIntent, PendingIntent.FLAG_IMMUTABLE);

        int pauseResumeIcon = currentState == STATE_PLAYING ? R.drawable.ic_pause : R.drawable.ic_play;
        String pauseResumeTitle = currentState == STATE_PLAYING ? "Tạm dừng" : "Tiếp tục";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_music_note)
                .setContentTitle("Nhạc nền")
                .setContentText(currentMusicName)
                .setContentIntent(openPending)
                .setOngoing(true)
                .setSilent(true)
                .addAction(pauseResumeIcon, pauseResumeTitle, pauseResumePending)
                .addAction(R.drawable.ic_stop, "Dừng", stopPending)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0, 1));

        Notification notification = builder.build();

        try {
            startForeground(NOTIFICATION_ID, notification);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Public getters for binding
    public int getCurrentState() {
        return currentState;
    }

    public String getCurrentMusicName() {
        return currentMusicName;
    }

    public int getCurrentResId() {
        return currentResId;
    }

    @Override
    public void onDestroy() {
        stopMusicInternal();
        try {
            unregisterReceiver(musicReceiver);
        } catch (Exception ignored) {}
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}