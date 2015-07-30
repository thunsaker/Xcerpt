package com.transcendentlabs.xcerpt;

import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Random;

public class SearchAsyncTask extends AsyncTask<Void, Void, Void> {

    private final String TAG = getClass().getName();
    private final int MAX_QUERY_LENGTH = 1961;

    private String mSearchStr;
    private int mNumOfResults = 0;

    private Callback mCallback;
    private BingSearchResults mBingSearchResults;
    private Error mError;

    public SearchAsyncTask(String searchStr, int numOfResults, Callback callback) {
        mSearchStr = "\"" + searchStr +  "\"";
        mNumOfResults = numOfResults;
        mCallback = callback;
    }

    @Override
    protected Void doInBackground(Void... params) {
        try {
            mSearchStr = mSearchStr.replaceAll("\\s+", " ");
            // mSearchStr = mSearchStr.replaceAll("\\p{P}", "");

            int numSpaces = mSearchStr.length() - mSearchStr.replaceAll(" ", "").length();
            int less = 10;
            int encodedLength = mSearchStr.length() + numSpaces*2;
            String subSearch = mSearchStr;
            while(encodedLength > MAX_QUERY_LENGTH){
                int lastSpace = subSearch.lastIndexOf(" ", MAX_QUERY_LENGTH - less);
                subSearch = subSearch.substring(0, lastSpace);
                numSpaces = subSearch.length() - subSearch.replaceAll(" ", "").length();
                encodedLength = subSearch.length() + numSpaces*2;
                less = less*2;
                Log.e("temp", subSearch);
            }

            String searchStr = URLEncoder.encode(subSearch);
            Log.e("temp", searchStr);
            String numOfResultsStr = mNumOfResults <= 0 ? "" : "&$top=" + mNumOfResults;

            Random rand = new Random();
            int randomNum = rand.nextInt(2);
            String bingUrl;
            if(randomNum == 0){
                bingUrl =
                        "https://api.datamarket.azure.com/Bing/Search/v1/Web?Query=%27"
                                + searchStr + "%27" + numOfResultsStr + "&$format=json";
            }else{
                bingUrl =
                        "https://api.datamarket.azure.com/Bing/SearchWeb/v1/Web?Query=%27"
                                + searchStr + "%27" + numOfResultsStr + "&$format=json";
            }

            String accountKey = BuildConfig.BING_ACCOUNT_KEY;
            String auth = accountKey + ":" + accountKey;
            String encodedAuth = Base64.encodeToString(auth.getBytes(), Base64.NO_WRAP);

            URL url = new URL(bingUrl);

            URLConnection urlConnection = url.openConnection();
            urlConnection.setRequestProperty("Authorization", "Basic " + encodedAuth);
            InputStream response = urlConnection.getInputStream();
            String res = readStream(response);

            Gson gson = (new GsonBuilder()).create();
            mBingSearchResults = gson.fromJson(res, BingSearchResults.class);

            Log.d(TAG, res);
            //conn.disconnect();

        } catch (Exception e) {
            e.printStackTrace();
            mError = new Error(e.getMessage(), e);
            //Log.e(TAG, e.getMessage());
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        super.onPostExecute(result);

        if (mCallback != null) {
            mCallback.onComplete(mBingSearchResults, mError);
        }

    }

    private String readStream(InputStream in) {
        BufferedReader reader = null;
        StringBuilder sb = new StringBuilder();
        try {
            reader = new BufferedReader(new InputStreamReader(in));
            String line = "";
            while ((line = reader.readLine()) != null) {
                //System.out.println(line);
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return sb.toString();


    }
    public interface Callback {
        void onComplete(Object o, Error error);
    }
}