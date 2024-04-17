package com.atakmap.android.wickr

import android.content.Context
import com.wickr.android.api.WickrAPI.createPairingAckIntent
import com.atakmap.android.wickr.utils.SettingsManager
import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.widget.Toast
import com.atakmap.android.ipc.AtakBroadcast
import com.atakmap.android.maps.MapView
import com.atakmap.android.wickr.plugin.R
import com.atakmap.android.wickr.plugin.WickrTool
import com.atakmap.android.wickr.ui.SendFileFragment
import com.atakmap.android.wickr.ui.UserAvatarCache
import com.atakmap.android.wickr.ui.adapters.WickrUserAdapter
import com.atakmap.android.wickr.ui.util.PhoneConvoReceiver
import com.wickr.android.api.*
import com.wickr.android.api.WickrAPIResponses.GetContactsResponse
import com.wickr.android.api.WickrAPIResponses.SuccessResponse
import com.wickr.android.api.WickrAPIResponses.GetConvosResponse
import com.wickr.android.api.WickrAPIResponses.PairingResponse
import com.wickr.android.api.WickrAPIResponses.PairingDenialResponse

class WickrAPIReceiver(pluginContext: Context) : WickrAPIResponseReceiver() {
    private val pluginContext: Context
    var settingsManager: SettingsManager?
    var userAvatarCache: UserAvatarCache

    private var currentMessageId: String = ""

    override fun getEncryptionKey(): ByteArray? {
        return settingsManager?.getKey()
    }

    override fun onInternalError(error: WickrAPIInternalError) {
        Log.e(TAG, "Could not process response: ${error.name}")
    }

    override fun onWickrAPIResponseReceived(
        context: Context,
        response: WickrAPIResponses.WickrAPIResponse
    ) {
        Log.i(TAG, "Received ${response.responseCase.name} response for ID ${response.identifier}")
    }

    override fun onSuccessResponseReceived(
        context: Context,
        identifier: String,
        response: SuccessResponse
    ) {
        when (identifier) {
            "pairingAckRequest" -> WickrMapComponent.EVENTBUS.post(WickrAPIPairedEvent())
        }
    }

    override fun onErrorResponseReceived(
        context: Context,
        identifier: String,
        response: WickrAPIResponses.ErrorResponse
    ) {
        Log.e(TAG, "The API returned an error ${response.error.name} for request ID: $identifier")
        val avatarPrefix = "UserAvatar-"
        if (identifier.startsWith(avatarPrefix)) {
            val userID = identifier.removePrefix(avatarPrefix)
            Log.d(TAG, "Setting user $userID as not having an avatar")
            userAvatarCache.setHasNoAvatar(userID)
            return
        }
        if (response.error == WickrAPIObjects.APIError.SYNCING) {
            WickrMapComponent.EVENTBUS.post(WickrSyncingEvent())
        } else if (response.error == WickrAPIObjects.APIError.NOT_PAIRED) {
            resetKeys()
        } else if (response.error == WickrAPIObjects.APIError.INVALID_REQUEST) {
            //Toast.makeText(MapView.getMapView().context, "Error trying to create the request: ${response.error.name}", Toast.LENGTH_SHORT).show()
            WickrMapComponent.EVENTBUS.post(WickrInvalidRequestEvent(response.error))
        }
    }

    override fun onPairingResponseReceived(
        context: Context,
        identifier: String,
        response: PairingResponse
    ) {
        Log.i(TAG, "Processing API pairing response")
        if (!response.hasEncryptionKey()) {
            Log.e(TAG, "Missing encryption key")
            return
        }

        val encryptionKey = response.encryptionKey.toByteArray()
        settingsManager?.saveKey(encryptionKey)
        val request = WickrAPI.createPairingAckIntent(
            Requests.PACKAGE_NAME,
            encryptionKey,
            "pairingAckRequest"
        )
        AtakBroadcast.getInstance().sendSystemBroadcast(request)
    }

    override fun onPairingDenialResponseReceived(
        context: Context,
        identifier: String,
        response: PairingDenialResponse
    ) {
        Log.i(TAG, "Processing API pairing denial response")
        resetKeys()
    }

    private fun resetKeys() {
        settingsManager?.clear()
        WickrMapComponent.EVENTBUS.post(WickrAPIUnpairedEvent())
    }

    override fun onGetContactsResponseReceived(
        context: Context,
        identifier: String,
        response: GetContactsResponse
    ) {
        if (WickrUserAdapter.SEARCH_ID.equals(identifier)) {
            WickrMapComponent.EVENTBUS.post(WickrContactSearchListEvent(response.contactsList))
        } else {
            WickrMapComponent.EVENTBUS.post(WickrContactListEvent(response.contactsList))
        }
    }

    override fun onGetConvosResponseReceived(
        context: Context,
        identifier: String,
        response: GetConvosResponse
    ) {
        WickrMapComponent.EVENTBUS.post(WickrConvoListEvent(identifier, response.convosList))
    }


    override fun onConvoUpdateResponseReceived(
        context: Context,
        identifier: String,
        response: WickrAPIResponses.ConvoUpdateResponse
    ) {
        WickrMapComponent.EVENTBUS.post(WickrConvoUpdateEvent(identifier, response.convo))
    }

