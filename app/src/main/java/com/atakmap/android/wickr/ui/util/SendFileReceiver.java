package com.atakmap.android.wickr.ui.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.wickr.Requests;
import com.atakmap.android.wickr.SendFileDropDownReceiver;
import com.atakmap.android.wickr.SendFileFragmentEvent;
import com.atakmap.android.wickr.WickrMapComponent;
import com.atakmap.android.wickr.plugin.R;
import com.atakmap.android.wickr.ui.SendFileFragment;
import com.atakmap.android.wickr.utils.SettingsManager;
import com.wickr.android.api.WickrAPI;
import com.wickr.android.api.WickrAPIObjects;
import com.wickr.android.api.WickrAPIRequests;

public class SendFileReceiver extends BroadcastReceiver {

    private Context pluginContext;
    private SettingsManager settingsManager;

    public SendFileReceiver(Context pluginContext) {
        super();
        this.pluginContext = pluginContext;
        settingsManager = new SettingsManager(pluginContext);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String uid = intent.getStringExtra("uid");
        Marker m = (Marker) MapView.getMapView().getRootGroup().deepFindUID(uid);
        String wickrId = m.getMetaString("wickrId", "");
        if (wickrId != null && !wickrId.isEmpty()) {
            String fullname = m.getMetaString("fullname", "");
            Intent i = new Intent();
            i.setAction(SendFileDropDownReceiver.SENDFILE_MESSAGE);
            i.putExtra("wickrId", wickrId);
            i.putExtra("fullname", fullname);
            AtakBroadcast.getInstance().sendBroadcast(i);
        } else {
            Toast.makeText(MapView.getMapView().getContext(), pluginContext.getString(R.string.no_user), Toast.LENGTH_SHORT).show();
        }
    }
}
