package com.atakmap.android.wickr

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.LayoutInflater
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.atakmap.android.contact.ContactLocationView
import com.atakmap.android.cot.CotMapComponent
import com.atakmap.android.cot.detail.CotDetailHandler
import com.atakmap.android.cot.detail.CotDetailManager
import com.atakmap.android.cotdetails.ExtendedInfoView
import com.atakmap.android.dropdown.DropDownMapComponent
import com.atakmap.android.ipc.AtakBroadcast
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter
import com.atakmap.android.ipc.DocumentedExtra
import com.atakmap.android.maps.MapItem
import com.atakmap.android.maps.MapView
import com.atakmap.android.maps.PointMapItem
import com.atakmap.android.wickr.plugin.R
import com.atakmap.android.wickr.service.HealthWearListenerService
import com.atakmap.android.wickr.service.HealthWearListenerService.Companion.ACTION_HEALTH_DATA_HR_UPDATE
import com.atakmap.android.wickr.service.HealthWearListenerService.Companion.ACTION_HEALTH_DATA_SPO2_UPDATE
import com.atakmap.android.wickr.service.HealthWearListenerService.Companion.EXTRA_HEALTH_DATA
import com.atakmap.android.wickr.service.HealthWearListenerService.Companion.EXTRA_IS_ABNORMAL
import com.atakmap.android.wickr.ui.UserAvatarCache
import com.atakmap.android.wickr.ui.UserAvatarCache.Companion.getInstance
import com.atakmap.android.wickr.ui.WickrPluginPreferenceFragment
import com.atakmap.android.wickr.utils.SettingsManager
import com.atakmap.app.preferences.ToolsPreferenceFragment
import com.atakmap.app.preferences.ToolsPreferenceFragment.ToolPreference
import com.atakmap.comms.CommsMapComponent
import com.atakmap.comms.ReportingRate
import com.atakmap.coremap.cot.event.CotDetail
import com.atakmap.coremap.cot.event.CotEvent
import com.atakmap.coremap.log.Log
import com.wickr.android.api.WickrAPI.setTargetApp
import com.wickr.android.api.WickrAPIObjects.WickrUser
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class WickrMapComponent : DropDownMapComponent() {

    var userAvatarCache: UserAvatarCache? = null
    private var pluginContext: Context? = null
    private var ddr: WickrDropDownReceiver? = null
    private var mddr: MessageDropDownReceiver? = null
    private var sfddr: SendFileDropDownReceiver? = null
    private var apiReceiver: WickrAPIReceiver? = null
    private var wdh: WickrDetailHandler? = null
    private var wickrDetail: WickrDetail? = null
    private var settingsManager: SettingsManager? = null
    private var selfUser: WickrUser? = null

    private lateinit var mapView: MapView
    private var healthDetail: CotDetailHandler? = null
    private var extendedSelfInfo: ContactLocationView.ExtendedSelfInfoFactory? = null

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(
        context: Context, intent: Intent,
        view: MapView
    ) {
        context.setTheme(R.style.ATAKPluginTheme)
        super.onCreate(context, intent, view)

        mapView = view
        pluginContext = context
        settingsManager = SettingsManager(pluginContext!!)
        wdh = WickrDetailHandler()
        CotDetailManager.getInstance().registerHandler(WickrDetailHandler.DETAIL_NAME, wdh)
        var pkg = settingsManager!!.getPref("WickrPkg")
        if (pkg!!.isEmpty()) {
            pkg = TARGET_APP
        }
        setTargetApp(pkg)
        apiReceiver = WickrAPIReceiver(context)
        userAvatarCache = getInstance(pluginContext!!)
        val wickrFilter = DocumentedIntentFilter()
        wickrFilter.addAction("com.wickr.android.api.response")
        AtakBroadcast.getInstance().registerSystemReceiver(apiReceiver, wickrFilter)
        EVENTBUS.register(this)
        ddr = WickrDropDownReceiver(
            view, context
        )
        mddr = MessageDropDownReceiver(view, context)
        sfddr = SendFileDropDownReceiver(view, context)
        Log.d(TAG, "registering the plugin filter")
        val ddFilter = DocumentedIntentFilter()
        ddFilter.addAction(WickrDropDownReceiver.SHOW_PLUGIN)
        registerDropDownReceiver(ddr, ddFilter)
        val mddFilter = DocumentedIntentFilter()
        mddFilter.addAction(MessageDropDownReceiver.SHOW_MESSAGE)
        registerDropDownReceiver(mddr, mddFilter)
        val sfddrFilter = DocumentedIntentFilter()
        sfddrFilter.addAction(SendFileDropDownReceiver.SENDFILE_MESSAGE)
        registerDropDownReceiver(sfddr, sfddrFilter)

        // register a listener for when a the radial menu asks for a special
        // drop down.  SpecialDetail is really a skeleton of a class that
        // shows a very basic drop down.
        val filter = DocumentedIntentFilter()
        filter.addAction(
            WickrDetail.ACTION,
            "This intent launches a Wickr DM with another user", arrayOf(
                DocumentedExtra(
                    "id",
                    "the id for the wickr user. used to initiate a DM."
                )
            )
        )
        if (settingsManager!!.isPaired()) {
            val requests = Requests(pluginContext!!)
            requests.settings()
        }
        wickrDetail = WickrDetail(view, pluginContext)
        ToolsPreferenceFragment
            .register(
                ToolPreference(
                    pluginContext!!.getString(R.string.preferences_title),
                    pluginContext!!.getString(R.string.preferences_summary),
                    "wickrPreference",
                    context.resources.getDrawable(
                        R.drawable.ic_launcher, null
                    ),
                    WickrPluginPreferenceFragment(pluginContext)
                )
            )

        registerCoTListener()
        registerWithContactLocationView()

        context.registerReceiver(
            broadcastReceiver, IntentFilter().apply {
                addAction(HealthWearListenerService.ACTION_HEALTH_DATA_SPO2_UPDATE)
                addAction(HealthWearListenerService.ACTION_HEALTH_DATA_HR_UPDATE)
            }
        )
    }

    override fun onDestroyImpl(context: Context, view: MapView) {
        super.onDestroyImpl(context, view)
        EVENTBUS.unregister(this)
        CotDetailManager.getInstance().unregisterHandler(wdh)
        AtakBroadcast.getInstance().unregisterSystemReceiver(apiReceiver)
    }

    @Subscribe
    fun onWickrApi(event: Any?) {
        if (event is WickrUserSettingsEvent) {
            val settingsEvent = event.userSettings
            selfUser = settingsEvent.selfUser
            wdh!!.contact = selfUser

            // send out some customized information as part of the SA or PPLI message.
            val cd = WickrDetailHandler.createDetail(
                selfUser?.id,
                selfUser?.username,
                selfUser?.fullName
            )
            CotMapComponent.getInstance().addAdditionalDetail(
                cd.elementName,
                cd
            )
        }
    }

    companion object {
        private const val COT_WEAR_HEALTH_DETAIL = "COT_WEAR_HEALTH_DETAIL"
        private const val TAG_COT_DETAIL_HR = "TAG_COT_DETAIL_HR"
        private const val TAG_COT_DETAIL_SPO2 = "TAG_COT_DETAIL_SPO2"

        private const val TAG = "WickrMapComponent"
        const val TARGET_APP = "com.wickr.pro.beta"

        @JvmField
        var EVENTBUS = EventBus.getDefault()
        private var badgeCount = 0
        private val unreadCount: MutableMap<String, Int> = HashMap()

        @JvmStatic
        fun clearAll() {
            for (messageId in unreadCount.keys) {
                unreadCount[messageId] = 0
            }
        }

        @JvmStatic
        fun addCount(messageId: String, count: Int) {
            if (unreadCount.containsKey(messageId)) {
                unreadCount[messageId] = unreadCount[messageId]!! + count
            } else {
                unreadCount[messageId] = count
            }
            badgeCount += count
        }

        @JvmStatic
        fun setCount(messageId: String, count: Int) {
            unreadCount[messageId] = count
        }

        fun getCounter(messageId: String): Int {
            return if (unreadCount.containsKey(messageId)) {
                unreadCount[messageId]!!
            } else {
                0
            }
        }

        @JvmStatic
        val allBadgeCounts: Int
            get() {
                var total = 0
                for (id in unreadCount.keys) {
                    val count = unreadCount[id]!!
                    total += count
                }
                return total
            }
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(contxt: Context?, intent: Intent?) {
            android.util.Log.d("XXXXX", "MapComponent message received")
            intent?.apply {
                val cotDetail = CotDetail(COT_WEAR_HEALTH_DETAIL)
                val healthData = getIntExtra(EXTRA_HEALTH_DATA, 0)
                val isAbnormal = getBooleanExtra(EXTRA_IS_ABNORMAL, false)
                when (action) {
                    ACTION_HEALTH_DATA_HR_UPDATE -> {
                        cotDetail.setAttribute(TAG_COT_DETAIL_HR, healthData.toString())
                    }

                    ACTION_HEALTH_DATA_SPO2_UPDATE -> {
                        cotDetail.setAttribute(TAG_COT_DETAIL_SPO2, healthData.toString())
                    }
                }

                CotMapComponent.getInstance().addAdditionalDetail(cotDetail.elementName, cotDetail)

                // manually update our own marker
                healthDetail?.toItemMetadata(mapView.selfMarker, CotEvent(), cotDetail)
            }

            // manually trigger an update to be sent out
            AtakBroadcast.getInstance().sendBroadcast(
                Intent(ReportingRate.REPORT_LOCATION).putExtra(
                    "reason", "detail update for heart rate"
                )
            )
        }
    }

    private fun registerCoTListener() {
        // Write the deserialization mechanism
        CotDetailManager.getInstance()
            .registerHandler(object : CotDetailHandler(COT_WEAR_HEALTH_DETAIL) {
                override fun toItemMetadata(
                    item: MapItem,
                    event: CotEvent,
                    detail: CotDetail
                ): CommsMapComponent.ImportResult {
                    try {
                        val heartRateString = detail.getAttribute(TAG_COT_DETAIL_HR)
                        val spO2String = detail.getAttribute(TAG_COT_DETAIL_SPO2)

                     //   if (!heartRateString.isNullOrEmpty()) {
                         //   return CommsMapComponent.ImportResult.FAILURE
                      //  }

                        if (!heartRateString.isNullOrEmpty()) {
                            item.setMetaString(TAG_COT_DETAIL_HR, heartRateString)
                        }
                        if (!spO2String.isNullOrEmpty()) {
                            item.setMetaString(TAG_COT_DETAIL_SPO2, spO2String)
                        }

                    } catch (e: Exception) {
                        Log.e("WearSampleFragment", e.toString())
                    }
                    return CommsMapComponent.ImportResult.SUCCESS
                }

                override fun toCotDetail(
                    item: MapItem, event: CotEvent, root: CotDetail
                ): Boolean {
                    // We do not need to worry about serializing the data here.   This is done directly
                    // with the timer.
                    //Log.d(TAG, "converting to cot detail from: " + item.getUID());
                    return true
                }
            }.also { healthDetail = it })
    }

    private fun registerWithContactLocationView() {
        ContactLocationView.register(
            ContactLocationView.ExtendedSelfInfoFactory {
                object : ExtendedInfoView(pluginContext) {
                    override fun setMarker(pointMapItem: PointMapItem) {

                        android.util.Log.d("XXXXX", "MapComponent updating marker")


                        val test = findViewWithTag<ConstraintLayout>("tag")


                        android.util.Log.d("XXXXX", "test = $test")

                        val layout = LayoutInflater.from(context)
                            .inflate(R.layout.layout_marker_wear_data, this).also {
                                tag = "tag"
                            }

                        val textViewHr =
                            layout.findViewById<AppCompatTextView>(R.id.textview_marker_wear_data_hr)

                        val textViewSpO2 =
                            layout.findViewById<AppCompatTextView>(R.id.textview_marker_wear_data_spo2)

                        var heartRate = pointMapItem.getMetaString(TAG_COT_DETAIL_HR, null)
                        var spO2 = pointMapItem.getMetaString(TAG_COT_DETAIL_SPO2, null)
                        android.util.Log.d("XXXXX", "MapComponent heartRate : $heartRate")
                        android.util.Log.d("XXXXX", "MapComponent spO2 : $spO2")


                        if (!spO2.isNullOrEmpty()) textViewSpO2.text = spO2

                        if (!heartRate.isNullOrEmpty()) textViewHr.text = heartRate

                        addView(layout)

                        mapView.invalidate()
                    }
                }
            }.also {
                extendedSelfInfo = it
            })
    }
}
