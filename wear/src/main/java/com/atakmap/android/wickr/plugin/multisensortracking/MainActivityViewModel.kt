package com.atakmap.android.wickr.plugin.multisensortracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atakmap.android.wickr.common.TrackedHealthData
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

    private var currentHr: Int? = null
    private var currentSpO2Data: Int? = null

    // TODO need to move the listeners into the VM
    fun onHrDataReceived(hrData: HeartRateData) {
        currentHr = hrData.hr
    }

    fun onSpO2DataReceived(value: Int) {
        currentSpO2Data = value
    }

    fun sendData() {
        viewModelScope.launch {
            val nodes = getCapableNodes()
            if (nodes.isNotEmpty()) {
                val node = nodes.first()
                encodeHrData(
                    TrackedHealthData(
                        currentHr,
                        currentSpO2Data,
                        null,
                        null
                    )
                ).also {
                    messageRepo.sendMessage(it, node, MESSAGE_PATH)
                }

                // TODO success toast
            } else {
                // TODO fail toast
            }
        }
    }

    fun sendHrAlert() {
        viewModelScope.launch {
            val nodes = getCapableNodes()
            if (nodes.isNotEmpty()) {
                val node = nodes.first()
                encodeHrData(
                    TrackedHealthData(
                        null,
                        null,
                        160,
                        null
                    )
                ).also {
                    messageRepo.sendMessage(it, node, MESSAGE_PATH)
                }

                // TODO success toast
            } else {
                // TODO fail toast
            }
        }
    }

    fun sendSpO2Alert() {
        viewModelScope.launch {
            val nodes = getCapableNodes()
            if (nodes.isNotEmpty()) {
                val node = nodes.first()
                encodeHrData(
                    TrackedHealthData(
                        null,
                        null,
                        null,
                        84
                    )
                ).also {
                    messageRepo.sendMessage(it, node, MESSAGE_PATH)
                }

                // TODO success toast
            } else {
                // TODO fail toast
            }
        }
    }

    private fun encodeHrData(trackedData: TrackedHealthData): String {
        return Json.encodeToString(trackedData)
    }
}
