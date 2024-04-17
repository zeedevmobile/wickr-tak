package com.atakmap.android.wickr

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.core.content.FileProvider
import com.atakmap.android.ipc.AtakBroadcast
import com.atakmap.android.maps.MapView
import com.atakmap.android.util.FileProviderHelper
import com.atakmap.android.wickr.ui.ContactsListFragment
import com.atakmap.android.wickr.ui.ConvoFragment
import com.atakmap.android.wickr.ui.adapters.WickrUserAdapter
import com.atakmap.android.wickr.utils.SettingsManager
import com.wickr.android.api.WickrAPI
import com.wickr.android.api.WickrAPI.createRequestIntent
import com.wickr.android.api.WickrAPIObjects
import com.wickr.android.api.WickrAPIRequests
import com.wickr.android.api.WickrAPIRequests.CreateConvoRequest
import kotlinx.android.synthetic.main.conversation_view.*
import java.io.File

class Requests(private val pluginContext: Context) {

    private var settingsManager: SettingsManager?

    fun settings() {
        val key = settingsManager?.getKey() ?: return
        val getUserSettingsRequest = WickrAPIRequests.GetUserSettingsRequest.newBuilder()
            .build()
        val intent = WickrAPI.createRequestIntent(PACKAGE_NAME, getUserSettingsRequest, key)
        AtakBroadcast.getInstance().sendSystemBroadcast(intent)
    }

    fun contacts() {
        val key = settingsManager?.getKey()
        val getContactsRequest = WickrAPIRequests.GetContactsRequest.newBuilder().setQuery("")
            .setOffset(0)
            .setCount(ContactsListFragment.PAGE_SIZE)
            .build()
        if (key != null) {
            val requestIntent =
                WickrAPI.createRequestIntent(PACKAGE_NAME, getContactsRequest, key, "contacts")
            AtakBroadcast.getInstance().sendSystemBroadcast(requestIntent)
        } else {
            Log.w(TAG, "The wickr api key is null")
        }

    }

    fun createConvo(wickrId: String, messageIdentifier: String = "") {
        val key = settingsManager?.getKey() ?: return
        val createConvoRequest = CreateConvoRequest.newBuilder()
            .setType(WickrAPIObjects.WickrConvo.ConvoType.DM)
            .addUserIDs(wickrId)
            .build()

        val cCRIntent = createRequestIntent(PACKAGE_NAME, createConvoRequest, key, messageIdentifier)
        AtakBroadcast.getInstance().sendSystemBroadcast(cCRIntent)
    }

