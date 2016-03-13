package com.transcendentlabs.xcerpt;

import android.app.Application;
import android.content.Context;
import android.graphics.Typeface;
import android.net.ConnectivityManager;

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
}
