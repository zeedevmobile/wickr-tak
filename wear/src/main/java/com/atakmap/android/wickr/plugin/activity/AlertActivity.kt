package com.atakmap.android.wickr.plugin.activity

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.animation.Animation
import androidx.activity.ComponentActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.atakmap.android.wickr.plugin.R
import com.zeedev.utilities.extensions.getSerializableExtraCompat


class AlertActivity : ComponentActivity() {

    companion object {
        const val EXTRA_EVENT_TYPE = "extra_event_type"
    }

    enum class AlertType {
        HEART_RATE,
        SPO2
    }

    private lateinit var layoutBackground: ConstraintLayout
    private lateinit var alertType: AlertType
    private lateinit var imageViewAlertType: AppCompatImageView
    private lateinit var textViewAlert: AppCompatTextView
    private lateinit var vibrator: Vibrator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alert)

        alertType = intent.getSerializableExtraCompat(EXTRA_EVENT_TYPE)!!

        imageViewAlertType = findViewById(R.id.imageview_alert_type)
        textViewAlert = findViewById(R.id.textview_alert)

        layoutBackground = findViewById<ConstraintLayout?>(R.id.layout_alert).apply {
            setOnClickListener {
                Intent().let {
                    it.putExtra(EXTRA_EVENT_TYPE, alertType)
                    setResult(RESULT_OK, it);
                    finish();
                }
            }
        }

        setAlertType()
        animate()
        vibrate()
    }

    override fun onDestroy() {
        super.onDestroy()

        vibrator.cancel()
    }

    private fun setAlertType() {
        when (alertType) {
            AlertType.HEART_RATE -> {
                imageViewAlertType.setImageResource(R.drawable.ic_hr)
                textViewAlert.text = "175"
            }

            AlertType.SPO2 -> {
                imageViewAlertType.setImageResource(R.drawable.ic_spo2)
                textViewAlert.text = "84"
            }
        }
    }

    private fun vibrate() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 1000, 1000), 0))

    }

    private fun animate() {
        val colorAnimation =
            ValueAnimator.ofObject(ArgbEvaluator(), Color.RED, Color.rgb(255, 100, 0))
        colorAnimation.duration = 750L
        colorAnimation.addUpdateListener { animator -> layoutBackground.setBackgroundColor(animator.animatedValue as Int) }
        colorAnimation.repeatCount = Animation.INFINITE
        colorAnimation.repeatMode = ValueAnimator.REVERSE
        colorAnimation.start()
    }
}