    override fun onCreateConvoResponseReceived(
        context: Context,
        identifier: String,
        response: WickrAPIResponses.CreateConvoResponse
    ) {
        if (response.hasError()) {
            WickrMapComponent.EVENTBUS.post(WickrConvoCreatedEvent(error = response.error))
        } else {
            if (identifier.equals(PhoneConvoReceiver.FOR_PHONE_CALL)) {
                WickrMapComponent.EVENTBUS.post(WickrConvoCreatedForPhoneEvent(convo = response.convo))
            } else if (identifier.equals(SendFileFragment.SENDFILE_ID)) {
                WickrMapComponent.EVENTBUS.post(WickrConvoCreatedForFileEvent(convo = response.convo))
            } else {
                WickrMapComponent.EVENTBUS.post(WickrConvoCreatedEvent(convo = response.convo))
            }
        }
    }

    override fun onEditConvoResponseReceived(
        context: Context,
        identifier: String,
        response: WickrAPIResponses.EditConvoResponse
    ) {
        if (response.hasError()) {
            WickrMapComponent.EVENTBUS.post(WickrConvoEditEvent(response.error, response.convo))
        } else {
            WickrMapComponent.EVENTBUS.post(WickrConvoEditEvent(convo = response.convo))
        }
    }

    override fun onGetMessagesResponseReceived(
        context: Context,
        identifier: String,
        response: WickrAPIResponses.GetMessagesResponse
    ) {
        WickrMapComponent.EVENTBUS.post(
            WickrMessageListEvent(
                response.convoID,
                response.messagesList
            )
        )
    }

    override fun onMessageUpdateResponseReceived(
        context: Context,
        identifier: String,
        response: WickrAPIResponses.MessageUpdateResponse
    ) {
        WickrMapComponent.EVENTBUS.post(
            WickrMessageUpdateResponse(
                response.convoID,
                response.message
            )
        )
    }

    override fun onSendMessageResponseReceived(
        context: Context,
        identifier: String,
        response: WickrAPIResponses.SendMessageResponse
    ) {
        WickrMapComponent.EVENTBUS.post(RequestDeleteFileEvent())
        if (response.hasError()) {
            if (response.message.hasFileMessage()) {

                Toast.makeText(
                    MapView.getMapView().context,
                    pluginContext.getString(R.string.failed_send),
                    Toast.LENGTH_SHORT
                )
            }
        } else {
            if (response.message.hasFileMessage()) {

                Toast.makeText(
                    MapView.getMapView().context,
                    pluginContext.getString(R.string.success_send),
                    Toast.LENGTH_SHORT
                )
            }
        }
    }

    override fun onSubscriptionResponseReceived(
        context: Context,
        identifier: String,
        response: WickrAPIResponses.SubscriptionResponse
    ) {


        when {
            !response.hasConvoID() -> Log.i(TAG, "No active subscriptions")
            response.convoID.isEmpty() -> Log.i(TAG, "Subscribed to all convo updates")
            else -> Log.i(TAG, "Subscribed to updates for ${response.convoID}")
        }
    }


    override fun onMessageNotificationResponseReceived(
        context: Context,
        identifier: String,
        response: WickrAPIResponses.MessageNotificationResponse
    ) {
        val key = getEncryptionKey()
        if (key == null) {
            Log.e(TAG, "Not paired")
            return
        }

        Log.i(
            TAG,
            "Acknowledging new message notification for ${response.messageID} in ${response.convoID}"
        )
        val messageAckRequest = WickrAPIRequests.MessageNotificationAckRequest.newBuilder()
            .setConvoID(response.convoID)
            .setMessageID(response.messageID)
            .build()
        val ackRequest = WickrAPI.createRequestIntent(Requests.PACKAGE_NAME, messageAckRequest, key)
        AtakBroadcast.getInstance().sendSystemBroadcast(ackRequest)

        Log.i(TAG, "Showing new message notification")
        val i = Intent(WickrTool.BADGE_ACTION)
        i.putExtra("count", 1)
        i.putExtra("messageId", response.convoID)
        AtakBroadcast.getInstance().sendBroadcast(i)
    }

    override fun onWickrUserSettingsResponseReceived(
        context: Context,
        identifier: String,
        response: WickrAPIResponses.GetUserSettingsResponse
    ) {
        WickrMapComponent.EVENTBUS.post(WickrUserSettingsEvent(response.settings))
    }


    override fun onWickrUserAvatarResponseReceived(
        context: Context,
        identifier: String,
        response: WickrAPIResponses.GetUserAvatarResponse
    ) {
        val bitmap = with(response.userAvatar.toByteArray()) {
            Log.i(TAG, "Loading bitmap for ${response.userID} with size: ${this.size}")
            BitmapFactory.decodeByteArray(this, 0, this.size)
        }
        userAvatarCache.put(response.userID, bitmap)
        WickrMapComponent.EVENTBUS.post(WickrUserAvatarUpdateEvent(response.userID))
    }

    companion object {
        private val TAG = WickrAPIReceiver::class.java.simpleName
        const val PAIRING_IDENTIFIER = "pairingIdentifier"
        const val ENCRYPTION_KEY_KEY = "encryption-key"
    }

    init {
        Log.d(TAG, "Receiver created")
        this.pluginContext = pluginContext
        settingsManager = SettingsManager(pluginContext)
        userAvatarCache = UserAvatarCache.getInstance(pluginContext)!!
    }
}