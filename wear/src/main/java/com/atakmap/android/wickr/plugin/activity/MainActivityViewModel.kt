package com.atakmap.android.wickr.plugin.activity

import android.app.Application
import android.os.CountDownTimer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.atakmap.android.wickr.common.MESSAGE_PATH_WEAR_HR_DATA
import com.atakmap.android.wickr.common.MESSAGE_PATH_WEAR_SPO2_DATA
import com.atakmap.android.wickr.common.WearTrackedHrData
import com.atakmap.android.wickr.common.WearTrackedSpO2Data
import com.atakmap.android.wickr.plugin.R
import com.atakmap.android.wickr.plugin.data.GetCapableNodes
import com.atakmap.android.wickr.plugin.data.MessageRepo
import com.atakmap.android.wickr.plugin.tracking.ConnectionManager
import com.atakmap.android.wickr.plugin.tracking.HeartRateData
import com.atakmap.android.wickr.plugin.tracking.HeartRateListener
import com.atakmap.android.wickr.plugin.tracking.SpO2Listener
import com.atakmap.android.wickr.plugin.tracking.SpO2Status
import com.atakmap.android.wickr.plugin.tracking.TrackerDataNotifier
import com.atakmap.android.wickr.plugin.tracking.TrackerDataObserver
import com.atakmap.android.wickr.plugin.utilities.SingleLiveData
import com.samsung.android.service.health.tracking.HealthTrackerException
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.util.concurrent.atomic.AtomicBoolean

