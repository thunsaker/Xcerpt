package com.transcendentlabs.xcerpt;

public class BingSearchResults {

    public ResultsContent d;

    public static class ResultsContent {
        public Result[] results;
    }

    public static class Result {
        public String DisplayUrl;
        public String Url;

    }

    public Result[] getResults(){
        if (d == null)
            return null;
        return d.results;
    }
}