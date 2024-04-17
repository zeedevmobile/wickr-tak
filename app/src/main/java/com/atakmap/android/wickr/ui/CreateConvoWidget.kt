package com.atakmap.android.wickr.ui

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CompoundButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.atakmap.android.ipc.AtakBroadcast
import com.atakmap.android.maps.MapView
import com.atakmap.android.wickr.*
import com.atakmap.android.wickr.plugin.R
import com.atakmap.android.wickr.utils.SettingsManager
import com.wickr.android.api.WickrAPI
import com.wickr.android.api.WickrAPIObjects
import com.wickr.android.api.WickrAPIRequests
import kotlinx.android.synthetic.main.dialog_create_convo.*
import kotlinx.android.synthetic.main.dialog_create_convo.view.*
import kotlinx.android.synthetic.main.dialog_loading.view.*
import org.greenrobot.eventbus.Subscribe

class CreateConvoWidget(val pluginContext: Context, val userIds: List<String>) : Fragment() {

    private val settingsManager: SettingsManager = SettingsManager(pluginContext)
    private var waitingDialog: Dialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = LayoutInflater.from(pluginContext).inflate(R.layout.dialog_create_convo, container, false);
        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val expirationTimes = pluginContext.resources.getStringArray(R.array.expiration_options).toMutableList()
        val burnOnReadTimes = pluginContext.resources.getStringArray(R.array.burnOnRead_options).toMutableList()

        val expirationInMillis = expirationTimes.map { value ->
            when(value) {
                expirationTimes[0] -> 8L * 60L * 60L // 8 hours
                expirationTimes[1] -> 24L * 60L * 60L // 1 day
                expirationTimes[2] -> 30L * 24L * 60L * 60L // 30 days
                expirationTimes[3] -> 180L * 24L * 60L * 60L // 180 days
                expirationTimes[4] -> 365L * 24L * 60L * 60L // 1 year
                else -> 0L
            }
        }

        val borInMillis = burnOnReadTimes.map { value ->
            when(value) {
                burnOnReadTimes[0] -> 0L // off
                burnOnReadTimes[1] -> 5L // 5 seconds
                burnOnReadTimes[2] -> 60L // 1 minute
                burnOnReadTimes[3] -> 30L * 60L // 30 minutes
                burnOnReadTimes[4] -> 24L * 60L * 60L // 1 day
                else -> 0L
            }
        }

        titleInputLayout.apply {
            visibility = View.VISIBLE
        }
        descriptionInputLayout.apply {
            visibility = View.VISIBLE
        }
        expirationTime.apply {
            visibility = View.VISIBLE
            adapter = ArrayAdapter(pluginContext, R.layout.spinner_item, expirationTimes)
        }
        borTime.apply {
            visibility = View.VISIBLE
            adapter = ArrayAdapter(pluginContext, R.layout.spinner_item, burnOnReadTimes)
        }

        enableModerationSwitch.setOnCheckedChangeListener{_, isChecked ->
            if (isChecked) {
                createTitle.text = pluginContext.getString(R.string.create_room)
                okBtn.text = pluginContext.getString(R.string.create_room)
            } else {
                createTitle.text = pluginContext.getString(R.string.create_group)
                okBtn.text = pluginContext.getString(R.string.create_group)
            }
        }

        okBtn.setOnClickListener {
            val expirationTime = expirationInMillis[expirationTime.selectedItemPosition]
            val borTime =  borInMillis[borTime.selectedItemPosition]

            val title = titleInputLayout.text?.toString() ?: ""
            val description = descriptionInputLayout.text?.toString() ?: ""

            sendCreateConvoRequest(enableModerationSwitch.isChecked, title, description, expirationTime, borTime)
        }

        cancelBtn.setOnClickListener {
            WickrMapComponent.EVENTBUS.post(PopFragmentEvent())
        }
    }

    override fun onStart() {
        super.onStart()
        WickrMapComponent.EVENTBUS.register(this)
    }

    override fun onStop() {
        super.onStop()
        WickrMapComponent.EVENTBUS.unregister(this)
    }

    @Subscribe
    internal fun onWickrApiEvent(event: Any) {
        when(event) {
            is WickrConvoCreatedEvent -> {
                if (event.error != null) {
                    showError(event.error.name)
                }
                exitCreateConvo()
            }
            is WickrInvalidRequestEvent -> {
//                showError(event.toString())
                exitCreateConvo()
            }
        }
    }

    private fun showError(errorMessage: String?) {
        Toast.makeText(
            MapView.getMapView().context,
            "Error creating convo: $errorMessage",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun exitCreateConvo() {
        hideWaitingDialog()
        WickrMapComponent.EVENTBUS.post(PopFragmentEvent())
    }

    private fun showWaitingDialog() {
        val layoutInflater = LayoutInflater.from(pluginContext)
        val rootView = layoutInflater.inflate(R.layout.dialog_loading, null, false)
        rootView.loadingTextView.setText(R.string.waiting_dialog_text)
        waitingDialog = AlertDialog.Builder(requireContext())
            .setView(rootView)
            .setCancelable(false)
            .create().also { it.show() }
    }

    private fun hideWaitingDialog() {
        waitingDialog?.dismiss()
        waitingDialog = null
    }

    private fun sendCreateConvoRequest(enableModeration: Boolean, title: String, description: String, expirationTime: Long, borTime: Long) {
        showWaitingDialog()
        val key = settingsManager.getKey() ?: return
        val convoType = if (enableModeration) {
            WickrAPIObjects.WickrConvo.ConvoType.ROOM
        } else {
            WickrAPIObjects.WickrConvo.ConvoType.GROUP
        }

        val createConvoRequest = WickrAPIRequests.CreateConvoRequest.newBuilder()
            .setType(convoType)
            .setTitle(title)
            .setDescription(description)
            .setExpiration(expirationTime)
            .setBurnOnRead(borTime)
            .addAllUserIDs(userIds)
            .build()
        val intent = WickrAPI.createRequestIntent(Requests.PACKAGE_NAME, createConvoRequest, key)
        AtakBroadcast.getInstance().sendSystemBroadcast(intent)
    }
}