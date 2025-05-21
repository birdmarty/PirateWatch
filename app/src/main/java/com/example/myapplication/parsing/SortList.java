package com.example.myapplication.parsing;


import android.util.Log;

public class SortList {
    private String sortOneThree;
    private String sortLimeTorrents;
    private String sortItem;

    public SortList(String sortItem) {
        switch (sortItem) {
            case "Seeders":
                sortOneThree = "/seeders/desc/";
                sortLimeTorrents = "/seeds/";
                break;
            case "Seeders DESC":
                sortOneThree = "/seeders/desc/";
                sortLimeTorrents = "/seeds/";
                break;
            case "Seeders ASC":
                sortOneThree = "/seeders/asc/";
                sortLimeTorrents = "/seeds/";
                break;

            case "Leechers":
                sortOneThree = "/leechers/desc/";
                sortLimeTorrents = "/leechs/";
                break;
            case "Leechers DESC":
                sortOneThree = "/leechers/desc/";
                sortLimeTorrents = "/leechs/";
                break;
            case "Leechers ASC":
                sortOneThree = "/leechers/asc/";
                sortLimeTorrents = "/leechs/";
                break;

            case "Size":
                sortOneThree = "/size/desc/";
                sortLimeTorrents = "/size/";
                break;
            case "Size DESC":
                sortOneThree = "/size/desc/";
                sortLimeTorrents = "/size/";
                break;
            case "Size ASC":
                sortOneThree = "/size/asc/";
                sortLimeTorrents = "/size/";
                break;

            case "Date":
                sortOneThree = "/time/desc/";
                sortLimeTorrents = "/date/";
                break;
            case "Date DESC":
                sortOneThree = "/time/desc/";
                sortLimeTorrents = "/date/";
                break;
            case "Date ASC":
                sortOneThree = "/time/asc/";
                sortLimeTorrents = "/date/";
                break;
        }
    }


    public String urlLimeTorrents (String keyword){
        return Url.UrlLimeTorrent + "search/all/" + keyword + sortLimeTorrents + "1/";
    }


    public String getOneThreeSort() {
        Log.d("sort", sortItem + "," + sortOneThree);
        return sortOneThree;
    }

    public String getLimeSort() {
        Log.d("sort", sortItem + "," + sortLimeTorrents);
        return sortLimeTorrents;
    }

    public String urlSortSearch (String keyword){
        return "https://www.1337x.to/" + "sort-search/" + keyword + sortOneThree + "1/";
    }
}