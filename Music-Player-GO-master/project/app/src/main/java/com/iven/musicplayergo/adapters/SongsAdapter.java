package com.iven.musicplayergo.adapters;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.iven.musicplayergo.R;
import com.iven.musicplayergo.Utils;
import com.iven.musicplayergo.models.Album;
import com.iven.musicplayergo.models.Song;

import java.util.List;

public class SongsAdapter extends RecyclerView.Adapter<SongsAdapter.SimpleViewHolder> {

    private final SongSelectedListener mSongSelectedListener;
    private final Activity mActivity;
    private List<Song> mSongs;
    private Album mAlbum;

    public SongsAdapter(@NonNull final Activity activity, @NonNull final Album album) {
        mActivity = activity;
        mAlbum = album;
        mSongs = mAlbum.songs;
        mSongSelectedListener = (SongSelectedListener) activity;
    }

    public void swapSongs(@NonNull final Album album) {
        mAlbum = album;
        mSongs = mAlbum.songs;
        notifyDataSetChanged();
    }

    @Override
    @NonNull
    public SimpleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        final View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.song_item, parent, false);

        return new SimpleViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull SimpleViewHolder holder, int position) {

        final Song song = mSongs.get(holder.getAdapterPosition());
        final String songTitle = song.title;

        final int songTrack = Song.formatTrack(song.trackNumber);
        final Spanned spanned = Utils.buildSpanned(mActivity.getString(R.string.track_title, songTrack, songTitle));
        holder.trackTitle.setText(spanned);
        holder.duration.setText(Song.formatDuration(song.duration));
    }

    @Override
    public int getItemCount() {
        return mSongs.size();
    }

    public interface SongSelectedListener {
        void onSongSelected(@NonNull final Song song, @NonNull final List<Song> songs);
    }

    class SimpleViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        final TextView trackTitle, duration;

        SimpleViewHolder(@NonNull final View itemView) {
            super(itemView);

            trackTitle = itemView.findViewById(R.id.track_title);
            duration = itemView.findViewById(R.id.duration);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            final Song song = mSongs.get(getAdapterPosition());
            mSongSelectedListener.onSongSelected(song, mSongs);
        }
    }
}