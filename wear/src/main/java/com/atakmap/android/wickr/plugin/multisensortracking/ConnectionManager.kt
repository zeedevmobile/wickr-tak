package com.atakmap.android.wickr.plugin.multisensortracking

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.samsung.android.service.health.tracking.ConnectionListener
import com.samsung.android.service.health.tracking.HealthTrackerException
import com.samsung.android.service.health.tracking.HealthTrackingService
import com.samsung.android.service.health.tracking.data.HealthTrackerType

class ConnectionManager internal constructor(private val connectionObserver: ConnectionObserver) {

    interface ConnectionObserver {

        fun connected()

        fun disconnected()

        fun onError(healthTrackerException: HealthTrackerException)
    }

    var isConnected = false

    private var healthTrackingService: HealthTrackingService? = null
    private var availableTrackers = mutableListOf<HealthTrackerType>()

    private val connectionListener: ConnectionListener = object : ConnectionListener {
        override fun onConnectionSuccess() {
            isConnected = true
            connectionObserver.connected()
        }

        override fun onConnectionEnded() {
            isConnected = false
            connectionObserver.disconnected()
        }

        override fun onConnectionFailed(error: HealthTrackerException) {
            isConnected = false
            connectionObserver.onError(error)
        }
    }

    fun connect(context: Context?) {
        healthTrackingService = HealthTrackingService(connectionListener, context)
        healthTrackingService?.connectService()
        healthTrackingService?.trackingCapability?.supportHealthTrackerTypes?.let {
            availableTrackers = it
        }
    }

    fun disconnect() {
        healthTrackingService?.disconnectService()
    }

    fun setSpO2Listener(spO2Listener: SpO2Listener) {
        healthTrackingService?.getHealthTracker(HealthTrackerType.SPO2)?.let {
            spO2Listener.setHealthTracker(it)
            spO2Listener.setHandler(Handler(Looper.getMainLooper()))
        }
    }

    fun setHeartRateListener(heartRateListener: HeartRateListener) {
        healthTrackingService?.getHealthTracker(HealthTrackerType.HEART_RATE)?.let {
            heartRateListener.setHealthTracker(it)
            heartRateListener.setHandler(Handler(Looper.getMainLooper()))
        }
    }

    fun isSpO2Available(): Boolean {
        return availableTrackers.contains(HealthTrackerType.SPO2)
    }

    fun isHeartRateAvailable(): Boolean {
        return availableTrackers.contains(HealthTrackerType.HEART_RATE)
    }
}
