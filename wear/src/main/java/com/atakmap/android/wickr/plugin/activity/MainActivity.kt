package com.atakmap.android.wickr.plugin.activity

import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import com.atakmap.android.wickr.plugin.R


class MainActivity : ComponentActivity() {

    private val viewModel: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val start = findViewById<TextView>(R.id.textView_start_tracking)
        start.setOnClickListener {
            viewModel.startTracking()
        }

        val stop = findViewById<TextView>(R.id.textView_stop_tracking)
        stop.setOnClickListener {
            viewModel.stopTracking()
        }

        val send = findViewById<TextView>(R.id.textView_send_message)
        send.setOnClickListener {
            viewModel.sendMessage()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!viewModel.connectionState.value.connected) {
            viewModel.setUpTracking()
        }
    }
}
