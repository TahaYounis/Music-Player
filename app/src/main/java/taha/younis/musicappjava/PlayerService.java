package taha.younis.musicappjava;

import static androidx.core.app.NotificationManagerCompat.IMPORTANCE_HIGH;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Binder;
import android.os.IBinder;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.ui.PlayerNotificationManager;

import java.util.Objects;

public class PlayerService extends Service {
    //member
    private final IBinder serviceBinder = new ServiceBinder();

    ExoPlayer player;
    PlayerNotificationManager playerNotificationManager;

    public class ServiceBinder extends Binder {
        public PlayerService getPlayerService(){
            return PlayerService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return serviceBinder;
        // when this method called then the ServiceBind intercept the player service
    }

    @Override
    public void onCreate() {
        super.onCreate();
        player = new ExoPlayer.Builder(getApplicationContext()).build();
        // this exoplayer must enter audio focus
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.CONTENT_TYPE_MUSIC)
                                .build();
        player.setAudioAttributes(audioAttributes, true);

        // for player notification when playing music we need notifications channel, id, and manager
        final String channelId = getResources().getString(R.string.app_name) + " Music Channel ";
        final int notificationId = 111111;
        playerNotificationManager = new PlayerNotificationManager.Builder(this,notificationId,channelId)
                .setNotificationListener(notificationListener)
                .setMediaDescriptionAdapter(descriptionAdapter)
                .setChannelImportance(IMPORTANCE_HIGH)
                .setSmallIconResourceId(R.drawable.img_music)
                .setChannelDescriptionResourceId(R.string.app_name)
                .setNextActionIconResourceId(R.drawable.ic_skip_next)
                .setPreviousActionIconResourceId(R.drawable.ic_skip_previous)
                .setPauseActionIconResourceId(R.drawable.ic_pause)
                .setPlayActionIconResourceId(R.drawable.ic_play)
                .setChannelNameResourceId(R.string.app_name)
                .build();
        // set player to notification manager
        playerNotificationManager.setPlayer(player);
        playerNotificationManager.setPriority(NotificationCompat.PRIORITY_MAX);
        playerNotificationManager.setUseRewindAction(false);
        playerNotificationManager.setUseFastForwardAction(false);

    }
    // notification listener to help us to know if notification is posted or his content
    PlayerNotificationManager.NotificationListener notificationListener = new PlayerNotificationManager.NotificationListener() {
        @Override
        public void onNotificationCancelled(int notificationId, boolean dismissedByUser) {
            PlayerNotificationManager.NotificationListener.super.onNotificationCancelled(notificationId, dismissedByUser);
            stopForeground(true);
            if (player.isPlaying())
                player.pause();
        }

        @Override
        public void onNotificationPosted(int notificationId, Notification notification, boolean ongoing) {
            PlayerNotificationManager.NotificationListener.super.onNotificationPosted(notificationId, notification, ongoing);
            // posted means that the notification is put into notification tray of the device
            startForeground(notificationId, notification); // start foreground notif.
        }
    };

    // notification description adapter
    PlayerNotificationManager.MediaDescriptionAdapter descriptionAdapter =
            new PlayerNotificationManager.MediaDescriptionAdapter() {
                @Override
                public CharSequence getCurrentContentTitle(Player player) {
                    return Objects.requireNonNull(player.getCurrentMediaItem()).mediaMetadata.title;
                }

                @Nullable
                @Override
                public PendingIntent createCurrentContentIntent(Player player) {
                    // Intent to open the app when clicked
                    Intent openAppIntent = new Intent(getApplicationContext(),MainActivity.class);
                    return PendingIntent.getActivity(getApplicationContext(),0, openAppIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
                }

                @Nullable
                @Override
                public CharSequence getCurrentContentText(Player player) {
                    return null;
                }

                @Nullable
                @Override
                public Bitmap getCurrentLargeIcon(Player player, PlayerNotificationManager.BitmapCallback callback) {
                    // try creating an image view on the fly then get its drawable
                    ImageView view = new ImageView(getApplicationContext());
                    view.setImageURI(Objects.requireNonNull(player.getCurrentMediaItem()).mediaMetadata.artworkUri);
                    //get view drawable
                    BitmapDrawable bitmapDrawable = (BitmapDrawable) view.getDrawable();
                    if (bitmapDrawable == null){
                        bitmapDrawable = (BitmapDrawable) ContextCompat.getDrawable(getApplicationContext(),R.drawable.img_music);
                    }

                    assert bitmapDrawable != null;
                    return bitmapDrawable.getBitmap();
                }
            };

    @Override
    public void onDestroy() {
        if (player.isPlaying()) player.stop();
        playerNotificationManager.setPlayer(null);
        player.release();
        player = null;
        stopForeground(true);
        stopSelf();
        super.onDestroy();
    }
}