/*
 * Copyright 2022 Samsung Electronics Co., Ltd. All Rights Reserved.
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
package com.atakmap.android.wickr.plugin.multisensortracking

import java.util.function.Consumer

class TrackerDataNotifier {

    private val observers: MutableList<TrackerDataObserver> = ArrayList()

    fun addObserver(observer: TrackerDataObserver) {
        observers.add(observer)
    }

    fun removeObserver(observer: TrackerDataObserver) {
        observers.remove(observer)
    }

    fun notifyHeartRateTrackerObservers(hrData: HeartRateData) {
        observers.forEach(Consumer { observer: TrackerDataObserver ->
            observer.onHeartRateTrackerDataChanged(
                hrData
            )
        })
    }

    fun notifySpO2TrackerObservers(status: Int, spO2Value: Int) {
        observers.forEach(Consumer { observer: TrackerDataObserver ->
            observer.onSpO2TrackerDataChanged(
                status,
                spO2Value
            )
        })
    }

    fun notifyError(errorResourceId: Int) {
        observers.forEach(Consumer { observer: TrackerDataObserver ->
            observer.onError(
                errorResourceId
            )
        })
    }
}
