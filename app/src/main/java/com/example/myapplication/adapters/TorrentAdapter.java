package com.example.myapplication.adapters;

import static com.github.se_bastiaan.torrentstream.utils.ThreadUtils.runOnUiThread;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.MainActivity;
import com.example.myapplication.R;
import com.example.myapplication.models.WatchedTorrent;
import com.example.myapplication.parsing.SearchResult;
import com.example.myapplication.services.TorrentDownloadService;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.List;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class TorrentAdapter extends RecyclerView.Adapter<TorrentAdapter.ViewHolder> {

    private static final String TAG = "TorrentAdapter";
    private final List<SearchResult> results;
    private final Context context;
    private final RecyclerviewListener listener;

    private static final String COLLECTION_WATCHED = "watched_torrents";
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_INFOHASH = "infoHash";
    private static final String FIELD_TIMESTAMP = "timestamp";
    private static final String FIELD_USERID = "userId";
    private static final String FIELD_MAGNET = "magnetLink";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();

    public interface RecyclerviewListener {
        void onItemClick(int position);
    }

    public TorrentAdapter(Context context, RecyclerviewListener listener, List<SearchResult> results) {
        this.context = context;
        this.listener = listener;
        this.results = results;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_torrent, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SearchResult result = results.get(position);
        holder.title.setText(result.getTitle());
        holder.seeds.setText(result.getSeeds());
        holder.leeches.setText(result.getLeeches());
        holder.size.setText(result.getSize());
        holder.website.setText(result.getWebsite());
        holder.link.setText(result.getLink());
        holder.infohash.setText(result.getInfoHash());

        holder.itemView.setOnClickListener(v -> {
            Log.d("TorrentAdapter", "Torrent clicked: " + result.getTitle());

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(result.getTitle())
                    .setItems(new String[]{"Watch", "Download"}, (dialog, which) -> {
                        if (which == 0) { // Watch action
                            handleWatchAction(result);
                        } else { // Download action
                            handleDownloadAction(result);
                        }
                    })
                    .show();
        });
    }


    private void saveToFirestore(SearchResult result, String magnetLink) {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (userId == null) {
            Toast.makeText(context, "You must be logged in to save progress", Toast.LENGTH_SHORT).show();
            return;
        }

        WatchedTorrent torrent = new WatchedTorrent(
                result.getTitle(),
                result.getInfoHash(),
                magnetLink,
                result.getLink(),       // torrentLink
                result.getWebsite(),    // website
                com.google.firebase.Timestamp.now(),
                userId,
                result.getSize()
        );

        db.collection(COLLECTION_WATCHED)
                .add(torrent)
                .addOnSuccessListener(documentReference ->
                        Log.d(TAG, "Torrent saved with ID: " + documentReference.getId()))
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error saving torrent", e);
                    runOnUiThread(() ->
                            Toast.makeText(context, "Failed to save progress", Toast.LENGTH_SHORT).show());
                });
    }



    private void handleWatchAction(SearchResult result) {
        if (result.getInfoHash() != null) {
            String magnetLink = "magnet:?xt=urn:btih:" + result.getInfoHash();
            saveToFirestore(result, magnetLink);
            startStream(magnetLink);
        } else {
            new Thread(() -> {
                try {
                    Log.d(TAG, "Fetching infohash for: " + result.getLink());
                    Document doc = Jsoup.connect(result.getLink())
                            .userAgent("Mozilla/5.0")
                            .timeout(15000)
                            .get();

                    // 1337x.to specific infohash location
                    Element infohashElement = doc.selectFirst("div.infohash-box span");
                    if (infohashElement != null) {
                        String infohash = infohashElement.text();
                        String magnetLink = "magnet:?xt=urn:btih:" + infohash;
                        saveToFirestore(result, magnetLink);
                        Log.d(TAG, "Successfully fetched infohash: " + infohash);
                        startStream(magnetLink);
                    } else {
                        Log.e(TAG, "Infohash element not found");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error fetching infohash", e);
                    runOnUiThread(() ->
                            Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }).start();
        }
    }

    private void startStream(String magnetLink) {
        runOnUiThread(() -> {
            if (!magnetLink.isEmpty() && context instanceof MainActivity) {
                ((MainActivity) context).startTorrentStream(magnetLink);
            } else {
                Toast.makeText(context, "Could not get torrent link", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void handleDownloadAction(SearchResult result) {
        Intent serviceIntent = new Intent(context, TorrentDownloadService.class);
        serviceIntent.putExtra("infoHash", result.getInfoHash());
        serviceIntent.putExtra("torrentLink", result.getLink());
        serviceIntent.putExtra("title", result.getTitle());
        serviceIntent.putExtra("detailPage", result.getWebsite());
        serviceIntent.putExtra("action", "Download");
        context.startService(serviceIntent);
    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, seeds, leeches, size, website, link, infohash;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.text_title);
            seeds = itemView.findViewById(R.id.text_seeds);
            leeches = itemView.findViewById(R.id.text_leeches);
            size = itemView.findViewById(R.id.text_size);
            website = itemView.findViewById(R.id.text_website);
            link = itemView.findViewById(R.id.text_link);
            infohash = itemView.findViewById(R.id.text_infohash);
        }
    }
}