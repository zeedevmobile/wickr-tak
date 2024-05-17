/*
 * Copyright 2023 Samsung Electronics Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.atakmap.android.wickr.service

import android.content.Intent
import android.util.Log
import com.atakmap.android.wickr.common.MESSAGE_PATH_WEAR_HR_DATA
import com.atakmap.android.wickr.common.MESSAGE_PATH_WEAR_SPO2_DATA
import com.atakmap.android.wickr.common.WearTrackedHrData
import com.atakmap.android.wickr.common.WearTrackedSpO2Data
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.serialization.json.Json

class HealthWearListenerService : WearableListenerService() {

    companion object {
        private const val TAG = "HealthWearListenerService"

        const val ACTION_HEALTH_DATA_HR_UPDATE = "ACTION_HEALTH_DATA_HR_UPDATE"
        const val ACTION_HEALTH_DATA_SPO2_UPDATE = "ACTION_HEALTH_DATA_SPO2_UPDATE"
        const val EXTRA_HEALTH_DATA = "EXTRA_HEALTH_DATA"
        const val EXTRA_IS_ABNORMAL = "EXTRA_IS_ABNORMAL"
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)

        val decodedDataString = messageEvent.data.decodeToString()
        if (decodedDataString.isEmpty()) {
            Log.d(TAG, "data is empty")
            return
        }
        Log.i(TAG, "Service: message path: ${messageEvent.path} received: $decodedDataString")

        when (messageEvent.path) {
            MESSAGE_PATH_WEAR_HR_DATA -> {
                Json.decodeFromString<WearTrackedHrData>(decodedDataString).let {
                    Intent().also { intent ->
                        intent.action = ACTION_HEALTH_DATA_HR_UPDATE
                        intent.putExtra(EXTRA_HEALTH_DATA, it.hr)
                        intent.putExtra(EXTRA_IS_ABNORMAL, it.abnormal)
                        sendBroadcast(intent)
                    }
                }
            }

            MESSAGE_PATH_WEAR_SPO2_DATA -> {
                Json.decodeFromString<WearTrackedSpO2Data>(decodedDataString).let {
                    Intent().also { intent ->
                        intent.action = ACTION_HEALTH_DATA_SPO2_UPDATE
                        intent.putExtra(EXTRA_HEALTH_DATA, it.sPo2)
                        intent.putExtra(EXTRA_IS_ABNORMAL, it.abnormal)
                        sendBroadcast(intent)
                    }
                }
            }
        }
    }
}
