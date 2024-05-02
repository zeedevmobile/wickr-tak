package com.atakmap.android.wickr.ui

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.Fragment
import com.atakmap.android.wickr.common.TrackedHealthData
import com.atakmap.android.wickr.plugin.R
import com.atakmap.android.wickr.service.HealthWearListenerService.Companion.ACTION_HEALTH_DATA_MESSAGE
import com.atakmap.android.wickr.service.HealthWearListenerService.Companion.EXTRA_HEALTH_DATA
import kotlinx.serialization.json.Json

class WearSampleFragment private constructor(private val pluginContext: Context) : Fragment() {

    companion object {

        // TODO need to fix this, this is only following the previous implementaiotn of fragments
        //  so it will fit into the old viewpager
        private var instance: WearSampleFragment? = null

        fun newInstance(pluginContext: Context): WearSampleFragment? {
            if (instance == null) {
                WearSampleFragment.instance = WearSampleFragment(pluginContext)
            }
            return instance
        }
    }

    private lateinit var textViewSpO2: AppCompatTextView
    private lateinit var textViewHr: AppCompatTextView
    private lateinit var imageViewHrAlert: AppCompatImageView
    private lateinit var imageViewSpo2Alert: AppCompatImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return LayoutInflater.from(pluginContext)
            .inflate(R.layout.fragment_wear_sample, container, false)
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        textViewSpO2 = view.findViewById(R.id.textview_wear_spo2)
        textViewHr = view.findViewById(R.id.textview_wear_hr)

        imageViewHrAlert = view.findViewById(R.id.imageview_hr_alert)
        imageViewHrAlert.setOnClickListener {
            imageViewHrAlert.visibility = View.GONE
        }
        imageViewSpo2Alert = view.findViewById(R.id.imageview_spo2_alert)
        imageViewSpo2Alert.setOnClickListener {
            imageViewSpo2Alert.visibility = View.GONE
        }

        requireContext().registerReceiver(
            broadcastReceiver, IntentFilter(ACTION_HEALTH_DATA_MESSAGE)
        )

    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(contxt: Context?, intent: Intent?) {
            if (intent?.action == ACTION_HEALTH_DATA_MESSAGE) {
                intent.getStringExtra(EXTRA_HEALTH_DATA)?.let { data ->
                    decodeString(data)?.let {

                        if (it.hr != null) {
                            textViewHr.text = it.hr.toString()
                        }
                        if (it.spO2 != null) {
                            textViewSpO2.text = it.spO2.toString()
                        }
                        if (it.hrAlert != null) {
                            imageViewHrAlert.visibility =
                                if (imageViewHrAlert.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                        }
                        if (it.spO2Alert != null) {
                            imageViewSpo2Alert.visibility =
                                if (imageViewSpo2Alert.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                        }
                    }
                }
            }
        }
    }

    private fun decodeString(data: String): TrackedHealthData? {
        return try {
            Json.decodeFromString<TrackedHealthData>(data)
        } catch (exception: Error) {
            null
        }
    }
}
