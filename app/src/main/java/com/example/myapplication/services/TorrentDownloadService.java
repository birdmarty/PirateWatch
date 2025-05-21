package com.example.myapplication.services;

import static com.github.se_bastiaan.torrentstream.utils.ThreadUtils.runOnUiThread;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.example.myapplication.R;
import com.github.se_bastiaan.torrentstream.StreamStatus;
import com.github.se_bastiaan.torrentstream.Torrent;
import com.github.se_bastiaan.torrentstream.TorrentOptions;
import com.github.se_bastiaan.torrentstream.TorrentStream;
import com.github.se_bastiaan.torrentstream.listeners.TorrentListener;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TorrentDownloadService extends Service implements TorrentListener {
    private static final String TAG = "TorrentDownloadService";
    private static final String CHANNEL_ID = "torrent_download_channel";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36";
    private static final int TIMEOUT = 15000;
    private TorrentStream torrentStream;
    private String torrentLink;
    private String detailPageLink;
    private String action;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        torrentLink = intent.getStringExtra("torrentLink");
        action = intent.getStringExtra("action");
        detailPageLink = intent.getStringExtra("detailPage");
        Log.d(TAG,"Action selected: " + action);
        Log.d(TAG, "Torrent link: " + torrentLink);
        Log.d(TAG, "detail page: " + detailPageLink);

        if (action != null && action.equals("Download"))
        {
            if (torrentLink != null) {
                executor.execute(this::fetchAndStartTorrent);
            }
            else if (detailPageLink != null) {
                executor.execute(this::fetchAndStartLime);
            } else {
                Log.e(TAG, "Torrent link is null");
                stopSelf();
            }
            if (action.equals("Watch")){
                // add watch implementation here
            }
        }


        return START_STICKY;
    }

    private void fetchAndStartLime() {
        try {
            // Fetch infohash from detail page
            Document detailDoc = Jsoup.connect(detailPageLink)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT)
                    .get();

            Element infohashElement = detailDoc.selectFirst("td:contains(Torrent Hash) + td");
            if (infohashElement == null) {
                Log.e(TAG, "Infohash element not found");
                return;
                }
            String infohash = infohashElement.text();
            torrentLink = "https://itorrents.org/torrent/" + infohash + ".torrent";
            Log.d(TAG, "Torrent link: " + torrentLink);
            startTorrentDownload(torrentLink);
        }
         catch (Exception e) {
            Log.e(TAG, "Error parsing LimeTorrents item", e);
        }
    }


    private void fetchAndStartTorrent() {
        try {
            // Fetch infohash from the torrent detail page
            Document doc = Jsoup.connect(torrentLink)
                    .userAgent("Mozilla/5.0")
                    .timeout(15000)
                    .get();

            Element infohashElement = doc.selectFirst("div.infohash-box span");
            if (infohashElement != null) {
                String infohash = infohashElement.text();
                String torrentUrl = "https://itorrents.org/torrent/" + infohash + ".torrent";
//                String torrentUrl = "magnet:?xt=urn:btih:" + infohash;

                startTorrentDownload(torrentUrl);

            } else {
                showErrorToast("Could not find torrent info");
                stopSelf();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching infohash: " + e.getMessage());
            showErrorToast("Error fetching torrent info");
            stopSelf();
        }
    }

    private void startTorrentDownload(String torrentUrl) {
        try {
            if (torrentStream == null) {
                initializeTorrentStream();
            }

            runOnUiThread(() -> {
                Toast.makeText(this, "Starting download", Toast.LENGTH_SHORT).show();
                createNotificationChannel();
            });

            torrentStream.startStream(torrentUrl);
            Log.d(TAG, "Torrent download started: " + torrentUrl);
        } catch (Exception e) {
            Log.e(TAG, "Error starting stream: " + e.getMessage());
            showErrorToast("Failed to start download");
            stopSelf();
        }
    }

    private void initializeTorrentStream() {
        try {
            File saveLocation = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS), "TorrentStream");
            if (!saveLocation.exists()) {
                saveLocation.mkdirs();
            }

            TorrentOptions torrentOptions = new TorrentOptions.Builder()
                    .saveLocation(saveLocation)
                    .removeFilesAfterStop(false)
                    .build();
            torrentStream = TorrentStream.init(torrentOptions);
            torrentStream.addListener(this);


            // Configure torrent options for maximum performance
//            TorrentOptions torrentOptions = new TorrentOptions.Builder()
//                    .saveLocation(saveLocation)
//                    .removeFilesAfterStop(false)
//                    .maxConnections(500) // Maximum allowed connections
//                    .maxDownloadSpeed(0)  // 0 = unlimited download
//                    .prepareSize(30 * 1024L * 1024L) // 30MB initial buffer
//                    .listeningPort(6891)  // Standard BitTorrent port
//                    .build();
//
//            torrentStream = TorrentStream.init(torrentOptions);
//            torrentStream.addListener(this);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing TorrentStream: " + e.getMessage());
        }
    }



    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Torrent Download";
            String description = "Notifications for torrent download progress";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void updateNotification(int progress, int downloadSpeed, int seedCount) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_download)
                .setContentText("Progress: " + progress + "% - Speed: " + downloadSpeed + " KB/s - Seeds: " + seedCount)
                .setProgress(100, progress, false)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW);

        notificationManager.notify(1, builder.build());
    }

    private void showErrorToast(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }



    // TorrentListener callbacks
    @Override
    public void onStreamPrepared(Torrent torrent) {
        Log.d(TAG, "Stream prepared");
    }

    @Override
    public void onStreamStarted(Torrent torrent) {
        Log.d(TAG, "Stream started");
    }

    @Override
    public void onStreamError(Torrent torrent, Exception e) {
        Log.e(TAG, "Stream error: " + e.getMessage());
        showErrorToast("Download error: " + e.getMessage());
        stopSelf();
    }

    @Override
    public void onStreamReady(Torrent torrent) {
        Log.d(TAG, "Stream ready");

        // Show a toast message indicating download is complete
        runOnUiThread(() -> Toast.makeText(this, "Download finished", Toast.LENGTH_LONG).show());

        // Remove the ongoing notification
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(1);
    }

    @Override
    public void onStreamProgress(Torrent torrent, StreamStatus status) {
        if (status.bufferProgress < 100) {

            Log.d(TAG, "progress: " + status.bufferProgress + " speed: " + (status.downloadSpeed / 1024) + " seeds: " + status.seeds);

            updateNotification((int) status.bufferProgress, status.downloadSpeed / 1024, status.seeds);
        }
    }

    @Override
    public void onStreamStopped() {
        Log.d(TAG, "Stream stopped");
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(1);
        stopSelf();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (torrentStream != null) {
            torrentStream.removeListener(this);
            torrentStream.stopStream();
        }
        executor.shutdown();
    }
}