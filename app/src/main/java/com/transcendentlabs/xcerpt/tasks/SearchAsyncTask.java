package com.transcendentlabs.xcerpt.tasks;

import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.transcendentlabs.xcerpt.BingSearchResults;
import com.transcendentlabs.xcerpt.BuildConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Performs a web search on text
 */
public class SearchAsyncTask extends AsyncTask<Void, Void, Void> {

    private final String TAG = getClass().getName();
    private final int MAX_QUERY_LENGTH = 1961;
    private String mSearchStr;
    private int mNumOfResults = 0;

    private Callback mCallback;
    private BingSearchResults mBingSearchResults;
    private Error mError;

    public SearchAsyncTask(String searchStr, int numOfResults, Callback callback) {
        mSearchStr = searchStr;
        mNumOfResults = numOfResults;
        mCallback = callback;
    }

    @Override
    protected Void doInBackground(Void... params) {
        try {
            // remove all non-alphabet characters and extra whitespace
            String[] words = mSearchStr.toLowerCase().split("\\s+");
            for(int i = 0; i < words.length; i++) {
                words[i] = words[i].trim().replaceFirst("^[^^\\p{L}0-9]+", "").replaceAll("[^^\\p{L}0-9]+$", "");
            }

            int startBatch = 0;
            int endBatch = 20;
            String finalQuery;
            if(words.length <= endBatch) {
                finalQuery = "\"" + TextUtils.join(" ", words) + "\"";
                finalQuery = truncateQuery(finalQuery);
            }
            else if (words.length <= endBatch * 2){
                String query1 = "";
                for (int i = 0; i < words.length / 2; i++) {
                    query1 = query1 + " " + words[i] + " ";
                }
                String query2 = "";
                for (int i = words.length / 2; i < words.length; i++) {
                    query2 = query2 + " " + words[i] + " ";
                }
                finalQuery = "\"" + query1.trim() + "\"" + " | " + "\"" + query2.trim() + "\"";
                String encoded = URLEncoder.encode(finalQuery, "UTF-8");
                if(encoded.length() > MAX_QUERY_LENGTH) {
                    finalQuery = "\"" + query1.trim() + "\"";
                    finalQuery = truncateQuery(finalQuery);
                }
            }
            else {
                ArrayList<String> queries = new ArrayList<>();
                while (endBatch < words.length) {
                    String query = "";
                    for (int i = startBatch; i < endBatch; i++) {
                        query = query + " " + words[i] + " ";
                    }
                    query = "\"" + query.trim() + "\"";
                    queries.add(query);
                    if(queries.size() > 3) {
                        break;
                    }
                    startBatch = endBatch;
                    endBatch += 20;
                }
                finalQuery = TextUtils.join(" | ", queries);
                String encoded = URLEncoder.encode(finalQuery, "UTF-8");
                while(encoded.length() > MAX_QUERY_LENGTH) {
                    if(queries.size() == 1) {
                        finalQuery = truncateQuery(finalQuery);
                        break;
                    }
                    queries.remove(queries.size() - 1);
                    finalQuery = TextUtils.join(" | ", queries);
                    encoded = URLEncoder.encode(finalQuery, "UTF-8");
                }
            }

            Log.e("search query", finalQuery);
            String searchStr = URLEncoder.encode(finalQuery, "UTF-8");
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

    private String truncateQuery(String finalQuery) throws UnsupportedEncodingException {
        int less = 5;
        String encoded = URLEncoder.encode(finalQuery, "UTF-8");
        while(encoded.length() > MAX_QUERY_LENGTH){
            int lastSpace = finalQuery.lastIndexOf(" ", MAX_QUERY_LENGTH - less);

            if(lastSpace == -1){
                finalQuery = finalQuery.substring(0, MAX_QUERY_LENGTH - less) + "\"";
            }else {
                finalQuery = finalQuery.substring(0, lastSpace) + "\"";
                encoded = URLEncoder.encode(finalQuery, "UTF-8");
                less = less * 2;
            }
        }
        return finalQuery;
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
            String line;
            while ((line = reader.readLine()) != null) {
                //System.out.println(line);
                sb.append(line).append("\n");
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