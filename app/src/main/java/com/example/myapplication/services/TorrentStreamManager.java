package com.example.myapplication.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.example.myapplication.R;
import com.github.se_bastiaan.torrentstream.StreamStatus;
import com.github.se_bastiaan.torrentstream.Torrent;
import com.github.se_bastiaan.torrentstream.TorrentOptions;
import com.github.se_bastiaan.torrentstreamserver.TorrentServerListener;
import com.github.se_bastiaan.torrentstreamserver.TorrentStreamNotInitializedException;
import com.github.se_bastiaan.torrentstreamserver.TorrentStreamServer;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class TorrentStreamManager implements TorrentServerListener {
    private static final String TAG = "TorrentStreamManager";
    private static final String STREAM_CHANNEL_ID = "torrent_stream_channel";
    private static final int STREAM_NOTIFICATION_ID = 1001;
    private static final int PROGRESS_UPDATE_THRESHOLD = 5; // Only update notification every 5% change

    private final Context context;
    private TorrentStreamServer torrentStreamServer;
    private TorrentStreamListener listener;
    private NotificationCompat.Builder notificationBuilder;
    private NotificationManager notificationManager;
    private int lastProgressUpdate = 0;

    public interface TorrentStreamListener {
        void onProgressUpdate(int progress);
        void onStreamReady();
        void onStreamError(String error);
    }

    public TorrentStreamManager(Context context, TorrentStreamListener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
        createNotificationChannel();
        initializeServer();
        notificationManager = context.getSystemService(NotificationManager.class);
        notificationBuilder = new NotificationCompat.Builder(context, STREAM_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stream)
                .setContentTitle("Preparing Stream")
                .setProgress(100, 0, false)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    STREAM_CHANNEL_ID,
                    "Stream Status",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Shows streaming preparation progress");
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private void initializeServer() {
        System.setProperty("nanohttpd.mimetypes", "mimetypes.properties");

        TorrentOptions torrentOptions = new TorrentOptions.Builder()
                .saveLocation(context.getExternalCacheDir())
                .prepareSize(100L * 1024 * 1024)
                .removeFilesAfterStop(true)
                .build();

        torrentStreamServer = TorrentStreamServer.getInstance();
        torrentStreamServer.setTorrentOptions(torrentOptions);
        torrentStreamServer.setServerHost("0.0.0.0");
        torrentStreamServer.setServerPort(8080);
        torrentStreamServer.startTorrentStream();
        torrentStreamServer.addListener(this);
    }

    public void startStream(String magnetUrl) {
        try {
            torrentStreamServer.startStream(magnetUrl);
        } catch (IOException | TorrentStreamNotInitializedException e) {
            listener.onStreamError(e.getMessage());
            Log.e(TAG, "Stream start error", e);
        }
    }

    public void stopStream() {
        if (torrentStreamServer != null) {
            torrentStreamServer.stopStream();
            deleteFiles();
            cancelNotification();
        }
    }

    private void deleteFiles() {
        try {
            Torrent currentTorrent = torrentStreamServer.getCurrentTorrent();
            if (currentTorrent != null) {
                File videoFile = currentTorrent.getVideoFile();
                File torrentDir = videoFile.getParentFile();

                if (videoFile.exists() && !videoFile.delete()) {
                    Log.e(TAG, "Failed to delete video file");
                }

                if (torrentDir != null && torrentDir.exists() && !deleteRecursive(torrentDir)) {
                    Log.e(TAG, "Failed to delete torrent directory");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Cleanup error", e);
        }
    }

    private boolean deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            File[] files = fileOrDirectory.listFiles();
            if (files != null) {
                for (File child : files) {
                    deleteRecursive(child);
                }
            }
        }
        return fileOrDirectory.delete();
    }

    @Override
    public void onStreamPrepared(Torrent torrent) {
        Log.d(TAG, "Stream prepared");
    }

    @Override
    public void onStreamStarted(Torrent torrent) {
        Log.d(TAG, "Stream started");
        Toast.makeText(context, "Stream started, buffering may take a while... (2-3 minutes)", Toast.LENGTH_LONG).show();
        notificationBuilder.setContentText("Buffering... 0%")
                .setProgress(100, 0, false);
        notificationManager.notify(STREAM_NOTIFICATION_ID, notificationBuilder.build());
    }

    @Override
    public void onStreamError(Torrent torrent, Exception e) {
        listener.onStreamError(e.getMessage());
        deleteFiles();
        showErrorNotification(e.getMessage());
    }

    private void showErrorNotification(String message) {
        notificationBuilder.setSmallIcon(R.drawable.ic_error)
                .setContentTitle("Stream Error")
                .setContentText(message)
                .setProgress(0, 0, false)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);
        notificationManager.notify(STREAM_NOTIFICATION_ID, notificationBuilder.build());
    }

    @Override
    public void onStreamReady(Torrent torrent) {
        listener.onStreamReady();
        showStreamReadyNotification();
    }

    @Override
    public void onStreamProgress(Torrent torrent, StreamStatus status) {
        if (status.bufferProgress < 100) {
            int progress = (int) status.bufferProgress;
            Log.d(TAG, "Progress: " + progress + "%");
            listener.onProgressUpdate(progress);
            
            // Only update notification if progress has changed significantly
            if (Math.abs(progress - lastProgressUpdate) >= PROGRESS_UPDATE_THRESHOLD) {
                notificationBuilder.setContentText("Buffering... " + progress + "%")
                        .setProgress(100, progress, false);
                notificationManager.notify(STREAM_NOTIFICATION_ID, notificationBuilder.build());
                lastProgressUpdate = progress;
            }
        }
    }

    private void showStreamReadyNotification() {
        notificationBuilder.setSmallIcon(R.drawable.ic_stream_ready)
                .setContentTitle("Stream Ready")
                .setContentText("Tap to open player")
                .setProgress(0, 0, false)
                .setAutoCancel(true);
        notificationManager.notify(STREAM_NOTIFICATION_ID, notificationBuilder.build());
    }

    private void cancelNotification() {
        notificationManager.cancel(STREAM_NOTIFICATION_ID);
    }

    @Override
    public void onStreamStopped() {
        deleteFiles();
        cancelNotification();
    }

    @Override
    public void onServerReady(String url) {
        Log.d(TAG, "onServerReady: " + url);
        Uri streamUri = Uri.parse(url);
        // Launch VLC directly without verification since we know it's our local server
        launchVlcPlayer(streamUri);
    }

    private void verifyServerConnection(Uri streamUri) {
        // Removed verification since it's our local server and we know it's streaming video data
        launchVlcPlayer(streamUri);
    }

    private void launchVlcPlayer(Uri streamUri) {
        try {
            // First try generic video player intent
            Intent videoIntent = new Intent(Intent.ACTION_VIEW);
            videoIntent.setDataAndType(streamUri, "video/*");
            videoIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            // Check if there are any apps that can handle video playback
            if (videoIntent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(videoIntent);
                return;
            }

            // If no generic video player found, try VLC specifically
            Intent vlcIntent = new Intent(Intent.ACTION_VIEW);
            vlcIntent.setDataAndType(streamUri, "video/*");
            vlcIntent.setPackage("org.videolan.vlc");
            vlcIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            if (vlcIntent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(vlcIntent);
            } else {
                // If VLC is not installed, prompt to install it
                Intent marketIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=org.videolan.vlc"));
                marketIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(marketIntent);
                listener.onStreamError("Please install VLC Player to watch videos");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error launching video player: " + e.getMessage(), e);
            listener.onStreamError("Error launching video player: " + e.getMessage());
        }
    }

    public void cleanup() {
        try {
            if (torrentStreamServer != null) {
                stopStream();
                torrentStreamServer.stopTorrentStream();
                torrentStreamServer.removeListener(this);
                torrentStreamServer = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Cleanup error", e);
        }
    }

    // Unused TorrentServerListener methods
    public void onServerReady(Torrent torrent) {}
}