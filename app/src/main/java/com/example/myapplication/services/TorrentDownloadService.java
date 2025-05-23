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
import com.example.myapplication.utils.DownloadLocationManager;
import com.github.se_bastiaan.torrentstream.StreamStatus;
import com.github.se_bastiaan.torrentstream.Torrent;
import com.github.se_bastiaan.torrentstream.TorrentOptions;
import com.github.se_bastiaan.torrentstream.TorrentStream;
import com.github.se_bastiaan.torrentstream.listeners.TorrentListener;

import com.google.firebase.auth.FirebaseAuth;

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
    private String torrentUrlMagnet;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private File downloadLocation;
    private FirebaseAuth mAuth;

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
            String torrentUrl = "http://itorrents.org/torrent/" + infohash + ".torrent";
            torrentUrlMagnet = "magnet:?xt=urn:btih:" + infohash;

            Log.d(TAG, "Starting download with torrent URL: " + torrentUrl);
            Log.d(TAG, "Magnet link backup: " + torrentUrlMagnet);

            startTorrentDownload(torrentUrl);
        } catch (Exception e) {
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
                String torrentUrl = "http://itorrents.org/torrent/" + infohash + ".torrent";
                torrentUrlMagnet = "magnet:?xt=urn:btih:" + infohash;

                Log.d(TAG, "Starting download with torrent URL: " + torrentUrl);
                Log.d(TAG, "Magnet link backup: " + torrentUrlMagnet);
                
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
            showErrorToast("Error starting download");
            stopSelf();
        }
    }

    private void initializeTorrentStream() {
        try {
            downloadLocation = DownloadLocationManager.getInstance(this).getCurrentLocation();
            if (!downloadLocation.exists()) {
                Log.d(TAG, "Download location doesn't exist, creating: " + downloadLocation.getAbsolutePath());
                downloadLocation.mkdirs();
            }

            TorrentOptions torrentOptions = new TorrentOptions.Builder()
                    .saveLocation(downloadLocation)
                    .removeFilesAfterStop(false)
                    .build();
            torrentStream = TorrentStream.init(torrentOptions);
            torrentStream.addListener(this);
            Log.d(TAG, "Download location: " + downloadLocation.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "Error initializing TorrentStream: " + e.getMessage());
        }
    }

    private void initializeDownloadLocation() {
        downloadLocation = DownloadLocationManager.getInstance(this).getCurrentLocation();
        Log.d(TAG, "Download location from profile: " + downloadLocation.getAbsolutePath());
        if (!downloadLocation.exists()) {
            downloadLocation.mkdirs();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mAuth = FirebaseAuth.getInstance();
        initializeDownloadLocation();
        // ... rest of existing onCreate code ...
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
        
        // Try magnet link fallback if torrent URL failed
        if (e.getMessage().contains("No torrent info could be found or read") && torrentUrlMagnet != null && !torrentUrlMagnet.isEmpty()) {
            Log.d(TAG, "Torrent URL failed, attempting magnet link fallback: " + torrentUrlMagnet);
            showErrorToast("Torrent URL failed, trying magnet link...");
            
            try {
                // Need to reinitialize torrentStream after error
                torrentStream.startStream(torrentUrlMagnet);
                Log.d(TAG, "Magnet link download started successfully");
                return; // Don't stop the service if magnet link started successfully
            } catch (Exception e2) {
                Log.e(TAG, "Error starting stream with magnet link: " + e2.getMessage());
                showErrorToast("Failed to start download with magnet link");
            }
        }
        
        // If we get here, either it wasn't a torrent info error or magnet link also failed
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