package com.atakmap.android.wickr.plugin.multisensortracking

import android.os.Handler
import com.samsung.android.service.health.tracking.HealthTracker
import com.samsung.android.service.health.tracking.HealthTracker.TrackerEventListener

open class BaseListener {

    private var handler: Handler? = null
    private var healthTracker: HealthTracker? = null
    private var isHandlerRunning = false
    private var trackerEventListener: TrackerEventListener? = null

    fun setHealthTracker(tracker: HealthTracker?) {
        healthTracker = tracker
    }

    fun setHandler(handler: Handler?) {
        this.handler = handler
    }

    fun setHandlerRunning(handlerRunning: Boolean) {
        isHandlerRunning = handlerRunning
    }

    fun setTrackerEventListener(tracker: TrackerEventListener?) {
        trackerEventListener = tracker
    }

    fun startTracker() {
        if (!isHandlerRunning) {
            handler?.post {
                healthTracker?.setEventListener(trackerEventListener)
                setHandlerRunning(true)
            }
        }
    }

    fun stopTracker() {
        if (isHandlerRunning) {
            healthTracker?.unsetEventListener()
            setHandlerRunning(false)
            handler?.removeCallbacksAndMessages(null)
        }
    }
}
