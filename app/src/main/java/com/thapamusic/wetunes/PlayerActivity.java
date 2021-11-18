package com.thapamusic.wetunes;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.palette.graphics.Palette;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.media.session.MediaSessionCompat;
import android.text.Layout;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Random;

import static com.thapamusic.wetunes.AlbumDetailsAdapter.albumFiles;
import static com.thapamusic.wetunes.ApplicationClass.ACTION_NEXT;
import static com.thapamusic.wetunes.ApplicationClass.ACTION_PLAY;
import static com.thapamusic.wetunes.ApplicationClass.ACTION_PREVIOUS;
import static com.thapamusic.wetunes.ApplicationClass.CHANNEL_ID_2;
import static com.thapamusic.wetunes.MainActivity.PATH_TO_FRAG;
import static com.thapamusic.wetunes.MainActivity.musicFiles;
import static com.thapamusic.wetunes.MainActivity.repeatBoolean;
import static com.thapamusic.wetunes.MainActivity.shuffleBoolean;
import static com.thapamusic.wetunes.MusicAdapter.mFiles;
import static com.thapamusic.wetunes.MusicService.passPosition;
import static java.security.AccessController.getContext;

public class PlayerActivity extends AppCompatActivity implements ActionPlaying, ServiceConnection {

    TextView song_name, artist_name, duration_played, duration_total, album_name, textNowplaying;
    ImageView cover_art, nextBtn, prevBtn, backBtn, shuffleBtn, repeatBtn;
    static byte[] artist_image;
    FloatingActionButton playPauseBtn;
    SeekBar seekBar;

