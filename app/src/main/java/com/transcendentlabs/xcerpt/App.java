package com.transcendentlabs.xcerpt;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.support.v7.app.AlertDialog;

import com.crashlytics.android.Crashlytics;
import com.twitter.sdk.android.Twitter;
import com.twitter.sdk.android.core.TwitterAuthConfig;

import io.fabric.sdk.android.Fabric;

/**
 * Created by Eric on 2016-03-10.
 */
public class App extends Application {
    public static final String TWITTER_KEY = BuildConfig.TWITTER_KEY;
    public static final String TWITTER_SECRET = BuildConfig.TWITTER_SECRET_KEY;

    private static App singleton;
    private static Typeface logoFont;
    private TwitterAuthConfig authConfig;

    public static App getInstance() {
        return singleton;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        singleton = this;
        logoFont = Typeface.createFromAsset(getAssets(), "chunk.otf");
        authConfig = new TwitterAuthConfig(TWITTER_KEY, TWITTER_SECRET);
        Fabric.with(this, new Crashlytics(), new Twitter(authConfig));
    }

    public static Typeface getLogoFont() {
        return logoFont;
    }

    public boolean isNetworkAvailable() {
        return ((ConnectivityManager) getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE))
                .getActiveNetworkInfo() != null;
    }

    public static String getDisplayUrl(String url) {
        String baseUrl = url;
        if(baseUrl.startsWith("https://www.")){
            baseUrl = baseUrl.substring("https://www.".length());
        }else if(baseUrl.startsWith("http://www.")){
            baseUrl = baseUrl.substring("http://www.".length());
        }else if(baseUrl.startsWith("https://")){
            baseUrl = baseUrl.substring("https://".length());
        }else if(baseUrl.startsWith("http://")){
            baseUrl = baseUrl.substring("http://".length());
        }
        int backslashAt = baseUrl.indexOf('/');
        if(backslashAt > 0){
            baseUrl = baseUrl.substring(0, backslashAt);
        }
        return baseUrl;
    }

    public void openTwitterProfile(String username) {
        Intent intent;
        try {
            // get the Twitter app if possible
            getPackageManager().getPackageInfo("com.twitter.android", 0);
            intent = new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("twitter://user?screen_name=" + username)
            );
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            // no Twitter app, revert to browser
            intent = new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://twitter.com/" + username)
            );
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }
}
