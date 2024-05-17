package com.atakmap.android.wickr.ui.util

import android.app.Activity
import android.util.Log
import com.atakmap.android.maps.MapView
import com.atakmap.android.wickr.WickrAPIPairedEvent
import com.atakmap.android.wickr.WickrAPIUnpairedEvent
import com.atakmap.android.wickr.WickrConvoListEvent
import com.atakmap.android.wickr.WickrConvoUpdateEvent
import com.atakmap.android.wickr.WickrUserAvatarUpdateEvent
import com.atakmap.android.wickr.ui.adapters.WickrConvoAdapter
import com.wickr.android.api.WickrAPIObjects
import org.greenrobot.eventbus.Subscribe

class ConvoReceiver(private val adapter: WickrConvoAdapter, private val convoType: WickrAPIObjects.WickrConvo.ConvoType) {


    @Subscribe
    internal fun onAppEvent(event: Any) {
        val mapActivity = MapView.getMapView().context as? Activity

        when (event) {
            is WickrConvoListEvent -> mapActivity?.runOnUiThread {
                Log.d("XXXXX", "WickrConvoListEvent")
                if (event.identifier == convoType.name) adapter.setItems(event.convos)
            }
            is WickrConvoUpdateEvent -> mapActivity?.runOnUiThread {
                if (isSubscribed(event.convo.type)) {
                    adapter.updateItems(listOf(event.convo))
                    //mapActivity?.refreshTabs(event.convo.type)
                }
            }
            is WickrAPIPairedEvent -> {
                mapActivity?.invalidateOptionsMenu()
                mapActivity?.runOnUiThread { adapter.setItems(emptyList()) }
            }
            is WickrAPIUnpairedEvent -> mapActivity?.runOnUiThread {
                adapter.setItems(emptyList())
            }
            is WickrUserAvatarUpdateEvent -> {
                adapter.notifyItemRangeChanged(0, adapter.itemCount, event)
            }
        }

    }

    private fun isSubscribed(type: WickrAPIObjects.WickrConvo.ConvoType): Boolean {
        if (convoType == null) return true
        if (convoType == WickrAPIObjects.WickrConvo.ConvoType.DM
            && type == WickrAPIObjects.WickrConvo.ConvoType.DM) return true
        if (convoType == WickrAPIObjects.WickrConvo.ConvoType.ROOM
            && (type == WickrAPIObjects.WickrConvo.ConvoType.ROOM || type == WickrAPIObjects.WickrConvo.ConvoType.GROUP)) return true
        return false
    }


}