package com.thapamusic.wetunes;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

import static com.thapamusic.wetunes.MainActivity.musicFiles;

public class AlbumDetails extends AppCompatActivity {

    RecyclerView recyclerView;
    ImageView albumPhoto;
    TextView passAlbumName, passArtistName;
    String albumName, artistName;
    ArrayList<MusicFiles> albumSongs = new ArrayList<>();
    AlbumDetailsAdapter albumDetailsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album_details);
        recyclerView = findViewById(R.id.recyclerView);
        albumPhoto = findViewById(R.id.albumPhoto);
        passArtistName = (TextView) findViewById(R.id.art_name);
        passAlbumName = (TextView) findViewById(R.id.alb_name);
        artistName = getIntent().getStringExtra("artistName");
        albumName = getIntent().getStringExtra("albumName");
        passAlbumName.setText(albumName);
        passArtistName.setText(artistName);
        int j = 0;
        for (int i = 0 ; i < musicFiles.size() ; i ++) {
            if (albumName.equals(musicFiles.get(i).getAlbum())){
                albumSongs.add(j, musicFiles.get(i));
                j++;
            }
        }
        byte[] image = getAlbumArt(albumSongs.get(0).getPath());
        if (image != null){
            Glide.with(this)
                    .load(image)
                    .into(albumPhoto);
        }
        else {
            Glide.with(this)
                    .load(R.drawable.musicicon)
                    .into(albumPhoto);
        }

    }

    @Override
    public void onBackPressed() {

        int count = getSupportFragmentManager().getBackStackEntryCount();

        if (count == 0) {
            super.onBackPressed();
//                Intent intent_D = new Intent(getApplicationContext(), MainActivity.class);
//                intent_D.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                startActivity(intent_D);
            finish();
        } else {
            getSupportFragmentManager().popBackStack();
            getSupportFragmentManager().popBackStackImmediate();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!(albumSongs.size() < 1)){
            albumDetailsAdapter = new AlbumDetailsAdapter(this, albumSongs);
            recyclerView.setAdapter(albumDetailsAdapter);
            recyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL,false));
        }
    }

    private byte[] getAlbumArt(String uri){
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(uri);
        byte[] art = retriever.getEmbeddedPicture();
        retriever.release();
        return art;
    }
}