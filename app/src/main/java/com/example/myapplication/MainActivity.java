package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.view.View;

import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.myapplication.fragments.DiscoverFragment;
import com.example.myapplication.fragments.FullScreenLoginFragment;
import com.example.myapplication.fragments.FullScreenSignUpFragment;
import com.example.myapplication.fragments.ProfileFragment;
import com.example.myapplication.fragments.SignIn;
import com.example.myapplication.fragments.SignUp;
import com.example.myapplication.services.TorrentStreamManager;
import com.github.se_bastiaan.torrentstream.TorrentOptions;
import com.github.se_bastiaan.torrentstream.TorrentStream;

import com.example.myapplication.fragments.HomeFragment;


import com.example.myapplication.fragments.SearchResultsFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;

public class MainActivity extends AppCompatActivity implements TorrentStreamManager.TorrentStreamListener, SignIn.AuthListener, SignUp.AuthListener, FullScreenLoginFragment.AuthListener, FullScreenSignUpFragment.AuthListener {

    private static final String PREF_NAME = "AppPrefs";
    private static final String KEY_THEME = "theme_mode";

    private TorrentStreamManager torrentStreamManager;
    public TorrentStream torrentStream;
    private static final String TAG = "MainActivity";
    private static final int STORAGE_PERMISSION_CODE = 101;
    private static final int NOTIFICATION_PERMISSION_CODE = 102;
    private static String keyword;
    private static String sortItem = "Seeds DESC";
    public static int current;
    private EditText inputSearch;
    private boolean isStreamBuffering = false; // Track if a stream is currently buffering
    private boolean isInitialized = false; // Track initialization state

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme before setting content view
        applyTheme();
        
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        requestPermissions();

