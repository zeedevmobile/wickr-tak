package com.atakmap.android.wickr.wear.data

import com.samsung.android.service.health.tracking.data.DataPoint
import com.samsung.android.service.health.tracking.data.ValueKey

class IBIDataParsing {

    companion object {
        private fun isIBIValid(ibiStatus: Int, ibiValue: Int): Boolean {
            return ibiStatus == 0 && ibiValue != 0
        }

        fun getValidIbiList(dataPoint: DataPoint): ArrayList<Int> {

            val ibiValues = dataPoint.getValue(ValueKey.HeartRateSet.IBI_LIST)
            val ibiStatuses = dataPoint.getValue(ValueKey.HeartRateSet.IBI_STATUS_LIST)

            val validIbiList = ArrayList<Int>()
            for ((i, ibiStatus) in ibiStatuses.withIndex()) {
                if (isIBIValid(ibiStatus, ibiValues[i])) {
                    validIbiList.add(ibiValues[i])
                }
            }
            return validIbiList
        }
    }
}