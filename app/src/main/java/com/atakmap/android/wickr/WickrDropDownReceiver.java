
package com.atakmap.android.wickr;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.atakmap.android.contact.Contact;
import com.atakmap.android.contact.Contacts;
import com.atakmap.android.contact.IndividualContact;
import com.atakmap.android.cot.CotMapComponent;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.menu.PluginMenuParser;
import com.atakmap.android.dropdown.DropDown.OnStateListener;
import com.atakmap.android.dropdown.DropDownReceiver;

import com.atakmap.android.wickr.plugin.R;
import com.atakmap.android.wickr.plugin.WickrTool;
import com.atakmap.android.wickr.ui.ChoreographerFragment;
import com.atakmap.android.wickr.ui.ContactsListFragment;
import com.atakmap.android.wickr.ui.MembershipFragment;
import com.atakmap.android.wickr.ui.PairFragment;
import com.atakmap.android.wickr.ui.SendFileFragment;
import com.atakmap.android.wickr.ui.SendVoiceMessageFragment;
import com.atakmap.android.wickr.ui.util.ChatReceiver;
import com.atakmap.android.wickr.ui.util.PhoneConvoReceiver;
import com.atakmap.android.wickr.ui.util.SendFileReceiver;
import com.atakmap.android.wickr.utils.SettingsManager;
import com.atakmap.coremap.log.Log;
import com.wickr.android.api.WickrAPIObjects;

import org.greenrobot.eventbus.Subscribe;

import java.util.List;

