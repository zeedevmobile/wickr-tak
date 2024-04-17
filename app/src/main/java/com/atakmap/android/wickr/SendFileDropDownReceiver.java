package com.atakmap.android.wickr;

import android.content.Context;
import android.content.Intent;

import com.atakmap.android.dropdown.DropDown;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.wickr.ui.SendFileFragment;
import com.atakmap.android.wickr.ui.util.SendFileReceiver;

import org.greenrobot.eventbus.Subscribe;

public class SendFileDropDownReceiver extends DropDownReceiver implements DropDown.OnStateListener {

    public static final String SENDFILE_MESSAGE = "com.atakmap.android.wickr.SHOW_SENDFILE";
    private Context pluginContext;
    private SendFileFragment sendFileFragment;
    private boolean dropDownVisible = false;


    protected SendFileDropDownReceiver(MapView mapView, Context pluginContext) {
        super(mapView);
        this.pluginContext = pluginContext;
        WickrMapComponent.EVENTBUS.register(this);
    }

    @Subscribe
    public void onWickrAPIEvent(Object event) {
        if (event instanceof RequestCloseDropDownEvent) {
            closeDropDown();
        }
    }


    @Override
    public void onDropDownSelectionRemoved() {

    }

    @Override
    public void onDropDownClose() {
        dropDownVisible = false;
    }

    @Override
    public void onDropDownSizeChanged(double v, double v1) {

    }

    @Override
    public void onDropDownVisible(boolean b) {

    }

    @Override
    protected void disposeImpl() {
        WickrMapComponent.EVENTBUS.unregister(this);
        dropDownVisible = false;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(SENDFILE_MESSAGE)) {
            sendFileFragment = new SendFileFragment(pluginContext, intent.getStringExtra("wickrId"), "", true);

            sendFileFragment.setRecipientTitle(intent.getStringExtra("fullname"));
            if (!dropDownVisible || isClosed()) {
                showDropDown(sendFileFragment, HALF_WIDTH, FULL_HEIGHT, FULL_WIDTH,
                        HALF_HEIGHT, false, false, this);
                dropDownVisible = true;
            }
        }
    }
}
