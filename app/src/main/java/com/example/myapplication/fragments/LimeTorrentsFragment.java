package com.example.myapplication.fragments;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.myapplication.R;
import com.example.myapplication.adapters.TorrentAdapter;
import com.github.se_bastiaan.torrentstream.Torrent;

public class LimeTorrentsFragment extends Fragment implements TorrentAdapter.RecyclerviewListener {

    public LimeTorrentsFragment() {
        // Required empty public constructor
    }

    // TODO: Rename and change types and number of parameters
    public static LimeTorrentsFragment newInstance(String param1, String param2) {
        LimeTorrentsFragment fragment = new LimeTorrentsFragment();

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_lime_torrents, container, false);
    }

    @Override
    public void onItemClick(int position) {

    }
}