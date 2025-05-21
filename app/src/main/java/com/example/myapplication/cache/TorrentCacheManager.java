package com.example.myapplication.cache;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.myapplication.parsing.SearchResult;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;

public class TorrentCacheManager {
    private static final String PREF_NAME = "TorrentCache";
    private static final String KEY_DISCOVER_CACHE = "discover_cache";
    private static final String KEY_CACHE_TIME = "cache_time";
    private static final long CACHE_VALIDITY_DURATION = 30 * 60 * 1000; // 30 minutes

    private final SharedPreferences preferences;
    private final Gson gson;

    public TorrentCacheManager(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public void cacheDiscoverResults(ArrayList<SearchResult> results) {
        SharedPreferences.Editor editor = preferences.edit();
        String json = gson.toJson(results);
        editor.putString(KEY_DISCOVER_CACHE, json);
        editor.putLong(KEY_CACHE_TIME, new Date().getTime());
        editor.apply();
    }

    public ArrayList<SearchResult> getCachedDiscoverResults() {
        long cacheTime = preferences.getLong(KEY_CACHE_TIME, 0);
        if (System.currentTimeMillis() - cacheTime > CACHE_VALIDITY_DURATION) {
            return null; // Cache expired
        }

        String json = preferences.getString(KEY_DISCOVER_CACHE, null);
        if (json == null) {
            return null;
        }

        Type type = new TypeToken<ArrayList<SearchResult>>(){}.getType();
        return gson.fromJson(json, type);
    }

    public void clearCache() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.apply();
    }
}