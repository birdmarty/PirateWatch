package com.example.myapplication.parsing;


public class SearchResult {
    private final String title, seeds, leeches, size, website, link, infoHash;

    public SearchResult(String title, String seeds, String leeches, String size, String website, String link, String infoHash) {
        this.title = title;
        this.seeds = seeds;
        this.leeches = leeches;
        this.size = size;
        this.website = website;
        this.link = link;
        this.infoHash = infoHash;
    }

    public String getTitle() {
        return title;
    }

    public String getSeeds() {
        return seeds;
    }

    public String getLeeches() {
        return leeches;
    }

    public String getWebsite() {
        return website;
    }

    public String getSize() {
        return size;
    }

    public String getLink() {
        return link;
    }

    public String getInfoHash() {
        return infoHash;
    }
}

