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
import com.atakmap.android.wickr.WickrConvoCreatedEvent;
import com.atakmap.android.wickr.WickrConvoCreatedForPhoneEvent;
import com.atakmap.android.wickr.plugin.R;
import com.atakmap.android.wickr.utils.SettingsManager;
import com.wickr.android.api.WickrAPI;
import com.wickr.android.api.WickrAPIObjects;
import com.wickr.android.api.WickrAPIRequests;

import org.greenrobot.eventbus.Subscribe;

public class PhoneConvoReceiver extends BroadcastReceiver {

    private Context pluginContext;
    private SettingsManager settingsManager;

    public static String FOR_PHONE_CALL = "ForPhoneCallToStart";

    private static final String TAG = PhoneConvoReceiver.class.getSimpleName();

    public PhoneConvoReceiver(Context pluginContext) {
        this.pluginContext = pluginContext;
        this.settingsManager = new SettingsManager(pluginContext);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String uid = intent.getStringExtra("uid");
        Marker m = (Marker) MapView.getMapView().getRootGroup().deepFindUID(uid);
        String wickrId = m.getMetaString("wickrId", "");
        if (wickrId != null && !wickrId.isEmpty()) {

            byte[] key = settingsManager.getKey();
            if (key == null) {
                return;
            }
            Log.d(TAG, "Initiate create convo request get a convo id for the phone call");
            WickrAPIRequests.CreateConvoRequest createConvoRequest = WickrAPIRequests.CreateConvoRequest.newBuilder()
                    .setType(WickrAPIObjects.WickrConvo.ConvoType.DM)
                    .addUserIDs(wickrId)
                    .build();

            Intent cCRIntent = WickrAPI.INSTANCE.createRequestIntent(Requests.Companion.getPACKAGE_NAME(), createConvoRequest, key, FOR_PHONE_CALL);
            AtakBroadcast.getInstance().sendSystemBroadcast(cCRIntent);
        } else {
            Toast.makeText(MapView.getMapView().getContext(), pluginContext.getString(R.string.no_user), Toast.LENGTH_SHORT).show();
        }
    }



    @Subscribe
    public void onWickrAPIEvent(Object event) {
        if (event instanceof WickrConvoCreatedForPhoneEvent) {
            WickrConvoCreatedForPhoneEvent convoCreatedEvent = (WickrConvoCreatedForPhoneEvent) event;
            Requests r = new Requests(pluginContext);
            Log.d(TAG, "Starting phone call with " + convoCreatedEvent.getConvo().getId());
            r.startCall(convoCreatedEvent.getConvo().getId());
        }
    }
}
