package com.atakmap.android.wickr;

import android.content.Intent;

import com.atakmap.android.contact.ContactConnectorManager;
import com.atakmap.android.ipc.AtakBroadcast;

public class WickrContactConnectionManager extends ContactConnectorManager.ContactConnectorHandler {

    @Override
    public boolean isSupported(String connectorType) {
        return connectorType.equals("connector.wickr");

    }

    @Override
    public boolean hasFeature(ContactConnectorManager.ConnectorFeature feature) {
        return false;
    }

    @Override
    public String getName() {
        return "Wickr";
    }

    @Override
    public String getDescription() {
        return "Wickr Capability for TAK";
    }

    @Override
    public boolean handleContact(String connectorType, String contactUID, String connectorAddress) {
        Intent i = new Intent("com.atakmap.android.wickr.launch");
        i.putExtra("uid", contactUID);
        AtakBroadcast.getInstance().sendBroadcast(i);
        return true;
    }

    @Override
    public Object getFeature(String connectorType, ContactConnectorManager.ConnectorFeature feature, String contactUID, String connectorAddress) {
        return null;
    }
}
