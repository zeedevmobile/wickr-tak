package com.atakmap.android.wickr;

import android.content.Context;

import com.atakmap.android.contact.Connector;
import com.atakmap.android.wickr.plugin.R;
import com.atakmap.coremap.log.Log;

public class WickrConnector extends Connector {

    private Context pluginContext;

    private static final String TAG = WickrConnector.class.getSimpleName();

    public WickrConnector(Context pluginContext) {
        this.pluginContext = pluginContext;
    }

    @Override
    public String getConnectionString() {
        return "wickr://test";
    }

    @Override
    public String getConnectionType() {
        return "connector.wickr";
    }

    @Override
    public String getConnectionLabel() {
        return "Wickr ATAK Plugin";
    }

    @Override
    public String getIconUri() {
        Log.d(TAG, "shb:  getIconUri()");
        return "android.resource://"
                + pluginContext
                .getPackageName()
                + "/"
                + R.drawable.tak_logo;
    }
}
