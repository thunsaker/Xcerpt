package com.transcendentlabs.xcerpt;

import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * Created by Eric on 2016-03-25.
 */
public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
}
