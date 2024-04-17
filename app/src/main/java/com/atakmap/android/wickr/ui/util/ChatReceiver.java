package com.atakmap.android.wickr.ui.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.wickr.MessageDropDownReceiver;
import com.atakmap.android.wickr.Requests;
import com.atakmap.android.wickr.WickrConvoCreatedEvent;
import com.atakmap.android.wickr.plugin.R;

import org.greenrobot.eventbus.Subscribe;

public class ChatReceiver extends BroadcastReceiver {

    private Context pluginContext;

    public ChatReceiver(Context pluginContext) {
        this.pluginContext = pluginContext;
    }

    @Subscribe
    public void onWickrAPIEvent(Object event) {
        if (event instanceof WickrConvoCreatedEvent) {
            Intent i = new Intent(MessageDropDownReceiver.SHOW_MESSAGE);
            i.putExtra("convo", ((WickrConvoCreatedEvent) event).getConvo());
            AtakBroadcast.getInstance().sendBroadcast(i);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String uid = intent.getStringExtra("uid");
        Marker m = (Marker) MapView.getMapView().getRootGroup().deepFindUID(uid);
        String wickrId = m.getMetaString("wickrId", "");
        if (wickrId != null && !wickrId.isEmpty()) {
            Requests r = new Requests(pluginContext);
            r.createConvo(wickrId, "");
        } else {
            Toast.makeText(MapView.getMapView().getContext(), pluginContext.getString(R.string.no_user), Toast.LENGTH_SHORT).show();
        }
    }
}
