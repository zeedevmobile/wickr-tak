package com.atakmap.android.wickr.plugin.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import com.atakmap.android.wickr.plugin.R
import com.atakmap.android.wickr.plugin.activity.AlertActivity.Companion.EXTRA_EVENT_TYPE
import com.atakmap.android.wickr.plugin.tracking.SpO2Status
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.zeedev.utilities.extensions.getSerializableExtraCompat

class MainActivity : ComponentActivity() {

    private val viewModel: MainActivityViewModel by viewModels()

    private lateinit var txtHeartRate: TextView
    private lateinit var txtStatus: TextView
    private lateinit var txtSpo2: TextView
    private lateinit var buttonStart: Button
    private lateinit var measurementProgress: CircularProgressIndicator

    private var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val alertType: AlertActivity.AlertType =
                    result.data?.getSerializableExtraCompat(EXTRA_EVENT_TYPE)!!
                when (alertType) {
                    AlertActivity.AlertType.HEART_RATE -> viewModel.sendHrAlert()
                    AlertActivity.AlertType.SPO2 -> viewModel.sendSpO2Alert()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        txtHeartRate = findViewById(R.id.txtHeartRate)
        txtStatus = findViewById(R.id.txtStatus)
        txtSpo2 = findViewById(R.id.txtSpO2)
        buttonStart = findViewById(R.id.button_measure_o2)
        measurementProgress = findViewById(R.id.progress_bar_spo2)

        buttonStart.setOnClickListener {
            viewModel.measureSpO2()
        }

        findViewById<Button>(R.id.button_alert_hr).apply {
            setOnClickListener {
                viewModel.sendHrAlert()
                Intent(this@MainActivity, AlertActivity::class.java).let {
                    it.putExtra(EXTRA_EVENT_TYPE, AlertActivity.AlertType.HEART_RATE)
                    resultLauncher.launch(it)
                }
            }
        }

        findViewById<Button>(R.id.button_alert_o2)?.apply {
            setOnClickListener {
                viewModel.sendSpO2Alert()
                Intent(this@MainActivity, AlertActivity::class.java).let {
                    it.putExtra(EXTRA_EVENT_TYPE, AlertActivity.AlertType.SPO2)
                    resultLauncher.launch(it)
                }
            }
        }

        findViewById<Button>(R.id.button_send)?.apply {
            setOnClickListener {
                viewModel.sendData()
            }
        }

        observeHealthData()
        checkPermissions()
    }

    private fun observeHealthData() {
        viewModel.hrLiveData.observe(this) {
            txtHeartRate.text = it
        }

        viewModel.spO2LiveData.observe(this) {
            txtSpo2.text = it
        }

        viewModel.statusUpdates.observe(this) {
            Toast.makeText(this, getString(it), Toast.LENGTH_SHORT).show()
        }

        viewModel.healthTrackerResolvableException.observe(this) {
            it.resolve(this@MainActivity)
        }

        viewModel.spO2MeasurementProgress.observe(this) {
            measurementProgress.setProgress(it, true)
        }

        viewModel.spO2MeasurementStatus.observe(this) {
            val message = when (it) {
                SpO2Status.DEVICE_MOVING -> R.string.StatusDeviceMoving
                SpO2Status.LOW_SIGNAL -> R.string.StatusLowSignal
                SpO2Status.CANCELLED -> {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    buttonStart.setText(R.string.measure_sp_label)
                    R.string.spo2_cancelled
                }

                SpO2Status.CALCULATING -> {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    buttonStart.setText(R.string.StopLabel)
                    R.string.StatusCalculating
                }

                SpO2Status.MEASUREMENT_COMPLETED -> {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    buttonStart.setText(R.string.measure_sp_label)
                    measurementProgress.setProgress(measurementProgress.max, true)
                    R.string.spo2_success
                }

                else -> {
                    throw Exception("unknown status: $it")
                }
            }

            Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkPermissions() {
        if (ActivityCompat.checkSelfPermission(
                applicationContext, getString(R.string.BodySensors)
            ) == PackageManager.PERMISSION_DENIED
        ) {
            requestPermissions(arrayOf(Manifest.permission.BODY_SENSORS), 0)
        } else {
            viewModel.connect()
        }
    }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)} passing\n      in a {@link RequestMultiplePermissions} object for the {@link ActivityResultContract} and\n      handling the result in the {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    @Suppress("DEPRECATION")
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        if (requestCode == 0) {
            var permissionGranted = true
            for (i in permissions.indices) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    //User denied permissions twice - permanent denial:
                    if (!shouldShowRequestPermissionRationale(permissions[i])) Toast.makeText(
                        applicationContext,
                        getString(R.string.PermissionDeniedPermanently),
                        Toast.LENGTH_SHORT
                    ).show() else Toast.makeText(
                        applicationContext,
                        getString(R.string.PermissionDeniedRationale),
                        Toast.LENGTH_SHORT
                    ).show()
                    permissionGranted = false
                    break
                }
            }
            if (permissionGranted) {
                viewModel.connect()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}
