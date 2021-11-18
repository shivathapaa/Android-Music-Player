package com.thapamusic.wetunes;

//import android.app.Notification;
//import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;

//import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.Fragment;

import android.os.IBinder;
//import android.preference.PreferenceManager;
//import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
//import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
//import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

//import java.util.Objects;

import static android.content.Context.MODE_PRIVATE;
import static com.thapamusic.wetunes.MainActivity.ARTIST_TO_FRAG;
import static com.thapamusic.wetunes.MainActivity.PATH_TO_FRAG;
import static com.thapamusic.wetunes.MainActivity.SHOW_MINI_PLAYER;
import static com.thapamusic.wetunes.MainActivity.SONG_NAME_TO_FRAG;
//import static com.thapamusic.wetunes.MainActivity.albums;


public class  NowPlayingFragmentBottom extends Fragment implements ServiceConnection {

    ImageView nextBtn, prevBtn;
    static ImageView albumArt;
    static TextView artist, songName;
    public static FloatingActionButton playPauseBtn;
    View view;
    MusicService musicService;
    public static final String MUSIC_LAST_PLAYED = "LAST_PLAYED";
    public static final String MUSIC_FILE = "STORED_MUSIC";
    public static final String ARTIST_NAME = "ARTIST NAME";
    public static final String SONG_NAME = "SONG NAME";
    static RelativeLayout bottom_bac_frag;
    Boolean bindservice = false;

    public NowPlayingFragmentBottom() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_now_playing_bottom, container, false);
        bottom_bac_frag = (RelativeLayout) view.findViewById(R.id.card_bottom_player);
