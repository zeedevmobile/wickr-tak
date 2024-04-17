
package com.atakmap.android.wickr;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.atakmap.android.cot.CotMapComponent;
import com.atakmap.android.cot.detail.CotDetailManager;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;

import com.atakmap.android.ipc.DocumentedExtra;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.dropdown.DropDownMapComponent;

import com.atakmap.android.maps.Marker;
import com.atakmap.android.wickr.ui.UserAvatarCache;
import com.atakmap.android.wickr.ui.WickrPluginPreferenceFragment;
import com.atakmap.android.wickr.utils.SettingsManager;
import com.atakmap.app.preferences.ToolsPreferenceFragment;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.log.Log;
import com.atakmap.android.wickr.plugin.R;
import com.wickr.android.api.WickrAPI;
import com.wickr.android.api.WickrAPIObjects;
import com.wickr.android.api.WickrAPIRequests;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.HashMap;
import java.util.Map;


public class WickrMapComponent extends DropDownMapComponent {

    private static final String TAG = "WickrMapComponent";
    public static final String TARGET_APP = "com.wickr.pro.beta";

    public static EventBus EVENTBUS = EventBus.getDefault();
    public UserAvatarCache userAvatarCache;

    private Context pluginContext;

    private WickrDropDownReceiver ddr;
    private MessageDropDownReceiver mddr;
    private SendFileDropDownReceiver sfddr;
    private WickrAPIReceiver apiReceiver;
    private WickrDetailHandler wdh;
    private WickrDetail wickrDetail;

    private SettingsManager settingsManager;

    private WickrAPIObjects.WickrUser selfUser;

    private static int badgeCount = 0;

    private static Map<String, Integer> unreadCount = new HashMap<String, Integer>();

    public static void clearAll() {
        for(String messageId : unreadCount.keySet()) {
            unreadCount.put(messageId, 0);
        }
    }

    public static void addCount(String messageId, int count) {
        if (unreadCount.containsKey(messageId)) {
            unreadCount.put(messageId, unreadCount.get(messageId) + count);
        } else {
            unreadCount.put(messageId, count);
        }
        badgeCount += count;
    }

    public static void setCount(String messageId, int count) {
        unreadCount.put(messageId, count);
    }

    public static int getCounter(String messageId) {
        if (unreadCount.containsKey(messageId)) {
            return unreadCount.get(messageId);
        } else {
            return 0;
        }
    }

    public static int getAllBadgeCounts() {
        int total = 0;
        for(String id : unreadCount.keySet()) {
            int count = unreadCount.get(id);
            total += count;
        }
        return total;
    }



    public void onCreate(final Context context, Intent intent,
            final MapView view) {

        context.setTheme(R.style.ATAKPluginTheme);
        super.onCreate(context, intent, view);
        pluginContext = context;
        settingsManager = new SettingsManager(pluginContext);

        wdh = new WickrDetailHandler();
        CotDetailManager.getInstance().registerHandler(WickrDetailHandler.DETAIL_NAME, wdh);
        String pkg = settingsManager.getPref("WickrPkg");
        if (pkg.isEmpty()) {
            pkg = TARGET_APP;
        }
        WickrAPI.INSTANCE.setTargetApp(pkg);
        apiReceiver = new WickrAPIReceiver(context);
        userAvatarCache = UserAvatarCache.Companion.getInstance(pluginContext);
        DocumentedIntentFilter wickrFilter = new DocumentedIntentFilter();
        wickrFilter.addAction("com.wickr.android.api.response");
        AtakBroadcast.getInstance().registerSystemReceiver(apiReceiver, wickrFilter);
        EVENTBUS.register(this);
        ddr = new WickrDropDownReceiver(
                view, context);

        mddr = new MessageDropDownReceiver(view, context);
        sfddr = new SendFileDropDownReceiver(view, context);

        Log.d(TAG, "registering the plugin filter");
        DocumentedIntentFilter ddFilter = new DocumentedIntentFilter();
        ddFilter.addAction(WickrDropDownReceiver.SHOW_PLUGIN);
        registerDropDownReceiver(ddr, ddFilter);

        DocumentedIntentFilter mddFilter = new DocumentedIntentFilter();
        mddFilter.addAction(MessageDropDownReceiver.SHOW_MESSAGE);
        registerDropDownReceiver(mddr, mddFilter);

        DocumentedIntentFilter sfddrFilter = new DocumentedIntentFilter();
        sfddrFilter.addAction(SendFileDropDownReceiver.SENDFILE_MESSAGE);
        registerDropDownReceiver(sfddr, sfddrFilter);

        // register a listener for when a the radial menu asks for a special
        // drop down.  SpecialDetail is really a skeleton of a class that
        // shows a very basic drop down.
        DocumentedIntentFilter filter = new DocumentedIntentFilter();
        filter.addAction(WickrDetail.ACTION,
                "This intent launches a Wickr DM with another user",
                new DocumentedExtra[] {
                        new DocumentedExtra("id",
                                "the id for the wickr user. used to initiate a DM.")
                });

        if (settingsManager.isPaired()) {
            Requests requests = new Requests(pluginContext);
            requests.settings();
        }
        wickrDetail = new WickrDetail(view, pluginContext);

        ToolsPreferenceFragment
                .register(
                        new ToolsPreferenceFragment.ToolPreference(
                                pluginContext.getString(R.string.preferences_title),
                                pluginContext.getString(R.string.preferences_summary),
                                "wickrPreference",
                                context.getResources().getDrawable(
                                        R.drawable.ic_launcher, null),
                                new WickrPluginPreferenceFragment(pluginContext)));
    }


    @Override
    protected void onDestroyImpl(Context context, MapView view) {
        super.onDestroyImpl(context, view);
        EVENTBUS.unregister(this);
        CotDetailManager.getInstance().unregisterHandler(wdh);
        AtakBroadcast.getInstance().unregisterSystemReceiver(apiReceiver);
    }

    @Subscribe
    public void onWickrApi(Object event) {
        if (event instanceof WickrUserSettingsEvent) {
            WickrAPIObjects.WickrUserSettings settingsEvent = ((WickrUserSettingsEvent) event).getUserSettings();
            selfUser = settingsEvent.getSelfUser();
            wdh.setContact(selfUser);

            // send out some customized information as part of the SA or PPLI message.
            CotDetail cd = WickrDetailHandler.createDetail(selfUser.getId(), selfUser.getUsername(), selfUser.getFullName());
            CotMapComponent.getInstance().addAdditionalDetail(cd.getElementName(),
                    cd);
        }
    }

}
