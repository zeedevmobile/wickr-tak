package com.atakmap.android.wickr.plugin.domain

import android.util.Log
import com.atakmap.android.wickr.common.TrackedHealthData
import com.atakmap.android.wickr.plugin.data.MessageRepo
import com.atakmap.android.wickr.plugin.data.TrackingRepo
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val TAG = "SendMessageUseCase"

private const val MESSAGE_PATH = "/msg"

class SendMessageUseCase(
    private val messageRepo: MessageRepo,
    private val trackingRepo: TrackingRepo,
    private val getCapableNodes: GetCapableNodes
) {

    suspend operator fun invoke(): Boolean {

        val nodes = getCapableNodes()

        return if (nodes.isNotEmpty()) {

            val node = nodes.first()
            val message = encodeMessage(trackingRepo.getValidHrData())
            messageRepo.sendMessage(message, node, MESSAGE_PATH)

            true

        } else {
            Log.i(TAG, "Ain't no nodes around")
            false
        }
    }

    fun encodeMessage(trackedData: ArrayList<TrackedHealthData>): String {

        return Json.encodeToString(trackedData)
    }
}
