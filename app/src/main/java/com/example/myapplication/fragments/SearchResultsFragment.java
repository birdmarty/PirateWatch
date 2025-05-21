package com.example.myapplication.fragments;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.MainActivity;
import com.example.myapplication.R;
import com.example.myapplication.adapters.TorrentAdapter;
import com.example.myapplication.parsing.SearchResult;
import com.example.myapplication.parsing.SortList;
import com.github.se_bastiaan.torrentstream.StreamStatus;
import com.github.se_bastiaan.torrentstream.Torrent;
import com.github.se_bastiaan.torrentstream.TorrentOptions;
import com.github.se_bastiaan.torrentstream.TorrentStream;
import com.github.se_bastiaan.torrentstream.listeners.TorrentListener;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class SearchResultsFragment extends Fragment implements TorrentAdapter.RecyclerviewListener, TorrentListener {

    public TorrentStream torrentStream;
    private static final String TAG = "SearchResultsFragment";
    private static TorrentAdapter torrentAdapter;
    private static final ArrayList<SearchResult> searchResults = new ArrayList<>();
    private String infohash;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_search_result, container, false);

        RecyclerView recyclerView = rootView.findViewById(R.id.recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        torrentAdapter = new TorrentAdapter(getContext(),this, searchResults);
        recyclerView.setAdapter(torrentAdapter);

        String keyword = MainActivity.getKeyword();
        String sortItem = MainActivity.getSortItem();
        new LoadSearchResultsTask().execute(new SortList(sortItem).urlSortSearch(keyword));

        return rootView;
    }

    public void initTorrentStream() {
        try {
            // Create download directory
            File saveLocation = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "TorrentStream");
            boolean dirCreated = saveLocation.mkdirs();
            Log.d(TAG, "Directory created: " + dirCreated + ", Path: " + saveLocation.getAbsolutePath());

            // Initialize TorrentStream with options
            TorrentOptions torrentOptions = new TorrentOptions.Builder()
                    .saveLocation(saveLocation)
                    .removeFilesAfterStop(false)
                    .maxConnections(0)
                    .maxDownloadSpeed(0) // No limit
                    .maxUploadSpeed(0) // No limit
                    .build();

            torrentStream = TorrentStream.init(torrentOptions);

            if (torrentStream != null) {
                torrentStream.addListener((TorrentListener) this);
//                Toast.makeText(getContext(), "TorrentStream initialized successfully", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "TorrentStream initialized successfully");
            } else {
//                Toast.makeText(getContext(), "Failed to initialize TorrentStream", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "TorrentStream initialization returned null");
            }
        } catch (Exception e) {
            String errorMsg = "Error initializing TorrentStream: " + e.getMessage();
//            Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
            Log.e(TAG, errorMsg, e);
        }
    }



    @Override
    public void onItemClick(int position) {

//  when download reaches 20% show a pop up
//      1. open GOM player and start streaming
//      2. continue downloading


//        SearchResult result = searchResults.get(position);
//        // Start the async task to fetch the magnet link
//        new FetchMagnetLinkTask().execute(result.getLink());

        SearchResult result = searchResults.get(position);
        String torrentLink = "https://itorrents.org/torrent/" + result.getInfoHash() + ".torrent";
        Log.d(TAG, torrentLink);

        // Initialize TorrentStream with options
        initTorrentStream();
        try{
            Log.d(TAG, "Starting torrent stream with URL: " + torrentLink);
            torrentStream.startStream(torrentLink);
        } catch (Exception e) {
            String errorMsg = "Error starting download: " + e.getMessage();
            Log.e(TAG, errorMsg, e);
        }
    }

    @Override
    public void onStreamPrepared(Torrent torrent) {

    }

    @Override
    public void onStreamStarted(Torrent torrent) {

    }

    @Override
    public void onStreamError(Torrent torrent, Exception e) {

    }

    @Override
    public void onStreamReady(Torrent torrent) {

    }

    @Override
    public void onStreamProgress(Torrent torrent, StreamStatus status) {

    }

    @Override
    public void onStreamStopped() {

    }

//    private void copyToClipboard(String magnetLink) {
//        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
//        ClipData clip = ClipData.newPlainText("Magnet Link", magnetLink);
//        clipboard.setPrimaryClip(clip);
//        Toast.makeText(getContext(), "Magnet link copied to clipboard", Toast.LENGTH_SHORT).show();
//    }

    // New AsyncTask for fetching magnet links
//    private class FetchMagnetLinkTask extends AsyncTask<String, Void, String> {
//        @Override
//        protected void onPreExecute() {
//            // Show a loading indicator if you want
//            Toast.makeText(getContext(), "Fetching magnet link...", Toast.LENGTH_SHORT).show();
//        }

//        @Override
//        protected String doInBackground(String... params) {
//            try {
//                Document document = Jsoup.connect(params[0]).get();
//                Elements elements = document.getElementsByClass("infohash-box").select("span");
//                if (!elements.isEmpty()) {
//                    return "magnet:?xt=urn:btih:" + elements.text();
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            return null;
//        }

//        @Override
//        protected void onPostExecute(String magnetLink) {
//            if (magnetLink != null) {
//                copyToClipboard(magnetLink);
//            } else {
//                Toast.makeText(getContext(), "Failed to fetch magnet link", Toast.LENGTH_SHORT).show();
//            }
//        }
//    }

    public static class LoadSearchResultsTask extends AsyncTask<String, Void, ArrayList<SearchResult>> {
        @Override
        protected ArrayList<SearchResult> doInBackground(String... params) {
            ArrayList<SearchResult> results = new ArrayList<>();
            try {
                Document document = Jsoup.connect(params[0]).get();
                Elements rows = document.select("tbody > tr");
                for (Element row : rows) {
                    Element titleLink = row.select("td.coll-1 a:not(.icon)").first();
                    String title = titleLink.text();
                    String seeds = row.select("td:nth-child(2)").text();
                    String leeches = row.select("td:nth-child(3)").text();
                    String size = row.select("td.coll-4").first().ownText();
                    String link = "https://www.1337x.to" + titleLink.attr("href");
                    String infohash = fetchInfohash(link);

                    results.add(new SearchResult(title, "Seeds: " + seeds, "Leeches: " + leeches, "Size: " + size, "1337x", link, infohash));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return results;
        }

        public String fetchInfohash(String link) {
            try{
                Document torrentPage = Jsoup.connect(link).get();
                Element infohashElement = torrentPage.select("div.infohash-box span").first();
                if (infohashElement!= null) {
                    return infohashElement.text();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null; // Return null if the infohash is not found
        }

        @Override
        protected void onPostExecute(ArrayList<SearchResult> results) {
            searchResults.clear();
            searchResults.addAll(results);
            torrentAdapter.notifyDataSetChanged();
        }
    }
}
