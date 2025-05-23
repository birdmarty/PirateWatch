package com.example.myapplication.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.File;

public class DownloadLocationManager {
    private static final String TAG = "DownloadLocationManager";
    private static final String PREF_NAME = "download_prefs";
    private static final String KEY_DOWNLOAD_PATH = "download_path";
    private static final String KEY_IS_FIRST_LAUNCH = "is_first_launch";
    private static DownloadLocationManager instance;
    private final SharedPreferences preferences;
    private File currentLocation;

    private DownloadLocationManager(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        loadSavedLocation();
    }

    public static synchronized DownloadLocationManager getInstance(Context context) {
        if (instance == null) {
            instance = new DownloadLocationManager(context);
        }
        return instance;
    }

    private void loadSavedLocation() {
        String savedPath = preferences.getString(KEY_DOWNLOAD_PATH, null);
        boolean isFirstLaunch = preferences.getBoolean(KEY_IS_FIRST_LAUNCH, true);

        if (savedPath != null) {
            // If we have a saved path, use it
            currentLocation = new File(savedPath);
        } else if (isFirstLaunch) {
            // First time launch - use default TorrentStream folder
            currentLocation = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS), "TorrentStream");
            // Create the directory
            if (!currentLocation.exists()) {
                currentLocation.mkdirs();
            }
            // Save this as the initial path
            preferences.edit()
                    .putString(KEY_DOWNLOAD_PATH, currentLocation.getAbsolutePath())
                    .putBoolean(KEY_IS_FIRST_LAUNCH, false)
                    .apply();
        } else {
            // Fallback to Downloads folder if something went wrong
            currentLocation = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS);
        }
        Log.d(TAG, "Loaded download location: " + currentLocation.getAbsolutePath());
    }

    public File getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(File newLocation) {
        if (newLocation != null) {
            currentLocation = newLocation;
            preferences.edit()
                    .putString(KEY_DOWNLOAD_PATH, newLocation.getAbsolutePath())
                    .apply();
            Log.d(TAG, "Updated download location to: " + newLocation.getAbsolutePath());
        }
    }

    public void setCurrentLocationFromUri(Uri uri) {
        if (uri != null) {
            // Convert URI to actual file path
            String path = uri.getPath();
            if (path != null && path.startsWith("/tree/")) {
                // Extract the actual path from the URI
                String[] parts = path.split("/");
                if (parts.length > 2) {
                    // If it's the downloads folder, use the proper path
                    if (parts[2].equals("downloads")) {
                        currentLocation = Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_DOWNLOADS);
                    } else {
                        // For other storage locations
                        String storagePath = "/storage/" + parts[2];
                        currentLocation = new File(storagePath);
                    }
                    
                    // Create the directory if it doesn't exist
                    if (!currentLocation.exists()) {
                        currentLocation.mkdirs();
                    }
                    
                    preferences.edit()
                            .putString(KEY_DOWNLOAD_PATH, currentLocation.getAbsolutePath())
                            .apply();
                    Log.d(TAG, "Updated download location from URI to: " + currentLocation.getAbsolutePath());
                }
            }
        }
    }

    public String getCurrentLocationPath() {
        return currentLocation.getAbsolutePath();
    }
} 