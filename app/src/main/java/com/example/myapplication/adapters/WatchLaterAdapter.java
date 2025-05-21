package com.example.myapplication.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.models.WatchLaterTorrent;

import java.util.List;

public class WatchLaterAdapter extends RecyclerView.Adapter<WatchLaterAdapter.ViewHolder> {
    private final List<WatchLaterTorrent> torrents;
    private final Context context;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(WatchLaterTorrent torrent);
    }

    public WatchLaterAdapter(Context context, List<WatchLaterTorrent> torrents, OnItemClickListener listener) {
        this.context = context;
        this.torrents = torrents;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_torrent_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WatchLaterTorrent torrent = torrents.get(position);
        holder.titleView.setText(torrent.getTitle());
        holder.sizeView.setText(torrent.getSize());
        holder.itemView.setOnClickListener(v -> listener.onItemClick(torrent));
    }

    @Override
    public int getItemCount() {
        return torrents.size();
    }

    public void updateData(List<WatchLaterTorrent> newTorrents) {
        torrents.clear();
        torrents.addAll(newTorrents);
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView titleView;
        final TextView sizeView;

        ViewHolder(View itemView) {
            super(itemView);
            titleView = itemView.findViewById(R.id.text_title);
            sizeView = itemView.findViewById(R.id.text_size);
        }
    }
} 