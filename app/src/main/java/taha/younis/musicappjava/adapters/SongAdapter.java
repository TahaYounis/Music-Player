package taha.younis.musicappjava.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.MediaMetadata;
import com.google.android.exoplayer2.Player;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import taha.younis.musicappjava.R;
import taha.younis.musicappjava.data.Song;

public class SongAdapter extends RecyclerView.Adapter <RecyclerView.ViewHolder> {

    Context context;
    List <Song> songs;
    private RecyclerView_InterFace recyclerView_interFace;

    public SongAdapter(Context context, List <Song> songs, RecyclerView_InterFace recyclerView_interFace) {
        this.context = context;
        this.songs = songs;
        this.recyclerView_interFace = recyclerView_interFace;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_list, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        Song song = songs.get(position);
        SongViewHolder viewHolder = (SongViewHolder) holder;

        viewHolder.title.setText(song.getTitle());
        viewHolder.duration.setText(getDuration(song.getDuration()));
        viewHolder.size.setText(getSize(song.getSize()));

        Glide.with(context).load(song.getAlbumUriForImage()).placeholder(R.drawable.img_music).centerCrop().into(viewHolder.artworkImg);

        viewHolder.itemView.setOnClickListener(v -> {
            recyclerView_interFace.onItemClick(position);
        });
    }

    public static class SongViewHolder extends RecyclerView.ViewHolder {

        ImageView artworkImg;
        TextView title, duration, size;

        public SongViewHolder(@NonNull View itemView) {
            super(itemView);

            artworkImg = itemView.findViewById(R.id.imgArtwork);
            title = itemView.findViewById(R.id.tvTitle);
            duration = itemView.findViewById(R.id.tvDuration);
            size = itemView.findViewById(R.id.tvSize);

        }
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    // filter songs / search results
    @SuppressLint("NotifyDataSetChanged")
    public void filterSongs(List <Song> filteredList) {
        songs = filteredList;
        notifyDataSetChanged();
    }

    @SuppressLint("DefaultLocale")
    private String getDuration(int totalDuration) {
        String totalDurationTxt;

        int hrs = totalDuration / (1000 * 60 * 60);
        int min = (totalDuration % (1000 * 60 * 60)) / (1000 * 60);
        int secs = (((totalDuration % (1000 * 60 * 60)) % (1000 * 60 * 60)) % (1000 * 60)) / 1000;

        if (hrs < 1)
            totalDurationTxt = String.format("%02d:%02d", min, secs);
        else
            totalDurationTxt = String.format("%1d:%02d:%02d", hrs, min, secs);

        return totalDurationTxt;
    }

    private String getSize(long bytes) {
        String hrSizes;

        double k = bytes / 1024.0;
        double m = ((bytes / 1024.0) / 1024.0);
        double g = (((bytes / 1024.0) / 1024.0) / 1024.0);
        double t = ((((bytes / 1024.0) / 1024.0) / 1024.0) / 1024.0);

        DecimalFormat dec = new DecimalFormat("0.00");
        if (t > 1)
            hrSizes = dec.format(t).concat(" TB");
        else if (g > 1)
            hrSizes = dec.format(g).concat(" GB");
        else if (m > 1)
            hrSizes = dec.format(m).concat(" MB");
        else if (k > 1)
            hrSizes = dec.format(k).concat(" KB");
        else
            hrSizes = dec.format(g).concat(" Byte");

        return hrSizes;
    }
}
