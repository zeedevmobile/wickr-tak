package com.atakmap.android.wickr;

import com.atakmap.android.cot.MarkerDetailHandler;
import com.atakmap.android.maps.Marker;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.log.Log;
import com.wickr.android.api.WickrAPIObjects;

import java.util.HashMap;
import java.util.Map;

public class WickrDetailHandler implements MarkerDetailHandler {

    private WickrAPIObjects.WickrUser contact;

    private static final String TAG = WickrDetailHandler.class.getSimpleName();

    public static final String DETAIL_NAME = "__wickr";

    public WickrAPIObjects.WickrUser getContact() {
        return contact;
    }

    public void setContact(WickrAPIObjects.WickrUser contact) {
        this.contact = contact;
    }

    @Override
    public void toMarkerMetadata(Marker marker, CotEvent event, CotDetail detail) {
        Log.d(TAG, "detail received: " + detail + " in:  " + event);
        marker.setMetaString("wickrId", detail.getAttribute("wickrId"));
        marker.setMetaString("username", detail.getAttribute("username"));
        marker.setMetaString("fullname", detail.getAttribute("fullname"));
    }

    public static CotDetail createDetail(String wickrId, String username, String fullname) {
        CotDetail detail = new CotDetail(DETAIL_NAME);
        detail.setAttribute("wickrId", wickrId);
        detail.setAttribute("username", username);
        detail.setAttribute("fullname", fullname);
        return detail;
    }

    @Override
    public void toCotDetail(Marker marker, CotDetail detail) {
        if (contact != null) {
            Log.d(TAG, "converting to: " + detail);
            CotDetail special = new CotDetail(DETAIL_NAME);
            special.setAttribute("wickrId", contact.getId());
            special.setAttribute("username", contact.getUsername());
            special.setAttribute("fullname", contact.getFullName());
            detail.addChild(special);
        }
    }
}