public class WickrDropDownReceiver extends DropDownReceiver implements
        OnStateListener {

    public static final String TAG = WickrDropDownReceiver.class
            .getSimpleName();

    public static final String SHOW_PLUGIN = "com.atakmap.android.wickr.SHOW_PLUGIN";
    private final Context pluginContext;
    private ChoreographerFragment choreographerFragment;

    private PairFragment pairFragment;

    private SettingsManager settingsManager;
    private WickrContactConnectionManager wickrContactConnectionManager;
    private PhoneConvoReceiver phoneBroadcastReceiver;
    private SendFileReceiver sendFileReceiver;
    private ChatReceiver chatReceiver;

    /**************************** CONSTRUCTOR *****************************/

    public WickrDropDownReceiver(final MapView mapView,
                                 final Context context) {
        super(mapView);
        this.pluginContext = context;
        settingsManager = new SettingsManager(pluginContext);
        choreographerFragment = ChoreographerFragment.newInstance(pluginContext);
        pairFragment = PairFragment.newInstance(pluginContext);
        wickrContactConnectionManager = new WickrContactConnectionManager();
        phoneBroadcastReceiver = new PhoneConvoReceiver(pluginContext);
        WickrMapComponent.EVENTBUS.register(phoneBroadcastReceiver);
        sendFileReceiver = new SendFileReceiver(pluginContext);
        chatReceiver = new ChatReceiver(pluginContext);
        WickrMapComponent.EVENTBUS.register(chatReceiver);
        WickrMapComponent.EVENTBUS.register(this);
        List<Contact> contacts = Contacts.getInstance().getAllContacts();
        for (Contact c : contacts) {
            Log.d(TAG, "Contact being processed " + c.getName());
            processContact(c);

        }
        CotMapComponent.getInstance().getContactConnectorMgr()
                .addContactHandler(wickrContactConnectionManager);

        Contacts.getInstance().addListener(new Contacts.OnContactsChangedListener() {
            @Override
            public void onContactsSizeChange(Contacts contacts) {

            }

            @Override
            public void onContactChanged(String uuid) {
                Contact c = Contacts.getInstance().getContactByUuid(uuid);
                processContact(c);
            }
        });
        AtakBroadcast.getInstance().registerReceiver(chatReceiver, new AtakBroadcast.DocumentedIntentFilter("com.atakmap.android.wickr.launch.chat"));
        AtakBroadcast.getInstance().registerReceiver(phoneBroadcastReceiver, new AtakBroadcast.DocumentedIntentFilter("com.atakmap.android.wickr.launch.phone"));
        AtakBroadcast.getInstance().registerReceiver(sendFileReceiver, new AtakBroadcast.DocumentedIntentFilter("com.atakmap.android.wickr.launch.file"));
    }

    private void processContact(Contact c) {
        if (c instanceof IndividualContact) {
            IndividualContact individualContact = (IndividualContact) c;
            MapItem mi = ((IndividualContact) c).getMapItem();

            if (mi != null) {
                String wickrId = mi.getMetaString("wickrId", "");
                if (wickrId != null || !wickrId.isEmpty()) {
                    mi.setMetaString("menu", PluginMenuParser.getMenu(pluginContext, "a-f.xml"));
                }
            }
            individualContact.addConnector(new WickrConnector(pluginContext));
        }
    }

    @Subscribe
    public void onWickrAPIEvent(Object event) {
        if (event instanceof WickrAPIUnpairedEvent) {
            choreographerFragment.showFragment(pairFragment, null, false);
        } else if (event instanceof MessageFragmentEvent) {
            WickrAPIObjects.WickrConvo convo = ((MessageFragmentEvent) event).getConvo();
            choreographerFragment.showConvoFragment(convo);
        } else if (event instanceof CreateConvoFragmentEvent) {
            List<String> users = ((CreateConvoFragmentEvent) event).getCheckedUsers();
            choreographerFragment.showCreateConvoFragment(users);
        } else if (event instanceof MainFragmentEvent) {
            choreographerFragment.showWickrFragment(ChoreographerFragment.MAIN_FRAGMENT);
        } else if (event instanceof RequestCloseDropDownEvent) {
            closeDropDown();
        } else if (event instanceof SendFileInlineFragmentEvent) {
            SendFileInlineFragmentEvent sfife = (SendFileInlineFragmentEvent) event;
            SendFileFragment sff = new SendFileFragment(pluginContext, ((SendFileInlineFragmentEvent) event).getUserId(), sfife.getConvoId(), false);
            sff.setRecipientTitle(sfife.getFullname());
            choreographerFragment.showFragment(sff, null, true);
        } else if (event instanceof SendVoiceMessageFragmentEvent) {
            SendVoiceMessageFragmentEvent svmfe = (SendVoiceMessageFragmentEvent) event;
            SendVoiceMessageFragment svmf = new SendVoiceMessageFragment(pluginContext, ((SendVoiceMessageFragmentEvent) event).getUserId(), svmfe.getConvoId(), false);
            svmf.setRecipientTitle(svmfe.getFullname());
            choreographerFragment.showFragment(svmf, null, true);
        } else if (event instanceof PopFragmentEvent) {
            choreographerFragment.onBackButtonPressed();
        } else if (event instanceof RoomOrGroupDetailsEvent) {
            RoomOrGroupDetailsEvent rogd = (RoomOrGroupDetailsEvent) event;
            MembershipFragment mf = new MembershipFragment(pluginContext, rogd.getConvo());
            choreographerFragment.showFragment(mf, null, true);
        } else if (event instanceof SelectContactsEvent) {
            ContactsListFragment clf = ContactsListFragment.Companion.newInstance(pluginContext, false, ((SelectContactsEvent) event).getNewConvo(), ((SelectContactsEvent) event).getConvo());
            choreographerFragment.showFragment(clf, null, true);
        }

    }

    /**************************** PUBLIC METHODS *****************************/

    public void disposeImpl() {
        WickrMapComponent.EVENTBUS.unregister(this);
        WickrMapComponent.EVENTBUS.unregister(phoneBroadcastReceiver);
        WickrMapComponent.EVENTBUS.unregister(chatReceiver);
        CotMapComponent.getInstance().getContactConnectorMgr()
                .removeContactHandler(wickrContactConnectionManager);
        AtakBroadcast.getInstance().unregisterReceiver(chatReceiver);
        AtakBroadcast.getInstance().unregisterReceiver(phoneBroadcastReceiver);
        AtakBroadcast.getInstance().unregisterReceiver(sendFileReceiver);
    }

    /**************************** INHERITED METHODS *****************************/

    @Override
    public void onReceive(Context context, Intent intent) {
        if (settingsManager.isPaired()) {
           Requests r = new Requests(pluginContext);
           r.settings();
        }

        final String action = intent.getAction();
        if (action == null)
            return;

        if (action.equals(SHOW_PLUGIN)) {
            Log.d(TAG, "showing plugin drop down");
            Intent i = new Intent(WickrTool.BADGE_ACTION);
            i.putExtra("clearAll", true);
            AtakBroadcast.getInstance().sendBroadcast(i);
            showDropDown(choreographerFragment, HALF_WIDTH, FULL_HEIGHT, FULL_WIDTH,
                    HALF_HEIGHT, false, false);

        }
    }

    @Override
    public void onDropDownSelectionRemoved() {
    }

    @Override
    public void onDropDownVisible(boolean v) {
    }

    @Override
    public void onDropDownSizeChanged(double width, double height) {
    }

    @Override
    public void onDropDownClose() {
    }

    @Override
    protected boolean onBackButtonPressed() {
        return choreographerFragment.onBackButtonPressed();
    }
}
