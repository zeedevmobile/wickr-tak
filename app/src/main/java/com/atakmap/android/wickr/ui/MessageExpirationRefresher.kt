package com.atakmap.android.wickr.ui

import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import com.atakmap.android.wickr.ui.adapters.WickrMessageAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MessageExpirationRefresher(
    private val view: RecyclerView,
    private val adapter: WickrMessageAdapter
) {

    class ExpirationRefresh()

    companion object {
        private const val LOOP_TIMEOUT = 800
        private val TAG = ExpirationRefresh::javaClass.name

    }
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var running = false

    fun start() {
        coroutineScope.launch {
            try {
                running = true
                var lastTime = 0L
                while (running) {
                    if (System.currentTimeMillis() - lastTime > LOOP_TIMEOUT) {
                        if (!view.isComputingLayout) {
                            adapter.notifyItemRangeChanged(0, adapter.itemCount, ExpirationRefresh())
                            lastTime = System.currentTimeMillis()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, e.toString(), e)
            }
        }
    }

    fun stop() {
        try {
            running = false
            coroutineScope.cancel()
        } catch (e: Exception) {
            // no job running
        }
    }

}