    int position = -1;
    static ArrayList<MusicFiles> listSongs = new ArrayList<>();
    static Uri uri;
//    static MediaPlayer mediaPlayer;
    private Handler handler = new Handler();
    private Thread playThread, prevThread, nextThread;
    MusicService musicService;
    static MusicService passMusicService;
    static GradientDrawable gradientDrawableBg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setFullScreen();
        setContentView(R.layout.activity_player);
        getSupportActionBar().hide();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("playerActivitypass", true);
        editor.commit();
        NowPlayingFragmentBottom.setLayoutVisible();
        initViews();
        getIntenMethod();
//        PassingBottomFrag();
        if (shuffleBoolean && !repeatBoolean){
            shuffleBtn.setImageResource(R.drawable.ic_shuffle_on);
        }else if (!shuffleBoolean && repeatBoolean){
            repeatBtn.setImageResource(R.drawable.ic_repeat_on);
        }else if (shuffleBoolean && repeatBoolean){
            shuffleBtn.setImageResource(R.drawable.ic_shuffle_on);
            repeatBtn.setImageResource(R.drawable.ic_repeat_on);
        }
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (musicService != null && fromUser){
                    musicService.seekTo(progress * 1000);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        PlayerActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (musicService != null){
                    int mCurrentPosition = musicService.getCurrentPosition() / 1000;
                    seekBar.setProgress(mCurrentPosition);
                    duration_played.setText(formattedTime(mCurrentPosition));
                }
                handler.postDelayed(this, 1000);
            }
        });
        shuffleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (shuffleBoolean)
                {
                    shuffleBoolean = false;
                    shuffleBtn.setImageResource(R.drawable.ic_shuffle_off);
                }else {
                    shuffleBoolean = true;
                    shuffleBtn.setImageResource(R.drawable.ic_shuffle_on);
                }
            }
        });
        repeatBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(repeatBoolean)
                {
                    repeatBoolean = false;
                    repeatBtn.setImageResource(R.drawable.ic_repeat_off);

                }else {
                    repeatBoolean = true;
                    repeatBtn.setImageResource(R.drawable.ic_repeat_on);
                }
            }
        });
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Intent intent_D = new Intent(getApplicationContext(), MainActivity.class);
//                intent_D.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                startActivity(intent_D);
                finish();
            }
        });
    }

    @Override
    public void onBackPressed() {
        finish();
//        finish();
//        overridePendingTransition( 0, 0);
//        startActivity(getIntent());
//        overridePendingTransition( 0, 0);
    }

    private void setFullScreen() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    @Override
    protected void onResume() {
        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, this, BIND_AUTO_CREATE);
        playThreadBtn();
        nextThreadBtn();
        prevThreadBtn();
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(this);
    }

    private void prevThreadBtn() {
        prevThread = new Thread()
        {
            @Override
            public void run() {
                super.run();
                prevBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        prevBtnClicked();

                    }
                });
            }
        };
        prevThread.start();
    }

    public void prevBtnClicked() {
        if (musicService.isPlaying()){
            musicService.stop();
            musicService.release();
            if (shuffleBoolean && !repeatBoolean){
                position = getRandom(listSongs.size() - 1);
                shuffleBtn.setImageResource(R.drawable.ic_shuffle_on);
            } else if (!shuffleBoolean && !repeatBoolean) {
                position = ((position - 1) < 0 ? (listSongs.size() - 1) : (position -1));
            }else if (!shuffleBoolean && repeatBoolean){
                repeatBtn.setImageResource(R.drawable.ic_repeat_on);
            }else if (shuffleBoolean && repeatBoolean){
                shuffleBtn.setImageResource(R.drawable.ic_shuffle_on);
                repeatBtn.setImageResource(R.drawable.ic_repeat_on);
            }
            uri = Uri.parse(listSongs.get(position).getPath());
            musicService.createMediaPlayer(position);
            metaData(uri);
            song_name.setText(listSongs.get(position).getTitle());
            artist_name.setText(listSongs.get(position).getArtist());
            album_name.setText(listSongs.get(position).getAlbum());
            seekBar.setMax(musicService.getDuration() / 1000);
            PlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (musicService != null){
                        int mCurrentPosition = musicService.getCurrentPosition() / 1000;
                        seekBar.setProgress(mCurrentPosition);
                    }
                    handler.postDelayed(this, 1000);
                }
            });
            musicService.OnCompleted();
            musicService.showNotification(R.drawable.ic_pause);
            playPauseBtn.setBackgroundResource(R.drawable.ic_pause);
            musicService.start();
            passMusicService = musicService;

            byte[] art = artist_image;
            if (art != null){
                Glide.with(getBaseContext()).load(art)
                        .into(NowPlayingFragmentBottom.albumArt);
            }else{
//                Toast.makeText(musicService, "Its null", Toast.LENGTH_SHORT).show();
            }
            NowPlayingFragmentBottom.songName.setText(listSongs.get(position).getTitle());
            NowPlayingFragmentBottom.artist.setText(listSongs.get(position).getArtist());
        }
        else {
            musicService.stop();
            musicService.release();
            if (shuffleBoolean && !repeatBoolean){
                position = getRandom(listSongs.size() - 1);
                shuffleBtn.setImageResource(R.drawable.ic_shuffle_on);
            } else if (!shuffleBoolean && !repeatBoolean) {
                position = ((position - 1) < 0 ? (listSongs.size() - 1) : (position -1));
            }else if (!shuffleBoolean && repeatBoolean){
                repeatBtn.setImageResource(R.drawable.ic_repeat_on);
            }else if (shuffleBoolean && repeatBoolean){
                shuffleBtn.setImageResource(R.drawable.ic_shuffle_on);
                repeatBtn.setImageResource(R.drawable.ic_repeat_on);
            }
            uri = Uri.parse(listSongs.get(position).getPath());
            musicService.createMediaPlayer(position);
            metaData(uri);
            song_name.setText(listSongs.get(position).getTitle());
            artist_name.setText(listSongs.get(position).getArtist());
            album_name.setText(listSongs.get(position).getAlbum());
            seekBar.setMax(musicService.getDuration() / 1000);
            PlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (musicService != null){
                        int mCurrentPosition = musicService.getCurrentPosition() / 1000;
                        seekBar.setProgress(mCurrentPosition);
                    }
                    handler.postDelayed(this, 1000);
                }
            });
            musicService.OnCompleted();
            musicService.showNotification(R.drawable.ic_play);
            playPauseBtn.setBackgroundResource(R.drawable.ic_play);
            passMusicService = musicService;

            byte[] art = artist_image;
            if (art != null){
                Glide.with(getBaseContext()).load(art)
                        .into(NowPlayingFragmentBottom.albumArt);
            }else{
//                Toast.makeText(musicService, "Its null", Toast.LENGTH_SHORT).show();
            }
            NowPlayingFragmentBottom.songName.setText(listSongs.get(position).getTitle());
            NowPlayingFragmentBottom.artist.setText(listSongs.get(position).getArtist());
        }
    }

    private void nextThreadBtn() {
        nextThread = new Thread()
        {
            @Override
            public void run() {
                super.run();
                nextBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        nextBtnClicked();

                    }
                });
            }
        };
        nextThread.start();
    }

    public void nextBtnClicked() {
        if (musicService.isPlaying()){
            musicService.stop();
            musicService.release();
            if (shuffleBoolean && !repeatBoolean){
                position = getRandom(listSongs.size() - 1);
                shuffleBtn.setImageResource(R.drawable.ic_shuffle_on);
            } else if (!shuffleBoolean && !repeatBoolean) {
                position = ((position + 1) % listSongs.size());
            }else if (!shuffleBoolean && repeatBoolean){
                repeatBtn.setImageResource(R.drawable.ic_repeat_on);
            }else if (shuffleBoolean && repeatBoolean){
                shuffleBtn.setImageResource(R.drawable.ic_shuffle_on);
                repeatBtn.setImageResource(R.drawable.ic_repeat_on);
            }
            //else position will be position
            uri = Uri.parse(listSongs.get(position).getPath());
            musicService.createMediaPlayer(position);
            metaData(uri);
            song_name.setText(listSongs.get(position).getTitle());
            artist_name.setText(listSongs.get(position).getArtist());
            album_name.setText(listSongs.get(position).getAlbum());
            seekBar.setMax(musicService.getDuration() / 1000);
            PlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (musicService != null){
                        int mCurrentPosition = musicService.getCurrentPosition() / 1000;
                        seekBar.setProgress(mCurrentPosition);
                    }
                    handler.postDelayed(this, 1000);
                }
            });
            musicService.OnCompleted();
            musicService.showNotification(R.drawable.ic_pause);
            playPauseBtn.setBackgroundResource(R.drawable.ic_pause);
            musicService.start();
            passMusicService = musicService;

            byte[] art = artist_image;
            if (art != null){
                Glide.with(getBaseContext()).load(art)
                        .into(NowPlayingFragmentBottom.albumArt);
            }else{
//                Toast.makeText(musicService, "Its null", Toast.LENGTH_SHORT).show();
            }
            NowPlayingFragmentBottom.songName.setText(listSongs.get(position).getTitle());
            NowPlayingFragmentBottom.artist.setText(listSongs.get(position).getArtist());
        }
        else {
            musicService.stop();
            musicService.release();
            if (shuffleBoolean && !repeatBoolean){
                position = getRandom(listSongs.size() - 1);
                shuffleBtn.setImageResource(R.drawable.ic_shuffle_on);
            } else if (!shuffleBoolean && !repeatBoolean) {
                position = ((position + 1) % listSongs.size());
            }else if (!shuffleBoolean && repeatBoolean){
                repeatBtn.setImageResource(R.drawable.ic_repeat_on);
            }else if (shuffleBoolean && repeatBoolean){
                shuffleBtn.setImageResource(R.drawable.ic_shuffle_on);
                repeatBtn.setImageResource(R.drawable.ic_repeat_on);
            }
            //else position will be position
//            position = ((position + 1) % listSongs.size());
            uri = Uri.parse(listSongs.get(position).getPath());
            musicService.createMediaPlayer(position);
            metaData(uri);
            song_name.setText(listSongs.get(position).getTitle());
            artist_name.setText(listSongs.get(position).getArtist());
            album_name.setText(listSongs.get(position).getAlbum());
            seekBar.setMax(musicService.getDuration() / 1000);
            PlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (musicService != null){
                        int mCurrentPosition = musicService.getCurrentPosition() / 1000;
                        seekBar.setProgress(mCurrentPosition);
                    }
                    handler.postDelayed(this, 1000);
                }
            });
            musicService.OnCompleted();
            musicService.showNotification(R.drawable.ic_play);
            playPauseBtn.setBackgroundResource(R.drawable.ic_play);
            passMusicService = musicService;

            byte[] art = artist_image;
            if (art != null){
                Glide.with(getBaseContext()).load(art)
                        .into(NowPlayingFragmentBottom.albumArt);
            }else{
//                Toast.makeText(musicService, "Art is NULL!!!", Toast.LENGTH_SHORT).show();
            }
            NowPlayingFragmentBottom.songName.setText(listSongs.get(position).getTitle());
            NowPlayingFragmentBottom.artist.setText(listSongs.get(position).getArtist());
        }
    }

    private int getRandom(int i) {
        Random random = new Random();
        return random.nextInt(i + 1);
    }

    private void playThreadBtn() {
        playThread = new Thread()
        {
            @Override
            public void run() {
                super.run();
                playPauseBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        playPauseBtnClicked();
                        
                    }
                });
            }
        };
        playThread.start();
    }

    public void playPauseBtnClicked() {
        if (musicService.isPlaying()){
            playPauseBtn.setImageResource(R.drawable.ic_play);
            musicService.showNotification(R.drawable.ic_play);
            NowPlayingFragmentBottom.playPauseBtn.setImageResource(R.drawable.ic_play);
            musicService.pause();
            if (shuffleBoolean && !repeatBoolean){
                shuffleBtn.setImageResource(R.drawable.ic_shuffle_on);
            }else if (!shuffleBoolean && repeatBoolean){
                repeatBtn.setImageResource(R.drawable.ic_repeat_on);
            }else if (shuffleBoolean && repeatBoolean){
                shuffleBtn.setImageResource(R.drawable.ic_shuffle_on);
                repeatBtn.setImageResource(R.drawable.ic_repeat_on);
            }
            seekBar.setMax(musicService.getDuration() / 1000);
            PlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (musicService != null){
                        int mCurrentPosition = musicService.getCurrentPosition() / 1000;
                        seekBar.setProgress(mCurrentPosition);
                    }
                    handler.postDelayed(this, 1000);
                }
            });
            passMusicService = musicService;
        }
        else{
            musicService.showNotification(R.drawable.ic_pause);
            playPauseBtn.setImageResource(R.drawable.ic_pause);
            NowPlayingFragmentBottom.playPauseBtn.setImageResource(R.drawable.ic_pause);
            musicService.start();
            if (shuffleBoolean && !repeatBoolean){
                shuffleBtn.setImageResource(R.drawable.ic_shuffle_on);
            }else if (!shuffleBoolean && repeatBoolean){
                repeatBtn.setImageResource(R.drawable.ic_repeat_on);
            }else if (shuffleBoolean && repeatBoolean){
                shuffleBtn.setImageResource(R.drawable.ic_shuffle_on);
                repeatBtn.setImageResource(R.drawable.ic_repeat_on);
            }
            seekBar.setMax(musicService.getDuration() / 1000);
            PlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (musicService != null){
                        int mCurrentPosition = musicService.getCurrentPosition() / 1000;
                        seekBar.setProgress(mCurrentPosition);
                    }
                    handler.postDelayed(this, 1000);
                }
            });
            passMusicService = musicService;
        }
    }

