package com.atakmap.android.wickr;

import android.content.Context;
import android.content.Intent;

import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.maps.MapView;
import com.wickr.android.api.WickrAPIObjects;

import org.greenrobot.eventbus.Subscribe;


public class WickrDetail extends DropDownReceiver {

    public static final String ACTION = "com.atakmap.android.wickr.detail";

    private Context pluginContext;

    public WickrDetail(final MapView mapView, final Context pluginContext) {
        super(mapView);
        this.pluginContext = pluginContext;
    }

    @Override
    protected void disposeImpl() {
        WickrMapComponent.EVENTBUS.unregister(this);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        WickrMapComponent.EVENTBUS.register(this);
        final String action = intent.getAction();
        if (action != null && ACTION.equals(action)) {

        }
    }

    @Subscribe
    private void onWickrAPI(Object event) {

    }
}
