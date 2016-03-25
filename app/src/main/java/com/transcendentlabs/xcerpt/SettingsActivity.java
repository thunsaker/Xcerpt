package com.transcendentlabs.xcerpt;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.Window;

import static com.transcendentlabs.xcerpt.Util.setActionBarColour;

/**
 * Created by Eric on 2016-03-25.
 */
public class SettingsActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar bar = getSupportActionBar();
        Window window = getWindow();
        setActionBarColour(bar, window, this);

        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }
}
