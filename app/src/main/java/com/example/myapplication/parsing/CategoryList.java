package com.example.myapplication.parsing;

public class CategoryList {
    private String OneThreeCategory;
    private String LimeCategory;


    public CategoryList(String category) {
        switch (category) {
            case "Movies":
                OneThreeCategory = "/Movies";
                LimeCategory = "movies/";
                break;
            case "All":
                OneThreeCategory = "/Movies";
                LimeCategory = "all/";
                break;
            case "TV Shows":
                OneThreeCategory = "/TV";
                LimeCategory = "tv/";
                break;
            case "Games":
                OneThreeCategory = "/Games";
                LimeCategory = "games/";
                break;
            case "Music":
                OneThreeCategory = "/Music";
                LimeCategory = "music/";
                break;
            case "Applications":
                OneThreeCategory = "/Applications";
                LimeCategory = "applications/";
                break;
            case "Documentaries":
                OneThreeCategory = "/Documentaries";
                break;
            case "Anime":
                OneThreeCategory = "/Aninme";
                LimeCategory = "anime/";
                break;
            case "Other":
                OneThreeCategory = "/Other";
                LimeCategory = "other/";
                break;
            case "XXX":
                OneThreeCategory = "/XXX";
                break;
        }
    }

    public String getOneThreeCategory() {
        return OneThreeCategory;
    }

    public String getLimeCategory() {
        return LimeCategory;
    }

    public String OneThreeCategorySearch(String keyword) {
        return "https://www.1337x.to/category-search/" + keyword + OneThreeCategory + "/1/";
    }

    public String LimeCategorySearch(String keyword) {
        return "https://www.limetorrents.lol/search/" + LimeCategory + keyword + "/";
    }


}
