package com.atakmap.android.wickr.ui;

import android.annotation.SuppressLint;
import android.app.UiAutomation;
import android.content.Context;
import android.os.Bundle;
import android.preference.Preference;

import com.atakmap.android.preference.AtakPreferenceFragment;
import com.atakmap.android.preference.PluginPreferenceFragment;
import com.atakmap.android.wickr.Requests;
import com.atakmap.android.wickr.plugin.R;

public class WickrPluginPreferenceFragment extends PluginPreferenceFragment {
    private static Context staticPluginContext;

    /**
     * Only will be called after this has been instantiated with the 1-arg constructor.
     * Fragments must has a zero arg constructor.
     */
    public WickrPluginPreferenceFragment() {
        super(staticPluginContext, R.xml.preferences);
    }

    @SuppressLint("ValidFragment")
    public WickrPluginPreferenceFragment(final Context pluginContext) {
        super(pluginContext, R.xml.preferences);
        staticPluginContext = pluginContext;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Preference unpairPref = findPreference("wickr_unpair_plugin");
        /*unpairPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

        });*/
    }
}
