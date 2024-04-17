package com.atakmap.android.wickr;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;

import com.atakmap.android.dropdown.DropDown;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.wickr.plugin.R;
import com.atakmap.android.wickr.ui.ChoreographerFragment;
import com.atakmap.android.wickr.ui.ConvoFragment;
import com.atakmap.android.wickr.ui.SendFileFragment;
import com.atakmap.android.wickr.ui.SendVoiceMessageFragment;
import com.atakmap.android.wickr.utils.SettingsManager;
import com.wickr.android.api.WickrAPIObjects;

import org.greenrobot.eventbus.Subscribe;

public class MessageDropDownReceiver extends DropDownReceiver implements DropDown.OnStateListener {

    public static final String SHOW_MESSAGE = "com.atakmap.android.wickr.SHOW_MESSAGE";
    private Context pluginContext;
    private ConvoFragment convoFragment;

    private boolean dropDownVisible = false;

    private SettingsManager settingsManager;
    private ChoreographerFragment choreographerFragment;
    
    public MessageDropDownReceiver(MapView mapView, Context pluginContext) {
        super(mapView);
        this.pluginContext = pluginContext;
        settingsManager = new SettingsManager(pluginContext);
        WickrMapComponent.EVENTBUS.register(this);
    }
    
    @Override
    protected void disposeImpl() {
        dropDownVisible = false;
        WickrMapComponent.EVENTBUS.unregister(this);
    }

    @Override
    protected void hideDropDown() {
        super.hideDropDown();
        dropDownVisible = false;
    }

    protected MessageDropDownReceiver(MapView mapView) {
        super(mapView);
    }

    @Subscribe
    public void onWickrAPIEvent(Object event) {
        if (event instanceof RequestCloseDropDownEvent) {
            closeDropDown();
        } else if (event instanceof MessageFragmentEvent) {
            WickrAPIObjects.WickrConvo convo = ((MessageFragmentEvent) event).getConvo();
            choreographerFragment.showConvoFragment(convo);
        } else if (event instanceof SendVoiceMessageFragmentEvent) {
            SendVoiceMessageFragmentEvent svmfe = (SendVoiceMessageFragmentEvent) event;
            SendVoiceMessageFragment svmf = new SendVoiceMessageFragment(pluginContext, ((SendVoiceMessageFragmentEvent) event).getUserId(), svmfe.getConvoId(), false);
            svmf.setRecipientTitle(svmfe.getFullname());
            choreographerFragment.showFragment(svmf, null, true);
        } else if (event instanceof SendFileInlineFragmentEvent) {
            SendFileInlineFragmentEvent sfife = (SendFileInlineFragmentEvent) event;
            SendFileFragment sff = new SendFileFragment(pluginContext, ((SendFileInlineFragmentEvent) event).getUserId(), sfife.getConvoId(), false);
            sff.setRecipientTitle(sfife.getFullname());
        } else if (event instanceof PopFragmentEvent) {
            if (choreographerFragment != null) {
                choreographerFragment.onBackButtonPressed();
            }
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(SHOW_MESSAGE)) {
            if (!settingsManager.isPaired()) {
                Intent showMain = new Intent();
                showMain.setAction(WickrDropDownReceiver.SHOW_PLUGIN);
                AtakBroadcast.getInstance().sendBroadcast(showMain);
                return;
            }
            WickrAPIObjects.WickrConvo convo = (WickrAPIObjects.WickrConvo) intent.getSerializableExtra("convo");

            if (convo != null) {
                convoFragment = new ConvoFragment(pluginContext, convo, true);
                choreographerFragment = ChoreographerFragment.newInstance(pluginContext, convoFragment);
                if (!dropDownVisible || isClosed()) {
                    showDropDown(choreographerFragment, HALF_WIDTH, FULL_HEIGHT, FULL_WIDTH,
                            HALF_HEIGHT, false, false, this);
                    dropDownVisible = true;
                }
            }
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
}
