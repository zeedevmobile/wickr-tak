package com.atakmap.android.wickr.ui

import android.content.Context
import android.os.Build.VERSION
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.atakmap.android.wickr.MessageFragmentEvent
import com.atakmap.android.wickr.Requests
import com.atakmap.android.wickr.WickrAPIUnpairedEvent
import com.atakmap.android.wickr.WickrMapComponent
import com.atakmap.android.wickr.plugin.R
import com.atakmap.android.wickr.ui.adapters.ConvoClickListener
import com.atakmap.android.wickr.ui.adapters.WickrConvoAdapter
import com.atakmap.android.wickr.ui.util.ConvoReceiver
import com.atakmap.android.wickr.utils.SettingsManager
import com.atakmap.app.BuildConfig
import com.atakmap.coremap.log.Log
import com.wickr.android.api.WickrAPIObjects.WickrConvo
import com.wickr.android.api.WickrAPIObjects.WickrConvo.ConvoType
import kotlinx.android.synthetic.main.rooms_list.*
import java.lang.reflect.Field
import java.util.*
import kotlin.concurrent.timerTask

class RoomsListFragment private constructor(private val pluginContext: Context, private val convoType: ConvoType) : Fragment(),
    ConvoClickListener {
    private var timer = Timer()
    private val requests: Requests
    private val adapter: WickrConvoAdapter
    private val settingsManager: SettingsManager
    private val convoReceiver: ConvoReceiver
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dispose()
        val v = LayoutInflater.from(pluginContext).inflate(R.layout.rooms_list, container, false)
        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!settingsManager.isPaired()) {
            WickrMapComponent.EVENTBUS.post(WickrAPIUnpairedEvent())
        }
        messageList.apply {
            layoutManager = LinearLayoutManager(pluginContext, LinearLayoutManager.VERTICAL, false)
            adapter = this@RoomsListFragment.adapter
            setHasFixedSize(true)
        }
    }

    override fun onStart() {
        super.onStart()
        WickrMapComponent.EVENTBUS.register(convoReceiver)
        requests.refreshConvosFromWickr(convoType)
        timer = Timer()
        timer.schedule(timerTask {
            requests.subscribeToUpdates(true)
        }, 0,10000)

        requests.subscribeToUpdates(true)
    }

    override fun onStop() {
        super.onStop()
        WickrMapComponent.EVENTBUS.unregister(convoReceiver)
        requests.subscribeToUpdates(false)

        timer.cancel()
    }

    override fun onDestroyView() {
        WickrMapComponent.EVENTBUS.unregister(this)
        super.onDestroyView()
    }

    override fun onConvoClicked(convo: WickrConvo) {
        WickrMapComponent.EVENTBUS.post(MessageFragmentEvent(convo))
    }

    companion object {
        private var roomInstance: RoomsListFragment? = null
        private var dmInstance: RoomsListFragment? = null
        private val TAG = RoomsListFragment::class.java.simpleName
        @JvmStatic
        fun newInstance(pluginContext: Context): RoomsListFragment? {
            return newInstance(pluginContext, ConvoType.ROOM)
        }

        fun newInstance(pluginContext: Context, convoType: ConvoType): RoomsListFragment? {
            if (convoType == ConvoType.ROOM || convoType == ConvoType.GROUP) {
                if (roomInstance == null) {
                    roomInstance = RoomsListFragment(pluginContext, convoType)
                }
                return roomInstance
            } else if (convoType == ConvoType.DM) {
                if (dmInstance == null) {
                    dmInstance = RoomsListFragment(pluginContext, convoType)
                }
                return dmInstance
            }
            return null
        }

        fun dispose() {
            try {
                val f: Field?
                f = if (VERSION.SDK_INT < 29) {
                    LayoutInflater::class.java.getDeclaredField("sConstructorMap")
                } else {
                    if (VERSION.SDK_INT >= 29 && BuildConfig.DEBUG) {
                        Log.e(
                            "LayoutInflaterHelper",
                            "may need to revisit double reflection trick: PluginLayoutInflator"
                        )
                    }
                    val xgetDeclaredField =
                        Class::class.java.getDeclaredMethod("getDeclaredField", String::class.java)
                    xgetDeclaredField.invoke(LayoutInflater::class.java, "sConstructorMap") as Field
                }
                if (f != null) {
                    f.isAccessible = true
                    val sConstructorMap = f[null as Any?] as MutableMap<*, *>
                    sConstructorMap.clear()
                }
            } catch (var2: Exception) {
                Log.d(
                    "LayoutInflaterHelper",
                    "error, could not clear out the constructor map: $var2"
                )
            }
        }
    }

    init {
        requests = Requests(pluginContext)
        settingsManager = SettingsManager(pluginContext)
        adapter = WickrConvoAdapter(this, pluginContext)
        convoReceiver = ConvoReceiver(adapter, convoType)
    }
}