package com.atakmap.android.wickr.wear.data

import android.content.Context
import android.util.Log
import com.samsung.android.service.health.tracking.ConnectionListener
import com.samsung.android.service.health.tracking.HealthTrackerException
import com.samsung.android.service.health.tracking.HealthTrackingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.callbackFlow

private const val TAG = "HealthTrackingServiceConnection"

@ExperimentalCoroutinesApi
class HealthTrackingServiceConnection(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    private var connected: Boolean = false
    private var healthTrackingService: HealthTrackingService? = null

    val connectionFlow = callbackFlow {
        val connectionListener = object : ConnectionListener {
            override fun onConnectionSuccess() {
                connected = true
                Log.i(TAG, "onConnectionSuccess()")
                coroutineScope.runCatching {
                    trySendBlocking(
                        ConnectionMessage.ConnectionSuccessMessage
                    )
                }
            }

            override fun onConnectionFailed(connectionException: HealthTrackerException?) {
                Log.i(TAG, "onConnectionFailed()")
                connected = false
                coroutineScope.runCatching {
                    trySendBlocking(ConnectionMessage.ConnectionFailedMessage(connectionException))
                }
            }

            override fun onConnectionEnded() {
                Log.i(TAG, "onConnectionEnded()")
                connected = false
                coroutineScope.runCatching {
                    trySendBlocking(ConnectionMessage.ConnectionEndedMessage)
                }
                Log.i(TAG, "before close()")
                close()
            }
        }
        Log.i(TAG, "healthTrackingService = HealthTrackingService(connectionListener, context)")
        healthTrackingService = HealthTrackingService(connectionListener, context)
        healthTrackingService!!.connectService()

        awaitClose {
            Log.i(TAG, "awaitClose: disconnect()")
            disconnect()
        }
    }

    private fun disconnect() {
        Log.i(TAG, "disconnect()")
        checkNotNull(healthTrackingService).disconnectService()
        connected = false
    }

    fun getHealthTrackingService(): HealthTrackingService? {
        return healthTrackingService
    }
}

sealed class ConnectionMessage {
    object ConnectionSuccessMessage : ConnectionMessage()
    class ConnectionFailedMessage(val exception: HealthTrackerException?) : ConnectionMessage()
    object ConnectionEndedMessage : ConnectionMessage()
}