//        bottom_bac_frag.setVisibility(Visibility.VISIBLE);
//        bottom_bac_frag.setBackground(PlayerActivity.gradientDrawableBg);
        artist = view.findViewById(R.id.song_artist_miniPlayer);
        songName = view.findViewById(R.id.song_name_miniPlayer);
        albumArt = view.findViewById(R.id.bottom_album_art);
        nextBtn = view.findViewById(R.id.skip_next_bottom);
        prevBtn = view.findViewById(R.id.skip_prev_bottom);
        playPauseBtn = view.findViewById(R.id.play_pause_miniPlayer);
        nextBtn.setOnClickListener(v -> {
//                Toast.makeText(getContext(), "Next", Toast.LENGTH_SHORT).show();
            if (musicService != null){
                musicService.nextBtnClicked();
                if (getActivity() != null) {
//                        SharedPreferences.Editor editor = getActivity().getSharedPreferences(MUSIC_LAST_PLAYED, MODE_PRIVATE).edit();
//                        editor.putString(MUSIC_FILE, musicService.musicFiles.get(musicService.position).getPath());
//                        editor.putString(ARTIST_NAME, musicService.musicFiles.get(musicService.position).getArtist());
//                        editor.putString(SONG_NAME, musicService.musicFiles.get(musicService.position).getTitle());
//                        editor.apply();

                    SharedPreferences preferences = getActivity().getSharedPreferences(MUSIC_LAST_PLAYED, MODE_PRIVATE);
                    String path = preferences.getString(MUSIC_FILE, null);
                    String artistName = preferences.getString(ARTIST_NAME, null);
                    String  song_name = preferences.getString(SONG_NAME, null);
                    if (path != null) {
                        SHOW_MINI_PLAYER = true;
                        PATH_TO_FRAG = path;
                        ARTIST_TO_FRAG = artistName;
                        SONG_NAME_TO_FRAG = song_name;
                    }
                    else{
                        SHOW_MINI_PLAYER = false;
                        PATH_TO_FRAG = null;
                        ARTIST_TO_FRAG = null;
                        SONG_NAME_TO_FRAG = null;
                    }
                    if (SHOW_MINI_PLAYER){
                        byte[] art = getAlbumArt(PATH_TO_FRAG);
                        if (art != null){
                            Glide.with(requireContext()).load(art)
                                    .into(albumArt);
                        }
                        else{
                            Glide.with(requireContext()).load(R.drawable.musicicon)
                                    .into(albumArt);
                        }
                        songName.setText(SONG_NAME_TO_FRAG);
                        artist.setText(ARTIST_TO_FRAG);
                    }
                }
            }
        });
        prevBtn.setOnClickListener(v -> {
//                Toast.makeText(getContext(), "Previous", Toast.LENGTH_SHORT).show();
            if (musicService != null){
                musicService.prevBtnClicked();
                if (getActivity() != null) {
//                        SharedPreferences.Editor editor = getActivity().getSharedPreferences(MUSIC_LAST_PLAYED, MODE_PRIVATE).edit();
//                        editor.putString(MUSIC_FILE, musicService.musicFiles.get(musicService.position).getPath());
//                        editor.putString(ARTIST_NAME, musicService.musicFiles.get(musicService.position).getArtist());
//                        editor.putString(SONG_NAME, musicService.musicFiles.get(musicService.position).getTitle());
//                        editor.apply();
                    SharedPreferences preferences = getActivity().getSharedPreferences(MUSIC_LAST_PLAYED, MODE_PRIVATE);
                    String path = preferences.getString(MUSIC_FILE, null);
                    String artistName = preferences.getString(ARTIST_NAME, null);
                    String  song_name = preferences.getString(SONG_NAME, null);
                    if (path != null) {
                        SHOW_MINI_PLAYER = true;
                        PATH_TO_FRAG = path;
                        ARTIST_TO_FRAG = artistName;
                        SONG_NAME_TO_FRAG = song_name;
                    }
                    else{
                        SHOW_MINI_PLAYER = false;
                        PATH_TO_FRAG = null;
                        ARTIST_TO_FRAG = null;
                        SONG_NAME_TO_FRAG = null;
                    }
                    if (SHOW_MINI_PLAYER){
                        byte[] art = getAlbumArt(PATH_TO_FRAG);
                        if (art != null){
                            Glide.with(requireContext()).load(art)
                                    .into(albumArt);
                        }
                        else{
                            Glide.with(requireContext()).load(R.drawable.musicicon)
                                    .into(albumArt);
                        }
                        songName.setText(SONG_NAME_TO_FRAG);
                        artist.setText(ARTIST_TO_FRAG);
                    }
                }
            }
        });
        playPauseBtn.setOnClickListener(v -> {
//                Toast.makeText(getContext(), "PlayPause", Toast.LENGTH_SHORT).show();
            musicService.playPauseBtnClicked();
//                if (musicService.isPlaying()){
//                    playPauseBtn.setImageResource(R.drawable.ic_pause);
//                }
//                else{
//                    playPauseBtn.setImageResource(R.drawable.ic_play);
//                }
        });

        view.setOnClickListener(v -> {

//                Toast.makeText(musicService, "Tap on MiniPlayer", Toast.LENGTH_SHORT).show();
//                Intent albumIntent = new Intent(getContext(), PlayerActivity.class);
//                albumIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                startActivity(albumIntent);
        });
        return view;
    }

    public static void setLayoutInvisible() {
        if (bottom_bac_frag.getVisibility() == View.VISIBLE) {
            bottom_bac_frag.setVisibility(View.GONE);
        }
    }
    public static void setLayoutVisible() {
        if (bottom_bac_frag.getVisibility() == View.GONE) {
            bottom_bac_frag.setVisibility(View.VISIBLE);
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        if (SHOW_MINI_PLAYER){
            if (PATH_TO_FRAG != null){
                byte[] art = getAlbumArt(PATH_TO_FRAG);
                if (art != null){
                    Glide.with(requireContext()).load(art)
                            .into(albumArt);
                }
                else{
                    Glide.with(requireContext()).load(R.drawable.musicicon)
                            .into(albumArt);
                }
                songName.setText(SONG_NAME_TO_FRAG);
                artist.setText(ARTIST_TO_FRAG);
                Intent intent = new Intent(getContext(), MusicService.class);
                if(getContext() != null){
                    getContext().bindService(intent, this, Context.BIND_AUTO_CREATE);
                    bindservice = true;
                }
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getContext() != null){
            if(bindservice) {
                getContext().unbindService(this);
            }
        }
    }

    private byte[] getAlbumArt(String uri){
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(uri);
        byte[] art = retriever.getEmbeddedPicture();
        retriever.release();
        return art;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        MusicService.MyBinder binder = (MusicService.MyBinder) service;
        musicService = binder.getService();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        musicService = null;
    }
}