class MainActivityViewModel(application: Application) : AndroidViewModel(application),
    KoinComponent {

    companion object {
        private const val MEASUREMENT_DURATION = 35000
        private const val MEASUREMENT_TICK = 250
    }

    private val trackerDataNotifier: TrackerDataNotifier = get()
    private val messageRepo: MessageRepo = get()
    private val getCapableNodes: GetCapableNodes = get()
    private var connectionManager: ConnectionManager? = null
    private var heartRateListener: HeartRateListener = HeartRateListener()
    private var spO2Listener: SpO2Listener = SpO2Listener()
    private val isMeasurementRunning = AtomicBoolean(false)
    private var currentHr: Int? = null
    private var currentSpO2: Int? = null
    private var currentSpO2MeasurementStatus = SpO2Status.INITIAL_STATUS

    val hrLiveData = MutableLiveData<String>()
    val spO2LiveData = MutableLiveData<String>()
    val spO2MeasurementProgress = MutableLiveData(0)
    val spO2MeasurementStatus = MutableLiveData<Int>()
    val statusUpdates = SingleLiveData<Int>()
    val healthTrackerResolvableException = MutableLiveData<HealthTrackerException>()

    override fun onCleared() {
        super.onCleared()

        heartRateListener.stopTracker()
        spO2Listener.stopTracker()
        trackerDataNotifier.removeObserver(trackerDataObserver)
        connectionManager?.disconnect()
    }

    fun onHrDataReceived(hrData: HeartRateData) {
        currentHr = hrData.hr
        hrLiveData.postValue(hrData.hr.toString())
    }

    fun connect() {
        connectionManager = ConnectionManager(connectionObserver)
        connectionManager?.connect(getApplication<Application>().applicationContext)
    }

    fun sendData() {
        viewModelScope.launch {
            val nodes = getCapableNodes()
            if (nodes.isNotEmpty()) {
                currentHr?.let { sendHrData(it, false) }
                currentSpO2?.let { sendSpO2Data(it, false) }
                statusUpdates.postValue(R.string.health_update_success)
            } else {
                statusUpdates.postValue(R.string.health_update_failed)
            }
        }
    }

    fun sendSpO2Data(sPo2: Int, abnormal: Boolean) {
        viewModelScope.launch {
            val nodes = getCapableNodes()
            if (nodes.isNotEmpty()) {
                val node = nodes.first()
                Json.encodeToString(
                    WearTrackedSpO2Data(sPo2, abnormal)
                ).let {
                    messageRepo.sendMessage(it, node, MESSAGE_PATH_WEAR_SPO2_DATA)
                }

                statusUpdates.postValue(R.string.health_update_success)
            } else {
                statusUpdates.postValue(R.string.health_update_failed)
            }
        }
    }

    fun sendHrData(hr: Int, abnormal: Boolean) {
        viewModelScope.launch {
            val nodes = getCapableNodes()
            if (nodes.isNotEmpty()) {
                val node = nodes.first()
                Json.encodeToString(
                    WearTrackedHrData(hr, abnormal)
                ).let {
                    messageRepo.sendMessage(it, node, MESSAGE_PATH_WEAR_HR_DATA)
                }

                statusUpdates.postValue(R.string.health_update_success)
            } else {
                statusUpdates.postValue(R.string.health_update_failed)
            }
        }
    }

    fun measureSpO2() {
        if (!isMeasurementRunning.get()) {
            currentSpO2MeasurementStatus = SpO2Status.INITIAL_STATUS
            isMeasurementRunning.set(true)
            spO2Listener.startTracker()
            spO2MeasurementProgress.postValue(0)
            Thread { countDownTimer.start() }.start()
        } else {
            isMeasurementRunning.set(false)
            spO2Listener.stopTracker()
            spO2MeasurementProgress.postValue(0)
            spO2MeasurementStatus.postValue(SpO2Status.CANCELLED)
        }
    }

    //   connectionObserver.onConnectionResult(R.string.NoSpo2Support)
    //  connectionObserver.onConnectionResult(R.string.NoHrSupport)

    private val connectionObserver: ConnectionManager.ConnectionObserver =
        object : ConnectionManager.ConnectionObserver {
            override fun connected() {
                trackerDataNotifier.addObserver(trackerDataObserver)
                connectionManager?.apply {
                    setSpO2Listener(spO2Listener)
                    setHeartRateListener(heartRateListener)
                }
                heartRateListener.startTracker()
                statusUpdates.postValue(R.string.connected_to_hs)
            }

            override fun disconnected() {
                statusUpdates.postValue(R.string.disconnected_from_hs)
            }

            override fun onError(healthTrackerException: HealthTrackerException) {
                if (healthTrackerException.errorCode == HealthTrackerException.OLD_PLATFORM_VERSION || healthTrackerException.errorCode == HealthTrackerException.PACKAGE_NOT_INSTALLED) {
                    statusUpdates.postValue(R.string.hs_version_outdated)
                }

                if (healthTrackerException.hasResolution()) {
                    healthTrackerResolvableException.postValue(healthTrackerException)
                } else {
                    statusUpdates.postValue(R.string.hs_connection_error)
                }
            }
        }

    private val trackerDataObserver: TrackerDataObserver = object : TrackerDataObserver {
        override fun onHeartRateTrackerDataChanged(hrData: HeartRateData) {
            onHrDataReceived(hrData)
        }

        override fun onSpO2TrackerDataChanged(status: Int, spO2Value: Int) {
            if (status == currentSpO2MeasurementStatus) {
                return
            }
            currentSpO2MeasurementStatus = status
            spO2MeasurementStatus.postValue(status)

            if (status == SpO2Status.MEASUREMENT_COMPLETED) {
                currentSpO2 = spO2Value
                isMeasurementRunning.set(false)
                spO2Listener.stopTracker()
                spO2LiveData.postValue(spO2Value.toString())
            }
        }

        override fun onError(errorResourceId: Int) {
            statusUpdates.postValue(errorResourceId)
        }
    }

    // Temporary to show SpO2 progress on demo SDK
    private val countDownTimer: CountDownTimer = object : CountDownTimer(
        MEASUREMENT_DURATION.toLong(), MEASUREMENT_TICK.toLong()
    ) {
        override fun onTick(timeLeft: Long) {
            if (isMeasurementRunning.get()) {
                val updatedValue = spO2MeasurementProgress.value!! + 1
                spO2MeasurementProgress.postValue(updatedValue)
            } else cancel()
        }

        override fun onFinish() {
            if (!isMeasurementRunning.get()) return
            spO2MeasurementProgress.postValue(0)
            spO2Listener.stopTracker()
            isMeasurementRunning.set(false)
        }
    }
}
