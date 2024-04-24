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

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import com.atakmap.android.wickr.plugin.R
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.samsung.android.service.health.tracking.HealthTrackerException
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : ComponentActivity() {

    companion object {
        private const val APP_TAG = "MainActivity"
        private const val MEASUREMENT_DURATION = 35000
        private const val MEASUREMENT_TICK = 250
    }

    private val viewModel: MainActivityViewModel by viewModels()

    private lateinit var txtHeartRate: TextView
    private lateinit var txtStatus: TextView
    private lateinit var txtSpo2: TextView
    private lateinit var butStart: Button
    private lateinit var measurementProgress: CircularProgressIndicator

    private val isMeasurementRunning = AtomicBoolean(false)
    private var connectionManager: ConnectionManager? = null
    private var heartRateListener: HeartRateListener? = null
    private var spO2Listener: SpO2Listener? = null
    private var connected = false
    private var permissionGranted = false
    private var previousStatus = SpO2Status.INITIAL_STATUS
    private var heartRateDataLast = HeartRateData()

    private val countDownTimer: CountDownTimer =
        object : CountDownTimer(MEASUREMENT_DURATION.toLong(), MEASUREMENT_TICK.toLong()) {
            override fun onTick(timeLeft: Long) {
                if (isMeasurementRunning.get()) {
                    runOnUiThread {
                        measurementProgress.setProgress(
                            measurementProgress.progress + 1, true
                        )
                    }
                } else cancel()
            }

            override fun onFinish() {
                if (!isMeasurementRunning.get()) return
                Log.i(APP_TAG, "Failed measurement")
                runOnUiThread {
                    txtStatus.setText(R.string.MeasurementFailed)
                    txtStatus.invalidate()
                    txtSpo2.setText(R.string.SpO2DefaultValue)
                    txtSpo2.invalidate()
                    butStart.setText(R.string.StartLabel)
                    measurementProgress.progress = 0
                    measurementProgress.invalidate()
                }
                spO2Listener!!.stopTracker()
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                isMeasurementRunning.set(false)
            }
        }

    private val trackerDataObserver: TrackerDataObserver = object : TrackerDataObserver {
        override fun onHeartRateTrackerDataChanged(hrData: HeartRateData) {

            if(hrData.status != 1) return

            viewModel.onHrDataReceived(hrData)

            runOnUiThread {
                heartRateDataLast = hrData
                Log.i(APP_TAG, "HR Status: " + hrData.status)
                if (hrData.status == HeartRateStatus.HR_STATUS_FIND_HR) {
                    txtHeartRate.text = hrData.hr.toString()
                    Log.i(APP_TAG, "HR: " + hrData.hr)
                } else {
                    txtHeartRate.text = getString(R.string.HeartRateDefaultValue)
                }
            }
        }

        override fun onSpO2TrackerDataChanged(status: Int, spO2Value: Int) {
            if (status == previousStatus) {
                return
            }
            previousStatus = status
            when (status) {
                SpO2Status.CALCULATING -> {
                    Log.i(APP_TAG, "Calculating measurement")
                    runOnUiThread {
                        txtStatus.setText(R.string.StatusCalculating)
                        txtStatus.invalidate()
                    }
                }

                SpO2Status.DEVICE_MOVING -> {
                    Log.i(APP_TAG, "Device is moving")
                    runOnUiThread {
                        Toast.makeText(
                            applicationContext,
                            R.string.StatusDeviceMoving,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                SpO2Status.LOW_SIGNAL -> {
                    Log.i(APP_TAG, "Low signal quality")
                    runOnUiThread {
                        Toast.makeText(
                            applicationContext,
                            R.string.StatusLowSignal,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                SpO2Status.MEASUREMENT_COMPLETED -> {
                    Log.i(APP_TAG, "Measurement completed")
                    isMeasurementRunning.set(false)
                    spO2Listener!!.stopTracker()
                    runOnUiThread {
                        txtStatus.setText(R.string.StatusCompleted)
                        txtStatus.invalidate()
                        txtSpo2.text = spO2Value.toString()
                        txtSpo2.invalidate()
                        butStart.setText(R.string.StartLabel)
                        measurementProgress.setProgress(measurementProgress.max, true)
                    }
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
        }

        override fun onError(errorResourceId: Int) {
            runOnUiThread {
                Toast.makeText(
                    applicationContext,
                    getString(errorResourceId),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    private val connectionObserver: ConnectionObserver = object : ConnectionObserver {
        override fun onConnectionResult(stringResourceId: Int) {
            runOnUiThread {
                Toast.makeText(
                    applicationContext,
                    getString(stringResourceId),
                    Toast.LENGTH_LONG
                ).show()
            }
            if (stringResourceId != R.string.ConnectedToHs) {
                finish()
            }
            connected = true
            TrackerDataNotifier.getInstance().addObserver(trackerDataObserver)
            spO2Listener = SpO2Listener()
            heartRateListener = HeartRateListener()
            connectionManager!!.initSpO2(spO2Listener)
            connectionManager!!.initHeartRate(heartRateListener)
            heartRateListener!!.startTracker()
        }

        override fun onError(e: HealthTrackerException) {
            if (e.errorCode == HealthTrackerException.OLD_PLATFORM_VERSION || e.errorCode == HealthTrackerException.PACKAGE_NOT_INSTALLED) runOnUiThread {
                Toast.makeText(
                    applicationContext,
                    getString(R.string.HealthPlatformVersionIsOutdated),
                    Toast.LENGTH_LONG
                ).show()
            }
            if (e.hasResolution()) {
                e.resolve(this@MainActivity)
            } else {
                runOnUiThread {
                    Toast.makeText(
                        applicationContext,
                        getString(R.string.ConnectionError),
                        Toast.LENGTH_LONG
                    ).show()
                }
                Log.e(APP_TAG, "Could not connect to Health Tracking Service: " + e.message)
            }
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        txtHeartRate = findViewById(R.id.txtHeartRate)
        txtStatus = findViewById(R.id.txtStatus)
        txtSpo2 = findViewById(R.id.txtSpO2)
        butStart = findViewById(R.id.butStart)
        measurementProgress = findViewById(R.id.progressBar)

        butStart.setOnClickListener {
            viewModel.send()
           // performMeasurement()
        }

        adjustProgressBar(measurementProgress)

        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                getString(R.string.BodySensors)
            ) == PackageManager.PERMISSION_DENIED
        ) requestPermissions(
            arrayOf(Manifest.permission.BODY_SENSORS), 0
        ) else {
            permissionGranted = true
            createConnectionManager()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (heartRateListener != null) heartRateListener!!.stopTracker()
        if (spO2Listener != null) spO2Listener!!.stopTracker()
        TrackerDataNotifier.getInstance().removeObserver(trackerDataObserver)
        if (connectionManager != null) {
            connectionManager!!.disconnect()
        }
    }

    private fun createConnectionManager() {
        try {
            connectionManager = ConnectionManager(connectionObserver)
            connectionManager!!.connect(applicationContext)
        } catch (t: Throwable) {
            Log.e(APP_TAG, t.message!!)
        }
    }

    private fun adjustProgressBar(progressBar: CircularProgressIndicator?) {
        val displayMetrics = this.resources.displayMetrics
        val pxWidth = displayMetrics.widthPixels
        val padding = 1
        progressBar!!.setPadding(padding, padding, padding, padding)
        val trackThickness = progressBar.trackThickness
        val progressBarSize = pxWidth - trackThickness - 2 * padding
        progressBar.indicatorSize = progressBarSize
    }

    /*private fun onDetails() {
        if (isPermissionsOrConnectionInvalid) {
            return
        }
        val intent = Intent(this, DetailsActivity::class.java)
        intent.putExtra(getString(R.string.ExtraHr), heartRateDataLast.hr)
        intent.putExtra(getString(R.string.ExtraHrStatus), heartRateDataLast.status)
        intent.putExtra(getString(R.string.ExtraIbi), heartRateDataLast.ibi)
        intent.putExtra(getString(R.string.ExtraQualityIbi), heartRateDataLast.qIbi)
        startActivity(intent)
    }*/

    private fun performMeasurement() {
        if (isPermissionsOrConnectionInvalid) {
            return
        }
        if (!isMeasurementRunning.get()) {
            previousStatus = SpO2Status.INITIAL_STATUS
            butStart.setText(R.string.StopLabel)
            txtSpo2.setText(R.string.SpO2DefaultValue)
            measurementProgress.progress = 0
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            spO2Listener?.startTracker()
            isMeasurementRunning.set(true)
            Thread { countDownTimer.start() }.start()
        } else {
            butStart.setEnabled(false)
            isMeasurementRunning.set(false)
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            spO2Listener?.stopTracker()
            val progressHandler = Handler(Looper.getMainLooper())
            progressHandler.postDelayed({
                butStart.setText(R.string.StartLabel)
                txtStatus.setText(R.string.StatusDefaultValue)
                measurementProgress.progress = 0
                butStart.setEnabled(true)
            }, (MEASUREMENT_TICK * 2).toLong())
        }
    }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)} passing\n      in a {@link RequestMultiplePermissions} object for the {@link ActivityResultContract} and\n      handling the result in the {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    @Suppress("DEPRECATION")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == 0) {
            permissionGranted = true
            for (i in permissions.indices) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    //User denied permissions twice - permanent denial:
                    if (!shouldShowRequestPermissionRationale(permissions[i])) Toast.makeText(
                        applicationContext,
                        getString(R.string.PermissionDeniedPermanently),
                        Toast.LENGTH_LONG
                    ).show() else Toast.makeText(
                        applicationContext,
                        getString(R.string.PermissionDeniedRationale),
                        Toast.LENGTH_LONG
                    ).show()
                    permissionGranted = false
                    break
                }
            }
            if (permissionGranted) {
                createConnectionManager()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private val isPermissionsOrConnectionInvalid: Boolean
        get() {
            if (ActivityCompat.checkSelfPermission(
                    applicationContext,
                    getString(R.string.BodySensors)
                ) == PackageManager.PERMISSION_DENIED
            ) requestPermissions(
                arrayOf(Manifest.permission.BODY_SENSORS), 0
            )
            if (!permissionGranted) {
                Log.i(APP_TAG, "Could not get permissions. Terminating measurement")
                return true
            }
            if (!connected) {
                Toast.makeText(
                    applicationContext,
                    getString(R.string.ConnectionError),
                    Toast.LENGTH_SHORT
                ).show()
                return true
            }
            return false
        }
}
