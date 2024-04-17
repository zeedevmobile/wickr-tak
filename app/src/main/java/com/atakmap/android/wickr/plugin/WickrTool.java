
package com.atakmap.android.wickr.plugin;

import com.atak.plugins.impl.AbstractPluginTool;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.navigation.NavButtonManager;
import com.atakmap.android.navigation.models.NavButtonModel;
import com.atakmap.android.wickr.WickrDropDownReceiver;
import com.atakmap.android.wickr.WickrMapComponent;
import gov.tak.api.util.Disposable;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class WickrTool extends AbstractPluginTool implements Disposable {

    public static final String BADGE_ACTION = "com.atakmap.android.wickr.plugin.iconcount";

    private Context context;

    public WickrTool(Context context) {
        super(context,
                context.getString(R.string.app_name_short),
                context.getString(R.string.app_name_short),
                context.getResources().getDrawable(R.drawable.ic_launcher_badge, context.getTheme()),
                WickrDropDownReceiver.SHOW_PLUGIN);
        this.context = context;
        AtakBroadcast.getInstance().registerReceiver(br, new AtakBroadcast.DocumentedIntentFilter(WickrTool.BADGE_ACTION));
    }

    private final BroadcastReceiver br = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            // currently broken
            boolean clearAll = intent.getBooleanExtra("clearAll", false);
            if (clearAll) {
                WickrMapComponent.clearAll();
            } else {

                int badgeCount = intent.getIntExtra("count", 0);
                String messageId = intent.getStringExtra("messageId");
                if (messageId != null) {
                    if (badgeCount > 0) {
                        WickrMapComponent.addCount(messageId, badgeCount);
                    } else {
                        WickrMapComponent.setCount(messageId, 0);
                    }
                    NavButtonModel model = NavButtonManager.getInstance().getModelByPlugin(WickrTool.this);
                    model.setBadgeCount(WickrMapComponent.getAllBadgeCounts());
                    NavButtonManager.getInstance().notifyModelChanged(model);
                }
            }
        }
    };
    @Override
    public void dispose() {
    }
}
