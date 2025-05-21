package com.example.myapplication.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.models.WatchedTorrent;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class WatchedTorrentAdapter extends RecyclerView.Adapter<WatchedTorrentAdapter.ViewHolder> {

    private final List<WatchedTorrent> torrents;
    private final Context context;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(WatchedTorrent torrent);
    }

    public WatchedTorrentAdapter(Context context, List<WatchedTorrent> torrents, OnItemClickListener listener) {
        this.context = context;
        this.torrents = torrents;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_media, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WatchedTorrent torrent = torrents.get(position);

        holder.title.setText(torrent.getTitle());
        holder.website.setText(torrent.getWebsite());
        holder.torrentLink.setText(torrent.getTorrentLink());
        holder.infoHash.setText(torrent.getInfoHash());
        holder.size.setText(torrent.getSize());

        // Format timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault());
        String formattedDate = sdf.format(torrent.getTimestamp().toDate());
        holder.timestamp.setText(formattedDate);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(torrent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return torrents.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, website, torrentLink, infoHash, timestamp, size;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tv_title);
            website = itemView.findViewById(R.id.tv_website);
            torrentLink = itemView.findViewById(R.id.tv_torrent_link);
            infoHash = itemView.findViewById(R.id.tv_info_hash);
            timestamp = itemView.findViewById(R.id.tv_timestamp);
            size = itemView.findViewById(R.id.tv_size);

        }
    }

    public void updateData(List<WatchedTorrent> newTorrents) {
        torrents.clear();
        torrents.addAll(newTorrents);
        notifyDataSetChanged();
    }
}