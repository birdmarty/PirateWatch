package com.example.myapplication.repository;

import android.os.AsyncTask;
import android.util.Log;


import com.example.myapplication.parsing.SearchResult;
import com.example.myapplication.parsing.Url;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
public class TorrentRepository {
    private static final String TAG = "TorrentRepo";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36";
    private static final int TIMEOUT = 15000;

    public interface TorrentListener {
        void onResultsReady(ArrayList<SearchResult> results);
        void onError(String message);
    }

    public void getTrendingTorrents1337x(TorrentListener listener) {
        new TorrentTask(listener, "1337x").execute(Url.UrlOneThree + "/trending");
    }

    public void getTrendingTorrentsLimeTorrents(TorrentListener listener) {
        new TorrentTask(listener, "LimeTorrentsTrending").execute(Url.UrlLimeTorrent + "top100");
    }

    public void searchTorrents(String searchUrl, String site, TorrentListener listener) {
        new TorrentTask(listener, site).execute(searchUrl);
    }

    private static class TorrentTask extends AsyncTask<String, Void, ArrayList<SearchResult>> {
        private final TorrentListener listener;
        private final String site;

        TorrentTask(TorrentListener listener, String site) {
            this.listener = listener;
            this.site = site;
        }

        @Override
        protected ArrayList<SearchResult> doInBackground(String... urls) {
            ArrayList<SearchResult> results = new ArrayList<>();
            try {
                Document doc = Jsoup.connect(urls[0])
                        .userAgent(USER_AGENT)
                        .timeout(TIMEOUT)
                        .ignoreHttpErrors(true)
                        .get();

                if (site.equals("1337x")) {
                    // 1337x parsing logic
                    Elements rows = doc.select("tbody tr");
                    for (Element row : rows) {
                        Element titleLink = row.select("td.coll-1 a:not(.icon)").first();
                        if (titleLink == null) continue;

                        String title = titleLink.text();
                        String seeds = row.select("td:nth-child(2)").text();
                        String leeches = row.select("td:nth-child(3)").text();
                        String size = row.select("td.coll-4").first().ownText();
                        String link = Url.UrlOneThree + titleLink.attr("href");

                        results.add(new SearchResult(
                                title, "Seeds: " + seeds, "Leeches: " + leeches,
                                "Size: " + size, "1337x", link, null
                        ));
                    }
                } else if (site.equals("LimeTorrents")) {
                    // LimeTorrents parsing logic for both search and trending
                    Elements items = doc.select("table.table2 tr:not(:has(th))"); // Skip header rows
                    for (Element item : items) {
                        try {
                            Element titleElement = item.selectFirst("td.tdleft a:not([rel=nofollow])");
                            if (titleElement == null) continue;

                            // Extract title and link
                            String title = titleElement.text();
                            String relativeLink = titleElement.attr("href");
                            String link = Url.UrlLimeTorrent + relativeLink.substring(1); // Remove leading /

                            // Extract other details
                            Elements tdNormals = item.select("td.tdnormal");
                            String category = tdNormals.size() > 0 ? tdNormals.get(0).text() : "N/A";
                            String size = tdNormals.size() > 1 ? tdNormals.get(1).text() : "N/A";

                            String seeds = item.selectFirst("td.tdseed").text();
                            String leeches = item.selectFirst("td.tdleech").text();

                            results.add(new SearchResult(
                                    title,
                                    "Seeds: " + seeds,
                                    "Leeches: " + leeches,
                                    "Size: " + size,
                                    "LimeTorrents",
                                    link,
                                    null
                            ));
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing LimeTorrents item", e);
                        }
                    }
                }
                if (site.equals("LimeTorrentsTrending")) {
                    // Fetch LimeTorrents top 100 torrents
                    Elements items = doc.select("table.table2 tr:not(:has(th))");
                    for (Element item : items) {
                        try {
                            Element titleElement = item.selectFirst("td.tdleft a:not([rel=nofollow])");
                            if (titleElement == null) continue;

                            String title = titleElement.text();
                            String detailPageLink = Url.UrlLimeTorrent + titleElement.attr("href").substring(1); // Remove leading "/"
                            Log.d(TAG, "Detail page link: " + detailPageLink);
                            String seeds = item.selectFirst("td.tdseed").text();
                            String leeches = item.selectFirst("td.tdleech").text();
                            String size = item.select("td.tdnormal").get(1).text();

                            results.add(new SearchResult(
                                    title, "Seeds: " + seeds, "Leeches: " + leeches,
                                    "Size: " + size, detailPageLink,
                                    null, null
                            ));
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing LimeTorrents trending item", e);
                        }
                    }
                }
            }
            catch(Exception e) {
                if (listener != null) {
                    listener.onError(e.getMessage());
                }
            }
            return results;
        }
        @Override
        protected void onPostExecute(ArrayList<SearchResult> results) {
            if (listener != null) {
                if (results.isEmpty()) {
                    listener.onError("No results found");
                } else {
                    listener.onResultsReady(results);
                }
            }
        }
    }
}