    fun sendFile(convoId: String, f: File, fileSize: Long, mimeType: String) {
        val key = settingsManager?.getKey() ?: return
        val uri = FileProviderHelper.fromFile(MapView.getMapView().context, f)
        val cursor = MapView.getMapView().context.contentResolver.query(uri, null, null, null, null)
        if (cursor != null) {
            val mimeType = MapView.getMapView().context.contentResolver.getType(uri) ?: ""
            var fileName = "attachment"
            var fileSize = 0L

            cursor.use {
                it.moveToFirst()
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                fileName = it.getString(nameIndex)
                fileSize = it.getLong(sizeIndex)
            }

            // grant permission so Wickr will be able to read the file
            var pkg = settingsManager?.getPref("WickrPkg");

            MapView.getMapView().context.grantUriPermission(pkg, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

            val fileMessage = WickrAPIObjects.WickrMessage.FileMessage.newBuilder()
                .setUri(uri.toString())
                .setFileName(fileName)
                .setFileSize(fileSize)
                .setMimeType(mimeType)
                .build()
            val sendMessageRequest = WickrAPIRequests.SendMessageRequest.newBuilder()
                .setConvoID(convoId)
                .setFileMessage(fileMessage)
                .build()
            val fileIntent = createRequestIntent(PACKAGE_NAME, sendMessageRequest, key, "")
                .apply{ flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION }

            AtakBroadcast.getInstance().sendSystemBroadcast(fileIntent)
        }

    }

    fun searchContacts(searchQuery: String) {
        val key = settingsManager?.getKey() ?: return
        val searchContactsRequest = WickrAPIRequests.GetContactsRequest.newBuilder()
            .setQuery(searchQuery)
            .setUseDirectory(true)
            .build()
        val requestIntent = WickrAPI.createRequestIntent(PACKAGE_NAME, searchContactsRequest, key, WickrUserAdapter.SEARCH_ID)
        AtakBroadcast.getInstance().sendSystemBroadcast(requestIntent)
    }

    fun convos(convoType: WickrAPIObjects.WickrConvo.ConvoType) {
        val getConvosRequest = WickrAPIRequests.GetConvosRequest.newBuilder()
            .addTypes(convoType)
            .build()
        val key = settingsManager?.getKey()
        if (key != null) {
            val requestIntent =
                WickrAPI.createRequestIntent(PACKAGE_NAME, getConvosRequest, key, "convos")
            AtakBroadcast.getInstance().sendSystemBroadcast(requestIntent)
        } else {
            Log.w(TAG, "The wickr api key is null")
        }
    }

    fun createConvo(contactId: String) {
        val key = settingsManager?.getKey() ?: return

        val createConvoRequest = CreateConvoRequest.newBuilder()
            .setType(WickrAPIObjects.WickrConvo.ConvoType.DM)
            .addAllUserIDs(listOf(contactId))
            .build()
        val intent =
            createRequestIntent(PACKAGE_NAME, createConvoRequest, key, "DM-$contactId")
        AtakBroadcast.getInstance().sendSystemBroadcast(intent)
    }

    fun convo(id: String, pageSize: Int, offset: Int) {
        val key = settingsManager?.getKey() ?: return

        val getMessagesRequest = WickrAPIRequests.GetMessagesRequest.newBuilder()
            .setConvoID(id)
            .setCount(pageSize)
            .setOffset(offset)
            .build()

        val requestIntent = WickrAPI.createRequestIntent(PACKAGE_NAME, getMessagesRequest, key)
        AtakBroadcast.getInstance().sendSystemBroadcast(requestIntent)
    }

    fun transferFile(messageID: String, message: WickrAPIObjects.WickrMessage.FileMessage) {
        val key = settingsManager?.getKey() ?: return
        Log.d(ConvoFragment.TAG,"Requesting to download file for message ${messageID}")
        val fileDownloadRequest = WickrAPIRequests.FileDownloadRequest.newBuilder()
            .setMessageID(messageID)
            .build()
        val intent = WickrAPI.createRequestIntent(PACKAGE_NAME, fileDownloadRequest, key)

        AtakBroadcast.getInstance().sendSystemBroadcast(intent)
    }

    fun startCall(id: String) {
        val key = settingsManager?.getKey() ?: return

        val callRequest = WickrAPIRequests.CallRequest.newBuilder()
            .setAction(WickrAPIRequests.CallRequest.CallAction.START)
            .setConvoID(id)
            .build()
        val intent = WickrAPI.createRequestIntent(
            PACKAGE_NAME,
            callRequest,
            key
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        MapView.getMapView().context.startActivity(intent)
    }

    fun subscribeToUpdates(subscribe: Boolean) {
        subscribeToUpdates(subscribe, "")
    }

    fun subscribeToUpdates(subscribe: Boolean, convoId: String?) {
        val key = settingsManager?.getKey() ?: return

        Log.i(TAG, "Subscribing to convo updates: $subscribe")
        val subscriptionRequest = WickrAPIRequests.SubscriptionRequest.newBuilder()
            .setSubscribe(subscribe)
            .setConvoID(convoId)
            .build()
        val requestIntent = WickrAPI.createRequestIntent(PACKAGE_NAME, subscriptionRequest, key)
        AtakBroadcast.getInstance().sendSystemBroadcast(requestIntent)
    }

    fun sendTextMessage(messageText: String, convoId: String) {
        val key = settingsManager?.getKey() ?: return
        val textMessage = WickrAPIObjects.WickrMessage.TextMessage.newBuilder()
            .setText(messageText)
        val sendMessageRequest = WickrAPIRequests.SendMessageRequest.newBuilder()
            .setConvoID(convoId)
            .setTextMessage(textMessage)
            .build()
        AtakBroadcast.getInstance().sendSystemBroadcast(WickrAPI.createRequestIntent(Requests.PACKAGE_NAME, sendMessageRequest, key, "SendMessage-${convoId}"))
    }

    fun removeUserFromConvo(userId: String, convo: WickrAPIObjects.WickrConvo?) {
        val key = settingsManager?.getKey() ?: return
        var userList: ArrayList<String> = arrayListOf()
        for (u in convo?.usersList!!) {
            if (u.id != userId) userList.add(u.id)
        }
        val wickrConvoEdit = WickrAPIObjects.WickrConvoEdit.newBuilder().apply {
            addAllUsers(userList)
        }

        val editConvoRequest = WickrAPIRequests.EditConvoRequest.newBuilder()
            .setConvoID(convo?.id)
            .setChanges(wickrConvoEdit)
            .build()

        val intent = WickrAPI.createRequestIntent(PACKAGE_NAME, editConvoRequest, key)

        AtakBroadcast.getInstance().sendSystemBroadcast(intent)
    }

    fun addUsersToConvo(selectedIds: List<String>, convo: WickrAPIObjects.WickrConvo?) {
        val key = settingsManager?.getKey() ?: return
        var userList: ArrayList<String> = arrayListOf()
        for (u in convo?.usersList!!) {
            userList.add(u.id)
        }
        userList.addAll(selectedIds)
        val wickrConvoEdit = WickrAPIObjects.WickrConvoEdit.newBuilder().apply {
            addAllUsers(userList)
        }
        val editConvoRequest = WickrAPIRequests.EditConvoRequest.newBuilder()
            .setConvoID(convo?.id)
            .setChanges(wickrConvoEdit)
            .build()

        val intent = WickrAPI.createRequestIntent(PACKAGE_NAME, editConvoRequest, key)

        AtakBroadcast.getInstance().sendSystemBroadcast(intent)
    }

    fun refreshConvosFromWickr(convoType: WickrAPIObjects.WickrConvo.ConvoType) {
        val key = settingsManager?.getKey() ?: return

        val getConvosRequest = WickrAPIRequests.GetConvosRequest.newBuilder()
            .apply {
                when (convoType) {
                    WickrAPIObjects.WickrConvo.ConvoType.ROOM -> addAllTypes(listOf(
                        WickrAPIObjects.WickrConvo.ConvoType.ROOM,
                        WickrAPIObjects.WickrConvo.ConvoType.GROUP
                    ))
                    WickrAPIObjects.WickrConvo.ConvoType.DM -> addAllTypes(listOf(
                        WickrAPIObjects.WickrConvo.ConvoType.DM
                    ))

                    WickrAPIObjects.WickrConvo.ConvoType.UNKNOWN -> TODO()
                    WickrAPIObjects.WickrConvo.ConvoType.GROUP -> TODO()
                }
            }
            .build()
        val requestIntent = WickrAPI.createRequestIntent(
            PACKAGE_NAME,
            getConvosRequest,
            key,
            convoType.name ?: ""
        )
        AtakBroadcast.getInstance().sendSystemBroadcast(requestIntent)
    }

    companion object {
        val PACKAGE_NAME: String = "com.atakmap.app.civ"
        val TAG: String = Requests.javaClass.simpleName
    }

    init {
        settingsManager = SettingsManager(pluginContext)
    }
}