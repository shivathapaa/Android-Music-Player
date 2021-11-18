package com.thapamusic.wetunes;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.renderscript.Script;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.ArrayList;

import static com.thapamusic.wetunes.ApplicationClass.ACTION_NEXT;
import static com.thapamusic.wetunes.ApplicationClass.ACTION_PLAY;
import static com.thapamusic.wetunes.ApplicationClass.ACTION_PREVIOUS;
import static com.thapamusic.wetunes.ApplicationClass.CHANNEL_ID_2;
import static com.thapamusic.wetunes.PlayerActivity.listSongs;

public class MusicService extends Service implements MediaPlayer.OnCompletionListener {

    IBinder mBinder = new MyBinder();
    MediaPlayer mediaPlayer;
    ArrayList<MusicFiles> musicFiles = new ArrayList<>();
    Uri uri;
    int position = -1;
    ActionPlaying actionPlaying;
    MediaSessionCompat mediaSessionCompat;
    public static final String MUSIC_LAST_PLAYED = "LAST_PLAYED";
    public static final String MUSIC_FILE = "STORED_MUSIC";
    public static final String ARTIST_NAME = "ARTIST NAME";
    public static final String SONG_NAME = "SONG NAME";
    static int passPosition;

    NotificationManager notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        mediaSessionCompat = new MediaSessionCompat(getBaseContext(),  "My Audio");
        try {
            musicFiles = listSongs;

        } catch (NullPointerException e) {
            System.err.println("Null pointer exception");
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.e("Bind", "Method");
        return mBinder;
    }

    public class MyBinder extends Binder{
        MusicService getService(){
            return MusicService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int myPosition = intent.getIntExtra("servicePosition", -1);
//        createMediaPlayer(myPosition);   //Commented line in action. CHECK OUT!!!
        String actionName = intent.getStringExtra("ActionName");
        if (myPosition != -1){
            try {
                playMedia(myPosition);

            } catch (NullPointerException e) {
                System.err.println("Null pointer exception");
            }
        }
        if (actionName != null){
            switch (actionName){
                case "playPause":
//                    Toast.makeText(this, "PlayPause", Toast.LENGTH_SHORT).show();
                    playPauseBtnClicked();
                    break;
                case "next":
//                    Toast.makeText(this, "Next", Toast.LENGTH_SHORT).show();
                    nextBtnClicked();
                    break;
                case "previous":
//                    Toast.makeText(this, "Previous", Toast.LENGTH_SHORT).show();
                    prevBtnClicked();
                    break;
            }
        }
        return START_STICKY;
    }

    private void playMedia(int StartPosition) {
        musicFiles = listSongs;
        position = StartPosition;
        if (mediaPlayer != null){
            mediaPlayer.stop();
            mediaPlayer.release();
            if (musicFiles != null){
                createMediaPlayer(position);
                mediaPlayer.start();
            }
        }
        else{
            try {
                createMediaPlayer(position);

            } catch (NullPointerException e) {
                System.err.println("Null pointer exception");
            }
            mediaPlayer.start();
        }
    }

    void start() {
        mediaPlayer.start();
    }

    boolean isPlaying(){
        return mediaPlayer.isPlaying();
    }

    void stop(){
        mediaPlayer.stop();
    }

    void release(){
        mediaPlayer.release();
    }

    int getDuration(){
        return mediaPlayer.getDuration();
    }

    void seekTo(int position){
        mediaPlayer.seekTo(position);
    }

    int getCurrentPosition(){
        return mediaPlayer.getCurrentPosition();
    }

    void createMediaPlayer(int positionInner){
        position = positionInner;
        try {
            uri = Uri.parse(musicFiles.get(position).getPath());

        } catch (NullPointerException e) {
            System.err.println("Null pointer exception in createmediaplayer");
        }
        SharedPreferences.Editor editor = getSharedPreferences(MUSIC_LAST_PLAYED, MODE_PRIVATE).edit();
        editor.putString(MUSIC_FILE, uri.toString());
        editor.putString(ARTIST_NAME, musicFiles.get(position).getArtist());
        editor.putString(SONG_NAME, musicFiles.get(position).getTitle());
        editor.apply();
        mediaPlayer = MediaPlayer.create(getBaseContext(), uri);
    }

    void pause(){
        mediaPlayer.pause();
    }

    void OnCompleted(){
        mediaPlayer.setOnCompletionListener(this);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (actionPlaying != null){
            actionPlaying.nextBtnClicked();
            if (mediaPlayer != null){
                createMediaPlayer(position);
                mediaPlayer.start();
                OnCompleted();
            }
        }
    }

    void setCallBack(ActionPlaying actionPlaying){
        this.actionPlaying = actionPlaying;
    }

    void showNotification(int playPauseBtn){
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, 0);

        Intent prevIntent = new Intent(this, NotificationReceiver.class)
                .setAction(ACTION_PREVIOUS);
        PendingIntent prevPending = PendingIntent.getBroadcast(this, 0, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent pauseIntent = new Intent(this, NotificationReceiver.class)
                .setAction(ACTION_PLAY);
        PendingIntent  pausePending = PendingIntent.getBroadcast(this, 0, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent nextIntent = new Intent(this, NotificationReceiver.class)
                .setAction(ACTION_NEXT);
        PendingIntent nextPending = PendingIntent.getBroadcast(this, 0, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        byte[] picture = null;
        picture = getAlbumArt(musicFiles.get(position).getPath());
        Bitmap thumb = null;
        if (picture != null){
            thumb = BitmapFactory.decodeByteArray(picture, 0, picture.length);
        }
        else{
            thumb = BitmapFactory.decodeResource(getResources(), R.drawable.musicicon);
        }
        passPosition = position;
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID_2)
                .setSmallIcon(playPauseBtn)
                .setLargeIcon(thumb)
                .setContentTitle(musicFiles.get(position).getTitle())
                .setContentText(musicFiles.get(position).getArtist())
                .addAction(R.drawable.ic_skip_previous, "Previous", prevPending)
                .addAction(playPauseBtn, "Pause", pausePending)
                .addAction(R.drawable.ic_skip_next, "Next", nextPending)
                .setContentIntent(contentIntent)
//                .setAutoCancel(true)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSessionCompat.getSessionToken()))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOnlyAlertOnce(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build();
        startForeground(2, notification);
        notificationManager =
                (NotificationManager)this.getSystemService(Context.NOTIFICATION_SERVICE);
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notificationManager.notify(2, notification);
    }

    private byte[] getAlbumArt(String uri){
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(uri);
        byte[] art = retriever.getEmbeddedPicture();
        retriever.release();
        return art;
    }

    void playPauseBtnClicked(){
        if (actionPlaying != null){
            actionPlaying.playPauseBtnClicked();
        }
    }

    void nextBtnClicked(){
        if (actionPlaying != null){
            actionPlaying.nextBtnClicked();
        }
    }

    void prevBtnClicked(){
        if (actionPlaying != null){
            actionPlaying.prevBtnClicked();
        }
    }

}
