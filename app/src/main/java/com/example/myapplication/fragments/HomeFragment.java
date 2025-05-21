package com.example.myapplication.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.MainActivity;
import com.example.myapplication.R;
import com.example.myapplication.adapters.WatchedTorrentAdapter;
import com.example.myapplication.adapters.WatchLaterAdapter;
import com.example.myapplication.models.WatchedTorrent;
import com.example.myapplication.models.WatchLaterTorrent;
import com.example.myapplication.services.TorrentDownloadService;
import com.example.myapplication.utils.TorrentActionDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private RecyclerView rvContinueWatching;
    private RecyclerView rvWatchLater;
    private ProgressBar progressBar;
    private WatchedTorrentAdapter watchedAdapter;
    private WatchLaterAdapter watchLaterAdapter;
    private final List<WatchedTorrent> watchedTorrents = new ArrayList<>();
    private final List<WatchLaterTorrent> watchLaterTorrents = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize views
        rvContinueWatching = view.findViewById(R.id.rv_continue_watching);
        rvWatchLater = view.findViewById(R.id.rv_watch_later);
        progressBar = view.findViewById(R.id.progress_bar);

        // Setup Continue Watching RecyclerView
        watchedAdapter = new WatchedTorrentAdapter(requireContext(), watchedTorrents, this::handleWatchedTorrentClick);
        rvContinueWatching.setLayoutManager(new LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false
        ));
        rvContinueWatching.setAdapter(watchedAdapter);

        // Setup Watch Later RecyclerView
        watchLaterAdapter = new WatchLaterAdapter(requireContext(), watchLaterTorrents, this::handleWatchLaterTorrentClick);
        rvWatchLater.setLayoutManager(new LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false
        ));
        rvWatchLater.setAdapter(watchLaterAdapter);

        // Load data
        loadWatchedContent();
        loadWatchLaterContent();

        return view;
    }

    private void handleWatchedTorrentClick(WatchedTorrent torrent) {
        showTorrentActionDialog(torrent);
    }

    private void handleWatchLaterTorrentClick(WatchLaterTorrent torrent) {
        showTorrentActionDialog(torrent);
    }

    private void showTorrentActionDialog(WatchedTorrent torrent) {
        TorrentActionDialog dialog = new TorrentActionDialog(requireContext(), torrent.getTitle(),
            new TorrentActionDialog.ActionListener() {
                @Override
                public void onWatchClick() {
                    if (getActivity() instanceof MainActivity) {
                        MainActivity activity = (MainActivity) getActivity();
                        String magnetLink = torrent.getMagnetLink();
                        if (magnetLink != null && !magnetLink.isEmpty()) {
                            activity.startTorrentStream(magnetLink);
                        } else if (torrent.getInfoHash() != null) {
                            String constructedMagnetLink = "magnet:?xt=urn:btih:" + torrent.getInfoHash();
                            activity.startTorrentStream(constructedMagnetLink);
                        }
                    }
                }

                @Override
                public void onDownloadClick() {
                    Intent serviceIntent = new Intent(requireContext(), TorrentDownloadService.class);
                    serviceIntent.putExtra("infoHash", torrent.getInfoHash());
                    serviceIntent.putExtra("torrentLink", torrent.getTorrentLink());
                    serviceIntent.putExtra("title", torrent.getTitle());
                    serviceIntent.putExtra("detailPage", torrent.getWebsite());
                    serviceIntent.putExtra("action", "Download");
                    requireContext().startService(serviceIntent);
                }

                @Override
                public void onWatchLaterClick() {
                    // Already in watch later, no need to implement
                }
            });
        dialog.show();
    }

    private void showTorrentActionDialog(WatchLaterTorrent torrent) {
        TorrentActionDialog dialog = new TorrentActionDialog(requireContext(), torrent.getTitle(),
            new TorrentActionDialog.ActionListener() {
                @Override
                public void onWatchClick() {
                    if (getActivity() instanceof MainActivity) {
                        MainActivity activity = (MainActivity) getActivity();
                        String magnetLink = torrent.getMagnetLink();
                        if (magnetLink != null && !magnetLink.isEmpty()) {
                            activity.startTorrentStream(magnetLink);
                        } else if (torrent.getInfoHash() != null) {
                            String constructedMagnetLink = "magnet:?xt=urn:btih:" + torrent.getInfoHash();
                            activity.startTorrentStream(constructedMagnetLink);
                        }
                    }
                }

                @Override
                public void onDownloadClick() {
                    Intent serviceIntent = new Intent(requireContext(), TorrentDownloadService.class);
                    serviceIntent.putExtra("infoHash", torrent.getInfoHash());
                    serviceIntent.putExtra("torrentLink", torrent.getTorrentLink());
                    serviceIntent.putExtra("title", torrent.getTitle());
                    serviceIntent.putExtra("detailPage", torrent.getWebsite());
                    serviceIntent.putExtra("action", "Download");
                    requireContext().startService(serviceIntent);
                }

                @Override
                public void onWatchLaterClick() {
                    // Already in watch later, no need to implement
                }
            });
        dialog.show();
    }

    private void loadWatchedContent() {
        progressBar.setVisibility(View.VISIBLE);

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance().collection("watched_torrents")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<WatchedTorrent> torrents = queryDocumentSnapshots.toObjects(WatchedTorrent.class);
                    watchedAdapter.updateData(torrents);
                    progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading watched content", e);
                    progressBar.setVisibility(View.GONE);
                });
    }

    private void loadWatchLaterContent() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance().collection("watch_later_torrents")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<WatchLaterTorrent> torrents = queryDocumentSnapshots.toObjects(WatchLaterTorrent.class);
                    watchLaterAdapter.updateData(torrents);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading watch later content", e);
                });
    }
}