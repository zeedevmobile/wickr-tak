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

class HeartRateData {
    var status = HeartRateStatus.HR_STATUS_NONE
    var hr = 0
    var ibi = 0
    var qIbi = 1

    internal constructor()

    internal constructor(status: Int, hr: Int, ibi: Int, qIbi: Int) {
        this.status = status
        this.hr = hr
        this.ibi = ibi
        this.qIbi = qIbi
    }

    val hrIbi: Int
        get() = qIbi shl IBI_QUALITY_SHIFT or ibi

    companion object {
        const val IBI_QUALITY_SHIFT = 15
        const val IBI_MASK = 0x1
        const val IBI_QUALITY_MASK = 0x7FFF
    }
}
