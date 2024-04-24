package com.atakmap.android.wickr.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.Fragment
import com.atakmap.android.wickr.plugin.R

class WearSampleFragment private constructor(private val pluginContext: Context) : Fragment() {

    companion object {

        private var instance: WearSampleFragment? = null

        fun newInstance(pluginContext: Context): WearSampleFragment? {
            if (instance == null) {
                WearSampleFragment.instance = WearSampleFragment(pluginContext)
            }
            return instance
        }
    }

    private lateinit var texter: AppCompatTextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return LayoutInflater.from(pluginContext)
            .inflate(R.layout.fragment_wear_sample, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        texter = view.findViewById(R.id.textview_wear_sample)

        requireContext().registerReceiver(broadCastReceiver, IntentFilter("filter"))
    }

    private val broadCastReceiver = object : BroadcastReceiver() {
        override fun onReceive(contxt: Context?, intent: Intent?) {
            if (intent?.action == "filter") {
                intent.getStringExtra("extra").let {
                    texter.text = it
                }
            }
        }
    }

}
