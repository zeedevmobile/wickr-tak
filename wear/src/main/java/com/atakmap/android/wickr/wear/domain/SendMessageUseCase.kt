package com.atakmap.android.wickr.wear.domain

import android.util.Log
import com.atakmap.android.wickr.common.TrackedData
import com.atakmap.android.wickr.wear.data.MessageRepo
import com.atakmap.android.wickr.wear.data.TrackingRepo
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

private const val TAG = "SendMessageUseCase"

private const val MESSAGE_PATH = "/msg"

class SendMessageUseCase : KoinComponent {

    private val messageRepo: MessageRepo = get()
    private val trackingRepo: TrackingRepo = get()
    private val getCapableNodes: GetCapableNodes = get()

    suspend operator fun invoke(): Boolean {

        val nodes = getCapableNodes()

        return if (nodes.isNotEmpty()) {

            val node = nodes.first()
            val message =
                encodeMessage(trackingRepo.getValidHrData())
            messageRepo.sendMessage(message, node, MESSAGE_PATH)

            true

        } else {
            Log.i(TAG, "Ain't no nodes around")
            false
        }
    }

    fun encodeMessage(trackedData: ArrayList<TrackedData>): String {

        return Json.encodeToString(trackedData)
    }
}
