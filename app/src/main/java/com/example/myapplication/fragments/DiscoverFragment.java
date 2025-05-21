package com.example.myapplication.fragments;

import android.util.Log;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import com.bumptech.glide.Glide;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myapplication.R;
import com.example.myapplication.adapters.TorrentAdapter;
import com.example.myapplication.cache.TorrentCacheManager;
import com.example.myapplication.parsing.CategoryList;
import com.example.myapplication.parsing.SearchResult;
import com.example.myapplication.parsing.SortList;
import com.example.myapplication.parsing.Url;
import com.example.myapplication.repository.TorrentRepository;

import java.util.ArrayList;

public class DiscoverFragment extends Fragment implements TorrentAdapter.RecyclerviewListener {

    private static final String SITE_1337X = "1337x";
    private static final String SITE_LIME = "LimeTorrents";
    private static final String TAG = "DiscoverFragment";
    private ImageView loadingIcon;
    private EditText inputSearch;
    private Spinner categorySpinner, sortSpinner;
    private String searchUrl;
    private String keyword = "";
    private String sortItem = "Seeds DESC";
    private String category = "All";
    private String currentSite = SITE_1337X;       // Default site
    private TorrentCacheManager cacheManager;
    private TorrentAdapter torrentAdapter;
    private ArrayList<SearchResult> torrentsList = new ArrayList<>();
    private TorrentRepository torrentRepository;
    private Handler uiHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        torrentRepository = new TorrentRepository();
        uiHandler = new Handler(Looper.getMainLooper());
        currentSite = SITE_1337X;
        updateSiteSelectionUI();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_discover, container, false);

        TextView tv1337x = rootView.findViewById(R.id.tv1337x);
        TextView tvLime = rootView.findViewById(R.id.tvLimeTorrents);

        tv1337x.setOnClickListener(v -> on1337xClicked());
        tvLime.setOnClickListener(v -> onLimeTorrentsClicked());

        RecyclerView recyclerView = rootView.findViewById(R.id.recyclerview);
        loadingIcon = rootView.findViewById(R.id.loadingIcon);
        categorySpinner = rootView.findViewById(R.id.categorySpinner);
        sortSpinner = rootView.findViewById(R.id.sortSpinner);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        torrentAdapter = new TorrentAdapter(getContext(), this, torrentsList);
        recyclerView.setAdapter(torrentAdapter);

        inputSearch = rootView.findViewById(R.id.inputSearch);
        inputSearch.setOnEditorActionListener(editorActionListener);

        setupSpinners();
        initializeCache();
        currentSite = SITE_1337X;
        updateSiteSelectionUI();
        updateSpinners();
        return rootView;
    }

    private void initializeCache() {
        cacheManager = new TorrentCacheManager(requireContext());
        ArrayList<SearchResult> cachedResults = cacheManager.getCachedDiscoverResults();
        if (cachedResults != null && !cachedResults.isEmpty()) { //check if we have cached trending
            torrentsList.clear();
            torrentsList.addAll(cachedResults);
            torrentAdapter.notifyDataSetChanged();
        } else { //if not, load trending from 1337x
            loadTrendingTorrents();
        }
    }

    private void setupSpinners() {
        updateSpinners(); // Use the new unified method

        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                category = parent.getItemAtPosition(pos).toString();
                if (!keyword.isEmpty()) searchTorrents(keyword);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                category = "All";
            }
        });

        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                sortItem = parent.getItemAtPosition(pos).toString();
                if (!keyword.isEmpty()) searchTorrents(keyword);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                sortItem = "Sort By...";
            }
        });
    }

    private void loadTrendingTorrents() {
        showLoading();
        if (currentSite.equals("1337x")) {      // Load 1337x results
            torrentRepository.getTrendingTorrents1337x(new TorrentRepository.TorrentListener() {
                @Override
                public void onResultsReady(ArrayList<SearchResult> results) {
                    uiHandler.post(() -> {
                        cacheManager.cacheDiscoverResults(results);
                        torrentsList.clear();
                        torrentsList.addAll(results);
                        torrentAdapter.notifyDataSetChanged();
                        hideLoading();
                    });
                }

                @Override
                public void onError(String message) {
                    uiHandler.post(() -> {
                        hideLoading();
                        showToast("Error: " + message);
                    });
                }
            });
        }
        else if (currentSite.equals("LimeTorrents")) {      // Load Lime Torrents results
            torrentRepository.getTrendingTorrentsLimeTorrents(new TorrentRepository.TorrentListener() {
                @Override
                public void onResultsReady(ArrayList<SearchResult> results) {
                    uiHandler.post(() -> {
                        cacheManager.cacheDiscoverResults(results);
                        torrentsList.clear();
                        torrentsList.addAll(results);
                        torrentAdapter.notifyDataSetChanged();
                        hideLoading();
                    });
                }

                @Override
                public void onError(String message) {
                    uiHandler.post(() -> {
                        hideLoading();
                        showToast("Error: " + message);
                    });
                }
            });
        }
    }

    private void searchTorrents(String keyword) {
        showLoading();
        torrentsList.clear();
        torrentAdapter.notifyDataSetChanged();

        buildSearchUrl();
        Log.d(TAG, "Search URL: " + searchUrl);

        torrentRepository.searchTorrents(searchUrl, currentSite, new TorrentRepository.TorrentListener() {
            @Override
            public void onResultsReady(ArrayList<SearchResult> results) {
                uiHandler.post(() -> {
                    torrentsList.clear();
                    torrentsList.addAll(results);
                    torrentAdapter.notifyDataSetChanged();
                    hideLoading();
                });
            }

            @Override
            public void onError(String message) {
                uiHandler.post(() -> {
                    hideLoading();
                    showToast("Error: " + message);
                });
            }
        });
    }

    private void buildSearchUrl() {
        SortList sort = new SortList(sortItem);
        CategoryList categoryList = new CategoryList(category);

        if (currentSite.equals(("1337x"))) {
            build1337xUrl(sort, categoryList);
        }
        else if (currentSite.equals(("LimeTorrents"))) {
            buildLimeTorrentsUrl(sort, categoryList);
        }
        Log.d(TAG, "Final Search URL: " + searchUrl);
    }

    private void build1337xUrl(SortList sort, CategoryList categoryList) {
        String baseUrl = Url.UrlOneThree;

        if (!category.equals("All") && !sortItem.equals("Sort By...")) {
            searchUrl = baseUrl + "/sort-category-search/" + keyword
                    + categoryList.getOneThreeCategory() + sort.getOneThreeSort() + "1/";
            Log.d(TAG, "Sort and Category URL: " + searchUrl);
        } else if (!category.equals("All") && sortItem.equals("Sort By...")) {
            searchUrl = categoryList.OneThreeCategorySearch(keyword);
            Log.d(TAG, "Category URL: " + searchUrl);
        } else if (category.equals("All") && !sortItem.equals("Sort By...")) {
            searchUrl = new SortList(sortItem).urlSortSearch(keyword);
            Log.d(TAG, "Sort URL: " + searchUrl);
        } else {
            searchUrl = baseUrl + "/search/" + keyword + "/1/";
            Log.d(TAG, "Default URL: " + searchUrl);
        }
    }

    private void buildLimeTorrentsUrl(SortList sort, CategoryList categoryList) {
        String baseUrl = Url.UrlLimeTorrent;

        if (!category.equals("All") && !sortItem.equals("Sort By...")) {
            searchUrl = baseUrl + "search/"  + categoryList.getLimeCategory() + keyword
                    + sort.getLimeSort() + "1/";
            Log.d(TAG, "Sort and Category URL: " + searchUrl);
        } else if (!category.equals("All") && sortItem.equals("Sort By...")) {
            searchUrl = categoryList.LimeCategorySearch(keyword);
            Log.d(TAG, "Category URL: " + searchUrl);
        } else if (category.equals("All") && !sortItem.equals("Sort By...")) {
            searchUrl = new SortList(sortItem).urlLimeTorrents(keyword);
            Log.d(TAG, "Sort URL: " + searchUrl);
        } else {
            searchUrl = baseUrl + "search/all/" + keyword + "/1/";
            Log.d(TAG, "Default URL: " + searchUrl);
        }

    }

    private void showLoading() {
        uiHandler.post(() -> {
            loadingIcon.setVisibility(View.VISIBLE);
            Glide.with(getContext())
                    .load(R.drawable.loading)
                    .into(loadingIcon);
            torrentsList.clear();
            torrentAdapter.notifyDataSetChanged();
        });
    }

    private void hideLoading() {
        uiHandler.post(() -> loadingIcon.setVisibility(View.GONE));
    }

    private void showToast(String message) {
        uiHandler.post(() -> Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show());
    }

    private final TextView.OnEditorActionListener editorActionListener = (v, actionId, event) -> {
        if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
            keyword = inputSearch.getText().toString().trim();
            if (!keyword.isEmpty()) {
                searchTorrents(keyword);
                return true;
            }
        }
        return false;
    };

    private void updateSpinners() {
        // Update category spinner
        ArrayAdapter<CharSequence> categoryAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                currentSite.equals(SITE_1337X) ? R.array.OneThree_category_options : R.array.Lime_category_options,
                android.R.layout.simple_spinner_item
        );
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);

        // Update sort spinner
        ArrayAdapter<CharSequence> sortAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                currentSite.equals(SITE_1337X) ? R.array.OneThree_sort_options : R.array.Lime_sort_options,
                android.R.layout.simple_spinner_item
        );
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.setAdapter(sortAdapter);

        // Reset selections
        categorySpinner.setSelection(0);
        sortSpinner.setSelection(0);
    }

    public void on1337xClicked() {
        currentSite = SITE_1337X;
        updateSiteSelectionUI();
        updateSpinners();
        loadTorrents();
    }

    public void onLimeTorrentsClicked() {
        currentSite = SITE_LIME;
        updateSiteSelectionUI();
        updateSpinners();
        loadTorrents();
    }

    private void loadTorrents() {
        keyword = inputSearch.getText().toString().trim();
        if (keyword.isEmpty()) {
            loadTrendingTorrents();
        } else {
            searchTorrents(keyword);
        }
    }



    private void updateSiteSelectionUI() {
        if (getView() == null) return;

        TextView tv1337x = getView().findViewById(R.id.tv1337x);
        TextView tvLime = getView().findViewById(R.id.tvLimeTorrents);

        int activeColor = ContextCompat.getColor(requireContext(), R.color.your_active_color);
        int inactiveColor = ContextCompat.getColor(requireContext(), R.color.design_default_color_primary);

        // Compare using constants
        tv1337x.setBackgroundColor(currentSite.equals(SITE_1337X) ? activeColor : inactiveColor);
        tvLime.setBackgroundColor(currentSite.equals(SITE_LIME) ? activeColor : inactiveColor);
    }

    @Override
    public void onItemClick(int position) {

    }
}