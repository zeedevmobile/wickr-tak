
package com.atakmap.android.wickr.plugin;

;
import com.atak.plugins.impl.AbstractPlugin;
import com.atak.plugins.impl.PluginContextProvider;
import com.atakmap.android.wickr.WickrMapComponent;
import android.content.Context;

import gov.tak.api.plugin.IServiceController;
import gov.tak.api.util.Disposable;


/**
 *
 * AbstractPluginLifeCycle shipped with
 *     the plugin.
 */
public class WickrLifecycle extends AbstractPlugin implements Disposable {

    private final static String TAG = "PluginTemplateLifecycle";

    public WickrLifecycle(IServiceController serviceController) {
        super(serviceController, new WickrTool(serviceController.getService(PluginContextProvider.class).getPluginContext()), new WickrMapComponent());
        PluginNativeLoader.init(serviceController.getService(PluginContextProvider.class).getPluginContext());
    }

    @Override
    public void dispose() {
    }
}
