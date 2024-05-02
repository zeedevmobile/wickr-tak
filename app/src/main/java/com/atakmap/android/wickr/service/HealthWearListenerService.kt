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
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

private const val TAG = "DataListenerService"
private const val MESSAGE_PATH = "/msg"

class HealthWearListenerService : WearableListenerService() {

    companion object {
        const val COT_DETAIL_NAME = "wear_health_details"
        const val ACTION_HEALTH_DATA_MESSAGE = "ACTION_HEALTH_DATA_MESSAGE"
        const val EXTRA_HEALTH_DATA = "EXTRA_HEALTH_DATA"
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)

        if (messageEvent.path == MESSAGE_PATH) {
            val data = messageEvent.data.decodeToString()
            Log.i(TAG, "Service: message (/msg) received: $data")

            if (data.isNotEmpty()) {
                Intent().also {
                    it.action = ACTION_HEALTH_DATA_MESSAGE
                    it.putExtra(EXTRA_HEALTH_DATA, data)
                    sendBroadcast(it)
                }
            } else {
                Log.i(TAG, "data is empty")
            }
        }
    }
}
