package com.atakmap.android.wickr.plugin.multisensortracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atakmap.android.wickr.common.TrackedHrData
import com.atakmap.android.wickr.plugin.data.MessageRepo
import com.atakmap.android.wickr.plugin.domain.GetCapableNodes
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class MainActivityViewModel : ViewModel(), KoinComponent {


    companion object {
        private const val MESSAGE_PATH = "/msg"
    }

    private val messageRepo: MessageRepo = get()
    private val getCapableNodes: GetCapableNodes = get()
   // private val coroutineScope : CoroutineScope = get()

    private var validHrData = ArrayList<TrackedHrData>()

    // TODO need to move the listeners into the VM
    fun onHrDataReceived(hrData: HeartRateData) {
        val trackedData = TrackedHrData()
        trackedData.hr = hrData.hr
        trackedData.ibi.addAll(listOf(hrData.hrIbi, hrData.qIbi))

        validHrData.add(0, trackedData)
        if (validHrData.size > 10) validHrData.removeLast()
    }

     fun send() {
         viewModelScope.launch {
             val nodes = getCapableNodes()
             if (nodes.isNotEmpty()) {

                 val node = nodes.first()
                 val message = encodeMessage(validHrData)
                 messageRepo.sendMessage(message, node, MESSAGE_PATH)

                 // TODO success toast
             } else {
                 // TODO fail toast
             }
         }
    }

    private fun encodeMessage(trackedData: ArrayList<TrackedHrData>): String {
        return Json.encodeToString(trackedData)
    }
}
