package com.example.myapplication.models;

import com.google.firebase.Timestamp;

public class WatchLaterTorrent {
    private String title;
    private String infoHash;
    private String magnetLink;
    private String torrentLink;
    private String website;
    private Timestamp timestamp;
    private String userId;
    private String size;

    public WatchLaterTorrent() {
        // Required empty constructor for Firestore
    }

    public WatchLaterTorrent(String title, String infoHash, String magnetLink, String torrentLink,
                            String website, Timestamp timestamp, String userId, String size) {
        this.title = title;
        this.infoHash = infoHash;
        this.magnetLink = magnetLink;
        this.torrentLink = torrentLink;
        this.website = website;
        this.timestamp = timestamp;
        this.userId = userId;
        this.size = size;
    }

    // Getters
    public String getTitle() { return title; }
    public String getInfoHash() { return infoHash; }
    public String getMagnetLink() { return magnetLink; }
    public String getTorrentLink() { return torrentLink; }
    public String getWebsite() { return website; }
    public Timestamp getTimestamp() { return timestamp; }
    public String getUserId() { return userId; }
    public String getSize() { return size; }

    // Setters
    public void setTitle(String title) { this.title = title; }
    public void setInfoHash(String infoHash) { this.infoHash = infoHash; }
    public void setMagnetLink(String magnetLink) { this.magnetLink = magnetLink; }
    public void setTorrentLink(String torrentLink) { this.torrentLink = torrentLink; }
    public void setWebsite(String website) { this.website = website; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setSize(String size) { this.size = size; }
} 