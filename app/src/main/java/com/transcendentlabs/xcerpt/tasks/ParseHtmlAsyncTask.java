package com.transcendentlabs.xcerpt.tasks;

import android.os.AsyncTask;
import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

/**
 * Gets the title of an article from a URL
 */
public class ParseHtmlAsyncTask extends AsyncTask<Void, Void, Void> {

    private String mUrl;
    private Callback mCallback;
    private Error mError;

    private String title;

    public ParseHtmlAsyncTask(String url, Callback callback) {
        mUrl = url;
        if(!url.startsWith("http")){
            mUrl = "http://" + url;
        }
        mCallback = callback;
    }

    @Override
    protected Void doInBackground(Void... params) {
        try {
            Document doc = Jsoup.connect(mUrl).ignoreContentType(true).get();

            Elements twitterTitleElems = doc.select("meta[property=twitter:title]");
            if (twitterTitleElems!=null && twitterTitleElems.attr("content").length() > 0) {
                title = twitterTitleElems.attr("content");
                Log.e("ParseHTMLTask", "Twitter title: " + title);
                return null;
            }

            Elements ogTitleElems = doc.select("meta[property=og:title]");
            if (ogTitleElems!=null && ogTitleElems.attr("content").length() > 0) {
                title = ogTitleElems.attr("content");
                Log.e("ParseHTMLTask", "OG title: " + title);
                return null;
            }

            title = doc.title();
            Log.e("ParseHTMLTask", "HTML title: " + title);

        } catch (Throwable t) {
            t.printStackTrace();
            mError = new Error(t.getMessage(), t);
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void s) {
        super.onPostExecute(s);
        if (mCallback != null) {
            mCallback.onComplete(title, mError);
        }
    }

    public interface Callback {
        void onComplete(Object o, Error error);
    }
}