package com.transcendentlabs.xcerpt;

public class Article{
    String title;
    String displayUrl;
    String url;

    public Article(String title, String displayUrl, String url){
        this.title = title;
        this.displayUrl = displayUrl;
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public String getDisplayUrl() {
        return displayUrl;
    }

    public String getUrl() {
        return url;
    }
}
