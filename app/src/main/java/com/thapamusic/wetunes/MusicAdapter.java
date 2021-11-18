package com.thapamusic.wetunes;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;

import static android.content.Context.MODE_PRIVATE;
import static com.thapamusic.wetunes.NowPlayingFragmentBottom.ARTIST_NAME;
import static com.thapamusic.wetunes.NowPlayingFragmentBottom.MUSIC_FILE;
import static com.thapamusic.wetunes.NowPlayingFragmentBottom.MUSIC_LAST_PLAYED;
import static com.thapamusic.wetunes.NowPlayingFragmentBottom.SONG_NAME;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.MyVieHolder> {

    private Context mContext;
    static ArrayList<MusicFiles> mFiles;
//    static ArrayList<MusicFiles> mFiles = new ArrayList<MusicFiles>();

    MusicAdapter(Context mContext, ArrayList<MusicFiles> mFiles){
        this.mFiles = mFiles;
        this.mContext = mContext;

    }


    @NonNull
    @NotNull
    @Override
    public MusicAdapter.MyVieHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.music_items, parent, false);
        return new MyVieHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull MusicAdapter.MyVieHolder holder, int position) {
        holder.file_name.setText(mFiles.get(position).getTitle());
        holder.artist_name.setText(mFiles.get(position).getArtist());
        int totalDuration = Integer.parseInt(mFiles.get(position).getDuration()) / 1000;
        holder.duration.setText(formatTime(totalDuration));
        byte[] image = getAlbumArt(mFiles.get(position).getPath());
        if (image != null){
            Glide.with(mContext).asBitmap()
                    .load(image)
                    .into(holder.album_art);
        }
        else {
            Glide.with(mContext)
                    .load(R.drawable.musicicon)
                    .into(holder.album_art);
        }
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Boolean isPlayerActivityRun = mContext.getSharedPreferences("PreferencePlayerActivity", MODE_PRIVATE).getBoolean("isPlayerActivityFirstrun", true);
//                if (isPlayerActivityRun){
//                    NowPlayingFragmentBottom.setLayoutVisible();
////            getSharedPreferences("PreferencePlayerActivity", MODE_PRIVATE).edit().putBoolean("isPlayerActivityFirstrun", false).apply();
//                }
//                SharedPreferences.Editor editor = mContext.getSharedPreferences(MUSIC_LAST_PLAYED, MODE_PRIVATE).edit();
//                editor.putString(MUSIC_FILE, mFiles.get(position).getPath());
//                editor.putString(ARTIST_NAME, mFiles.get(position).getArtist());
//                editor.putString(SONG_NAME, mFiles.get(position).getTitle());
//                editor.apply();
//                if (image != null){
//                    Glide.with(mContext).load(getAlbumArt(mFiles.get(position).getPath()))
//                            .into(NowPlayingFragmentBottom.albumArt);
//                }else{
////                Toast.makeText(musicService, "Art is NULL!!!", Toast.LENGTH_SHORT).show();
//                }
//                NowPlayingFragmentBottom.songName.setText(mFiles.get(position).getTitle());
//                NowPlayingFragmentBottom.artist.setText(mFiles.get(position).getArtist());
                Intent intent = new Intent(mContext, PlayerActivity.class);
                intent.putExtra("musicAdapter", "MusicAdapt");
                intent.putExtra("positionMfiles", position);
                mContext.startActivity(intent);
                NowPlayingFragmentBottom.playPauseBtn.setImageResource(R.drawable.ic_pause);
            }
        });
        holder.menuMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popupMenu = new PopupMenu(mContext, v);
                popupMenu.getMenuInflater().inflate(R.menu.popup, popupMenu.getMenu());
                popupMenu.show();
                popupMenu.setOnMenuItemClickListener((item) ->{
                    switch (item.getItemId()){
                        case R.id.delete:
                            Toast.makeText(mContext, "Song Deleted!", Toast.LENGTH_SHORT).show();
                            deleteFile(position, v);
                            break;
                    }
                    return true;
                });
            }
        });
    }

    private String formatTime(int mCurrentPosition) {
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

    private void deleteFile(int position, View v){
        Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, Long.parseLong(mFiles.get(position).getId())); //content: //
        File file = new File(mFiles.get(position).getPath());
        boolean deleted = file.delete(); //delete your file
        if (deleted) {
            mContext.getContentResolver().delete(contentUri, null, null);
            mFiles.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, mFiles.size());
            Snackbar.make(v, "File Deleted : ", Snackbar.LENGTH_LONG)
                    .show();
        }else{
//            may be file in sd card of api level is 19 or more
            Snackbar.make(v, "File cannot be deleted.", Snackbar.LENGTH_LONG)
                    .show();
        }
    }

    @Override
    public int getItemCount() {
        return mFiles.size();
    }


    public class MyVieHolder extends RecyclerView.ViewHolder{

        TextView file_name, artist_name, duration;
        ImageView album_art, menuMore;

        public MyVieHolder(@NonNull @NotNull View itemView) {
            super(itemView);
            file_name = itemView.findViewById(R.id.music_file_name);
            artist_name = itemView.findViewById(R.id.music_artist_name);
            duration = itemView.findViewById(R.id.duration);
            album_art = itemView.findViewById(R.id.music_img);
            menuMore = itemView.findViewById(R.id.menuMore);

        }
    }

    private byte[] getAlbumArt(String uri){
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(uri);
        byte[] art = retriever.getEmbeddedPicture();
        retriever.release();
        return art;
    }

    void updateList(ArrayList<MusicFiles> musicFilesArrayList){
        mFiles = new ArrayList<>();
        mFiles.addAll(musicFilesArrayList);
        notifyDataSetChanged();
    }
}
