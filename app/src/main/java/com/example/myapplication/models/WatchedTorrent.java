package com.example.myapplication.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;

public class WatchedTorrent {
    private String title;
    private String infoHash;
    private String magnetLink;
    private String torrentLink;
    private String website;
    private com.google.firebase.Timestamp timestamp;
    private String userId;
    private String size;

    // Empty constructor required for Firestore
    public WatchedTorrent() {}

    public WatchedTorrent(String title, String infoHash, String magnetLink,
                          String torrentLink, String website,
                          com.google.firebase.Timestamp timestamp, String userId, String size) {
        this.title = title;
        this.infoHash = infoHash;
        this.magnetLink = magnetLink;
        this.torrentLink = torrentLink;
        this.website = website;
        this.timestamp = timestamp;
        this.userId = userId;
        this.size = size;
    }

    @PropertyName("size")
    public String getSize() {
        return size;
    }

    @PropertyName("size")
    public void setSize(String Size) {
        this.size = size;
    }

    @PropertyName("torrentLink")
    public String getTorrentLink() {
        return torrentLink;
    }

    @PropertyName("torrentLink")
    public void setTorrentLink(String torrentLink) {
        this.torrentLink = torrentLink;
    }

    @PropertyName("website")
    public String getWebsite() {
        return website;
    }

    @PropertyName("website")
    public void setWebsite(String website) {
        this.website = website;
    }

    @PropertyName("title")
    public String getTitle() {
        return title;
    }

    @PropertyName("title")
    public void setTitle(String title) {
        this.title = title;
    }

    @PropertyName("infoHash")
    public String getInfoHash() {
        return infoHash;
    }

    @PropertyName("infoHash")
    public void setInfoHash(String infoHash) {
        this.infoHash = infoHash;
    }

    @PropertyName("magnetLink")
    public String getMagnetLink() {
        return magnetLink;
    }

    @PropertyName("magnetLink")
    public void setMagnetLink(String magnetLink) {
        this.magnetLink = magnetLink;
    }

    @PropertyName("timestamp")
    public Timestamp getTimestamp() {
        return timestamp;
    }

    @PropertyName("timestamp")
    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    @PropertyName("userId")
    public String getUserId() {
        return userId;
    }

    @PropertyName("userId")
    public void setUserId(String userId) {
        this.userId = userId;
    }

}