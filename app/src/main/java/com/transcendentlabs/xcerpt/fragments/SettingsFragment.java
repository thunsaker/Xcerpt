package com.transcendentlabs.xcerpt.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import com.transcendentlabs.xcerpt.App;
import com.transcendentlabs.xcerpt.R;
import com.transcendentlabs.xcerpt.activities.SettingsActivity;

/**
 * Created by Eric on 2016-03-25.
 */
public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        initOpenTwitterAccount();
        initOpenPlayStorePage();
    }

    private void initOpenPlayStorePage() {
        Preference openXcerptPlayStorePreference = findPreference(SettingsActivity.KEY_GO_TO_PLAY_STORE);
        openXcerptPlayStorePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                final String appPackageName = App.getInstance().getPackageName();
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                } catch (android.content.ActivityNotFoundException anfe) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                }
                return true;
            }
        });
    }

    private void initOpenTwitterAccount() {
        Preference openXcerptTwitterAccountPreference = findPreference(SettingsActivity.KEY_SEE_XCERPT_APP);
        openXcerptTwitterAccountPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                App.getInstance().openTwitterProfile("XcerptApp");
                return true;
            }
        });
    }
}
