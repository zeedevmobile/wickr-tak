package com.atakmap.android.wickr.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.atakmap.android.gui.PluginSpinner;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.wickr.WickrAPIPairedEvent;
import com.atakmap.android.wickr.WickrAPIReceiver;
import com.atakmap.android.wickr.WickrMapComponent;
import com.atakmap.android.wickr.WickrSyncingEvent;
import com.atakmap.android.wickr.plugin.R;
import com.atakmap.android.wickr.utils.SettingsManager;
import com.atakmap.coremap.log.Log;
import com.wickr.android.api.WickrAPI;

import org.greenrobot.eventbus.Subscribe;

public class PairFragment extends Fragment {

    private static final String TAG = PairFragment.class.getSimpleName();
    private Context pluginContext;
    private static PairFragment instance;

    private TextView status;

    private PairFragment(Context pluginContext) {
        this.pluginContext = pluginContext;
    }

    public static PairFragment newInstance(Context pluginContext) {
        if (instance == null) {
            instance = new PairFragment(pluginContext);
        }
        return instance;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = LayoutInflater.from(pluginContext).inflate(R.layout.pair, container, false);
        SettingsManager settingsManager = new SettingsManager(pluginContext);

        Button pairBtn = v.findViewById(R.id.pairBtn);
        status = v.findViewById(R.id.pairingStatus);
        pairBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent pairingIntent = WickrAPI.INSTANCE.createPairingIntent(
                        pluginContext,
                        "com.atakmap.app.civ",
                        "Wickr ATAK Plugin",
                        WickrAPIReceiver.PAIRING_IDENTIFIER
                );
                Log.d(TAG, "Sending the pairing request " + pluginContext.getPackageName());
                AtakBroadcast.getInstance().sendSystemBroadcast(pairingIntent);
                status.setText(pluginContext.getString(R.string.pairing_requested));
            }
        });
        ImageView logo = v.findViewById(R.id.logo);

        PluginSpinner flavorChooser = v.findViewById(R.id.flavorChooser);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(pluginContext,
                R.array.app_flavors, R.layout.spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(R.layout.spinner_item);
        // Apply the adapter to the spinner
        flavorChooser.setAdapter(adapter);
        flavorChooser.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long l) {
                String selection = adapter.getItem(pos).toString();
                String pkg = "";
                if (selection.equals(pluginContext.getString(R.string.enterprise))) {
                    logo.setImageDrawable(pluginContext.getDrawable(R.drawable.wickr_logo_ent));
                    pkg = pluginContext.getString(R.string.ent_pkg);

                } else if (selection.equals(pluginContext.getString(R.string.ent_beta))) {
                    logo.setImageDrawable(pluginContext.getDrawable(R.drawable.wickr_logo_ent));
                    pkg = pluginContext.getString(R.string.ent_beta_pkg);
                } else if (selection.equals(pluginContext.getString(R.string.pro_beta))) {
                    logo.setImageDrawable(pluginContext.getDrawable(R.drawable.wickr_logo_pro));
                    pkg = pluginContext.getString(R.string.pro_beta_pkg);
                } else if (selection.equals(pluginContext.getString(R.string.aws))) {
                    logo.setImageDrawable(pluginContext.getDrawable(R.drawable.wickr_logo_aws));
                    pkg = pluginContext.getString(R.string.pro_pkg);
                } else if (selection.equals(pluginContext.getString(R.string.wickrgov))) {
                    logo.setImageDrawable(pluginContext.getDrawable(R.drawable.wickr_logo_gov));
                    pkg = pluginContext.getString(R.string.wickrgov_pkg);
                } else {
                    pkg = pluginContext.getString(R.string.pro_pkg);
                }
                WickrAPI.INSTANCE.setTargetApp(pkg);
                settingsManager.savePref("WickrPkg", pkg);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        WickrMapComponent.EVENTBUS.register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        WickrMapComponent.EVENTBUS.unregister(this);
    }

    @Subscribe
    public void onWickrAPIEvent(Object event) {
        if (event instanceof WickrAPIPairedEvent) {
            status.setText(pluginContext.getString(R.string.pairing_complete));
        } else if (event instanceof WickrSyncingEvent) {
            status.setText(pluginContext.getString(R.string.syncing));
        }
    }
}