        Button btnHome = findViewById(R.id.btn_home);
        Button btnProfile = findViewById(R.id.btn_profile);
        Button btnDiscover = findViewById(R.id.btn_discover);

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            // User is either logged in or in guest mode
            showNavigationButtons(true);
            loadFragment(new HomeFragment());
        } else {
            // Show login first
            showNavigationButtons(false);
            loadFragment(new SignIn());
        }

        btnHome.setOnClickListener(v -> {
            if (isUserAuthenticated()) {
                loadFragment(new HomeFragment());
            } else {
                showNavigationButtons(false);
                loadFragment(new SignIn());
            }
        });

        btnProfile.setOnClickListener(v -> {
            if (isUserAuthenticated()) {
                loadFragment(new ProfileFragment());
            } else {
                showNavigationButtons(false);
                loadFragment(new SignIn());
            }
        });

        btnDiscover.setOnClickListener(v -> {
            if (isUserAuthenticated()) {
                loadFragment(new DiscoverFragment());
            } else {
                showNavigationButtons(false);
                loadFragment(new SignIn());
            }
        });
    }

    private void applyTheme() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean(KEY_THEME, false);
        AppCompatDelegate.setDefaultNightMode(isDarkMode ? 
            AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }

    private void showNavigationButtons(boolean show) {
        View bottomButtons = findViewById(R.id.bottom_buttons);
        if (bottomButtons != null) {
            bottomButtons.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private boolean isUserAuthenticated() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null; // Allow both regular and anonymous users
    }

    @Override
    public void onAuthSuccess() {
        runOnUiThread(() -> {
            showNavigationButtons(true);
            loadFragment(new HomeFragment());
            // Clear back stack
            getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        });
    }

    @Override
    public void onAuthCancel() {
        runOnUiThread(() -> {
            showNavigationButtons(true);
            // Go back to previous fragment
            getSupportFragmentManager().popBackStack();
        });
    }

    public void startTorrentStream(String magnetUrl) {
        if (torrentStreamManager != null) {
            isStreamBuffering = true;
            Log.d(TAG, "Starting torrent stream: " + magnetUrl);
            torrentStreamManager.startStream(magnetUrl);
        } else {
            Toast.makeText(this, "Torrent system not initialized", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public void onProgressUpdate(int progress) {
        isStreamBuffering = (progress < 100);
    }

    @Override
    public void onStreamReady() {
        isStreamBuffering = false;
        Toast.makeText(this, "Stream ready", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStreamError(String error) {
        isStreamBuffering = false;
        isInitialized = false; // Reset initialization state on error
        runOnUiThread(() -> {
            Toast.makeText(this, error, Toast.LENGTH_LONG).show();
        });
    }

    private boolean loadFragment(Fragment fragment) {
        if (fragment != null) {
            // Hide navigation buttons if showing login/signup
            if (fragment instanceof SignIn || fragment instanceof SignUp || 
                fragment instanceof FullScreenLoginFragment || fragment instanceof FullScreenSignUpFragment) {
                showNavigationButtons(false);
            } else {
                showNavigationButtons(true);
            }

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
            return true;
        }
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Only cleanup and reinitialize if we're not currently buffering a stream
        if (!isStreamBuffering) {
            Log.d(TAG, "onResume: No active stream, cleaning up");
            cleanupTorrentSystem();
            if (!isInitialized) {
                Log.d(TAG, "onResume: System not initialized, initializing");
                initializeTorrentSystem();
            }
        } else {
            Log.d(TAG, "onResume: Stream is buffering, skipping cleanup");
        }
    }

    private void cleanupTorrentSystem() {
        Log.d(TAG, "Cleaning up torrent system");
        if (torrentStreamManager != null) {
            torrentStreamManager.cleanup();
            torrentStreamManager = null;
        }
        if (torrentStream != null) {
            torrentStream.stopStream();
            torrentStream = null;
        }
        isStreamBuffering = false;
        isInitialized = false;
    }

    private void initializeTorrentSystem() {
        if (isInitialized) {
            Log.d(TAG, "Torrent system already initialized, skipping");
            return;
        }

        try {
            Log.d(TAG, "Initializing torrent system");
            // Initialize download system
            File saveLocation = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS), "TorrentStream");
            if (!saveLocation.exists()) saveLocation.mkdirs();

            TorrentOptions torrentOptions = new TorrentOptions.Builder()
                    .saveLocation(saveLocation)
                    .removeFilesAfterStop(true) // Changed to true to cleanup files
                    .build();
            torrentStream = TorrentStream.init(torrentOptions);

            // Initialize streaming system
            torrentStreamManager = new TorrentStreamManager(this, this);
            isInitialized = true;
            Log.d(TAG, "Torrent system initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Initialization error: " + e.getMessage(), e);
            Toast.makeText(this, "Initialization error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            isInitialized = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (torrentStreamManager != null) {
            torrentStreamManager.cleanup();
        }
        clearAppCache();
    }

    private void clearAppCache() {
        try {
            // Clear internal cache
            File cacheDir = getCacheDir();
            File externalCacheDir = getExternalCacheDir();
            
            // Clear internal cache
            if (cacheDir != null && cacheDir.exists()) {
                deleteRecursive(cacheDir);
            }
            
            // Clear external cache
            if (externalCacheDir != null && externalCacheDir.exists()) {
                deleteRecursive(externalCacheDir);
            }
            
            // Clear torrent stream cache
            File torrentCacheDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS), "TorrentStream");
            if (torrentCacheDir.exists()) {
                deleteRecursive(torrentCacheDir);
            }
            
            Log.d(TAG, "Cache cleared successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error clearing cache: " + e.getMessage());
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
                // Do not add listener here; we will handle it in the service
                Toast.makeText(this, "TorrentStream initialized successfully", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "TorrentStream initialized successfully");
            } else {
                Toast.makeText(this, "Failed to initialize TorrentStream", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "TorrentStream initialization returned null");
            }
        } catch (Exception e) {
            String errorMsg = "Error initializing TorrentStream: " + e.getMessage();
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
            Log.e(TAG, errorMsg, e);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    initTorrentStream();
                    initializeTorrentSystem();
                    Toast.makeText(this, "Permissions granted, initializing TorrentStream", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Manage External Storage permission required", Toast.LENGTH_LONG).show();
                }
            }
        }
    }


    private void requestPermissions() {
        // Request notification permission for Android 13 and above
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, 
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, 
                    NOTIFICATION_PERMISSION_CODE);
            }
        }

        // Request storage permissions
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, STORAGE_PERMISSION_CODE);
            } else {
                if (!isInitialized) {
                    initializeTorrentSystem();
                }
            }
        } else {
            String[] permissions = {
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.INTERNET
            };

            boolean allPermissionsGranted = true;
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(this, permission) 
                        != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (!allPermissionsGranted) {
                ActivityCompat.requestPermissions(this, permissions, STORAGE_PERMISSION_CODE);
            } else {
                if (!isInitialized) {
                    initializeTorrentSystem();
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == STORAGE_PERMISSION_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                if (!isInitialized) {
                    initializeTorrentSystem();
                }
            } else {
                Toast.makeText(this, "Storage permissions are required for the app to function properly", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Notification permission granted
                Log.d(TAG, "Notification permission granted");
            } else {
                Toast.makeText(this, "Notification permission is recommended for better experience", Toast.LENGTH_LONG).show();
            }
        }
    }

    private final TextView.OnEditorActionListener editorActionListener = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                keyword = inputSearch.getText().toString();
                if (!keyword.isEmpty()){
                    loadSearchResultsFragment();
                    return true;
                }
                current = 0;
            }

            return false;
        }
    };

    public static String getKeyword() {
        return keyword;
    }

    public static String getSortItem() {
        return sortItem;
    }

    private void loadSearchResultsFragment() {
        // Create an instance of SearchResultsFragment
        SearchResultsFragment fragment = new SearchResultsFragment();

        // Begin a fragment transaction
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // Replace the fragmentContainer with the SearchResultsFragment
        transaction.replace(R.id.fragment_container, fragment);

        // Optionally add the transaction to the back stack
        transaction.addToBackStack(null);

        // Commit the transaction
        transaction.commit();
    }
}