//    public void PassingBottomFrag(){
//        uri = Uri.parse(listSongs.get(position).getPath());
//        metaData(uri);
//        byte[] artPhoto = artist_image;
//        if (artPhoto != null){
//            Glide.with(getBaseContext()).load(artPhoto)
//                    .into(NowPlayingFragmentBottom.albumArt);
//        }else{
////                Toast.makeText(musicService, "Art is NULL!!!", Toast.LENGTH_SHORT).show();
//        }
//        NowPlayingFragmentBottom.songName.setText(listSongs.get(position).getTitle());
//        NowPlayingFragmentBottom.artist.setText(listSongs.get(position).getArtist());
//    }

    private String formattedTime(int mCurrentPosition) {
        String totalout = "";
        String totalNew = "";
        String seconds = String.valueOf(mCurrentPosition % 60);
        String minutes = String.valueOf(mCurrentPosition / 60);
        totalout = minutes + ":" + seconds;
        totalNew = minutes + ":" + "0" + seconds;
        if (seconds.length() == 1){
            return totalNew;
        }
        else{
            return totalout;
        }
    }

    private void getIntenMethod() {
        String sender = getIntent().getStringExtra("sender");
        String musicAdapt = getIntent().getStringExtra("musicAdapter");
        if (sender != null && sender.equals("albumDetails")){
            position = getIntent().getIntExtra("positionAlbum",-1);
            listSongs = albumFiles;
        }
        else{
            position = getIntent().getIntExtra("positionMfiles",-1);
            listSongs = mFiles;
        }
        if(listSongs != null)
        {
            NowPlayingFragmentBottom.playPauseBtn.setImageResource(R.drawable.ic_pause);
            playPauseBtn.setImageResource(R.drawable.ic_pause);
            uri = Uri.parse(listSongs.get(position).getPath());
        }


        //HERE IT IS!!

        if (musicService != null){
            musicService.stop();
            musicService.release();
        }

        //ABOVE BLOCK

        Intent intent = new Intent(this, MusicService.class);
        intent.putExtra("servicePosition", position);
        startService(intent);
    }

    private void initViews(){
        song_name = findViewById(R.id.song_name);
        artist_name = findViewById(R.id.song_artist);
        album_name = findViewById(R.id.song_album);
        duration_played = findViewById(R.id.durationPlayed);
        duration_total = findViewById(R.id.durationTotal);
        cover_art = findViewById(R.id.cover_art);
        nextBtn = findViewById(R.id.id_next);
        prevBtn = findViewById(R.id.id_prev);
        backBtn = findViewById(R.id.back_btn);
        shuffleBtn = findViewById(R.id.id_shuffle);
        repeatBtn = findViewById(R.id.id_repeat);
        playPauseBtn = findViewById(R.id.play_pause);
        seekBar = findViewById(R.id.seekBar);
        textNowplaying = findViewById(R.id.nowplaing);
//         = findViewById(R.id.);
//         = findViewById(R.id.);
//         = findViewById(R.id.);
    }
    public boolean isColorDark(int color){
        double darkness = 1-(0.299*Color.red(color) + 0.587*Color.green(color) + 0.114*Color.blue(color))/255;
        if(darkness<0.5){
            return false; // It's a light color
        }else{
            return true; // It's a dark color
        }
    }
    private void metaData(Uri uri){
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(uri.toString());
        int durationTotal = Integer.parseInt(listSongs.get(position).getDuration()) / 1000;
        duration_total.setText (formattedTime(durationTotal));
        byte[] art = retriever.getEmbeddedPicture();
        artist_image = art;
        Bitmap bitmap;
        if (art != null){
            bitmap = BitmapFactory.decodeByteArray(art, 0, art.length);
            ImageAnimation(this, cover_art, bitmap);
            Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {
                @Override
                public void onGenerated(@Nullable @org.jetbrains.annotations.Nullable Palette palette) {
                    Palette.Swatch swatch = palette.getDominantSwatch();
                    if (swatch != null)
                    {
                        ImageView gredient = findViewById(R.id.imageViewGredient);
                        RelativeLayout mContainer = findViewById(R.id.mContainer);
                        gredient.setImageResource(R.drawable.gredient_bg);
//                        RelativeLayout topGradient = (RelativeLayout) findViewById(R.id.layot_top_btn);
//                        topGradient.setBackgroundResource(R.drawable.gredient_for_top_bg);
                        mContainer.setBackgroundResource(R.drawable.main_bg);
                        GradientDrawable gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                                new int[]{swatch.getRgb(), 0x00000000});
                        gredient.setBackground(gradientDrawable);
                        GradientDrawable gradientDrawableBg = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                                new int[]{0x44444444, swatch.getRgb()});
                        GradientDrawable mContainer_gradientDrawableBg = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                                new int[]{0x44444444, swatch.getRgb()});
                        NowPlayingFragmentBottom.bottom_bac_frag.setBackground(gradientDrawableBg);
                        mContainer.setBackground(mContainer_gradientDrawableBg);

                        song_name.setTextColor(swatch.getTitleTextColor());
                        artist_name.setTextColor(swatch.getBodyTextColor());
                        album_name.setTextColor(swatch.getTitleTextColor());
                        textNowplaying.setTextColor(swatch.getBodyTextColor());
                        NowPlayingFragmentBottom.songName.setTextColor(swatch.getBodyTextColor());
                        NowPlayingFragmentBottom.artist.setTextColor(swatch.getTitleTextColor());
//                        playPauseBtn.setBackgroundTintList(ColorStateList.valueOf(swatch.getRgb()));
                        if (isColorDark(swatch.getRgb())== true){
                            int ColorValue = Color.parseColor("#E8DFDF");
                            ImageViewCompat.setImageTintList(playPauseBtn, ColorStateList.valueOf(ColorValue));
                            ImageViewCompat.setImageTintList(NowPlayingFragmentBottom.playPauseBtn, ColorStateList.valueOf(ColorValue));
                        }else{
                            int ColorValue = Color.parseColor("#353131");
                            ImageViewCompat.setImageTintList(playPauseBtn, ColorStateList.valueOf(ColorValue));
                            ImageViewCompat.setImageTintList(NowPlayingFragmentBottom.playPauseBtn, ColorStateList.valueOf(ColorValue));
                        }
                        playPauseBtn.setBackgroundTintList(ColorStateList.valueOf(swatch.getRgb()));
                        NowPlayingFragmentBottom.playPauseBtn.setBackgroundTintList(ColorStateList.valueOf(swatch.getRgb()));

                    }
                    else {
                        ImageView gredient = findViewById(R.id.imageViewGredient);
                        RelativeLayout mContainer = findViewById(R.id.mContainer);
                        gredient.setImageResource(R.drawable.gredient_bg);
                        mContainer.setBackgroundResource(R.drawable.main_bg);
                        GradientDrawable gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                                new int[]{0xff000000, 0x00000000});
                        gredient.setBackground(gradientDrawable);
                        GradientDrawable gradientDrawableBg_mContainer = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                                new int[]{0xff000000, 0xff000000});
                        GradientDrawable gradientDrawableBg = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                                new int[]{0xff000000, 0xff000000});
                        NowPlayingFragmentBottom.bottom_bac_frag.setBackground(gradientDrawableBg);
                        mContainer.setBackground(gradientDrawableBg_mContainer);
                        song_name.setTextColor(Color.WHITE);
                        artist_name.setTextColor(Color.DKGRAY);
                        album_name.setTextColor(Color.WHITE);
                    }
                }

            });
        }
        else{
            if (isValidContextForGlide(this)){
                // Load image via Glide lib using context
                Glide.with(this)
                        .asBitmap()
                        .load(R.drawable.musicicon)
                        .into(cover_art);
            }

            ImageView gredient = findViewById(R.id.imageViewGredient);
            RelativeLayout mContainer = findViewById(R.id.mContainer);
            gredient.setImageResource(R.drawable.gredient_bg);
            mContainer.setBackgroundResource(R.drawable.main_bg);
//            GradientDrawable gradientDrawableBg = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
//                    new int[]{0xff000000, 0xff000000});
//            NowPlayingFragmentBottom.bottom_bac_frag.setBackground(gradientDrawableBg);
            song_name.setTextColor(Color.WHITE);
            artist_name.setTextColor(Color.DKGRAY);
            album_name.setTextColor(Color.WHITE);
        }
    }

    public void ImageAnimation(Context context, ImageView imageView, Bitmap bitmap)
    {
        Animation animOut = AnimationUtils.loadAnimation(context, android.R.anim.fade_out);
        Animation animIn = AnimationUtils.loadAnimation(context, android.R.anim.fade_in);
        animOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                Glide.with(context).load(bitmap).into(imageView);
                animIn.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                imageView.startAnimation(animIn);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        imageView.startAnimation(animOut);
    }

    public static boolean isValidContextForGlide(final Context context) {
        if (context == null) {
            return false;
        }
        if (context instanceof Activity) {
            final Activity activity = (Activity) context;
            if (activity.isDestroyed() || activity.isFinishing()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service)  {
        MusicService.MyBinder myBinder = (MusicService.MyBinder) service;
        musicService = myBinder.getService();
        musicService.setCallBack(this);
//        Toast.makeText(this, "Connected" + musicService + "7777777777777777", Toast.LENGTH_SHORT).show();
        seekBar.setMax(musicService.getDuration() / 1000);
        metaData(uri);
        song_name.setText(listSongs.get(position).getTitle());
        artist_name.setText(listSongs.get(position).getArtist());
        album_name.setText(listSongs.get(position).getAlbum());
        musicService.OnCompleted();
        musicService.showNotification(R.drawable.ic_pause);
        passMusicService = musicService;
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        musicService = null;
    }

}