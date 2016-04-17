package com.transcendentlabs.xcerpt;

public class BingSearchResults {

    public ResultsContent resultsContent;

    public static class ResultsContent {
        public Result[] results;
    }

    public static class Result {
        public String id;
        public String title;
        public String description;
        public String displayUrl;
        public String url;
    }

    public Result[] getResults(){
        if (resultsContent == null)
            return null;
        return resultsContent.results;
    }
}