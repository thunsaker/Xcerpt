package ericbai.com.sharepiece;

import android.os.AsyncTask;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class ParseHtmlAsyncTask extends AsyncTask<Void, Void, Void> {

    private String mUrl;
    private Callback mCallback;
    private Error mError;

    private String title;

    public ParseHtmlAsyncTask(String url, Callback callback) {
        mUrl = url;
        mCallback = callback;
    }

    @Override
    protected Void doInBackground(Void... params) {
        try {
            Document doc = Jsoup.connect(mUrl).get();

            Elements twitterTitleElems = doc.select("meta[name=twitter:title]");
            if(!twitterTitleElems.isEmpty() && twitterTitleElems.first() != null){
                title = twitterTitleElems.select("content").first().toString();
                return null;
            }

            Elements ogTitleElems = doc.select("meta[name=og:title]");
            if(!ogTitleElems.isEmpty() && ogTitleElems.first() != null){
                title = ogTitleElems.select("content").first().toString();
                return null;
            }

            title = doc.title();

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