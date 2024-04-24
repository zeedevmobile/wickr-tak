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

package com.atakmap.android.wickr.plugin.multisensortracking;

public class HeartRateData {
    public final static int IBI_QUALITY_SHIFT = 15;
    public final static int IBI_MASK = 0x1;
    public final static int IBI_QUALITY_MASK = 0x7FFF;

    int status = HeartRateStatus.HR_STATUS_NONE;
    int hr = 0;
    int ibi = 0;
    int qIbi = 1;

    HeartRateData() {

    }

    HeartRateData(int status, int hr, int ibi, int qIbi) {
        this.status = status;
        this.hr = hr;
        this.ibi = ibi;
        this.qIbi = qIbi;
    }

    int getHrIbi() {
        return (qIbi << IBI_QUALITY_SHIFT) | ibi;
    }
}
