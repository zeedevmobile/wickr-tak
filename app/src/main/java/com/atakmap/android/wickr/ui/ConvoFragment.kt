package com.atakmap.android.wickr.ui

import android.content.Context
import android.content.Intent
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.atakmap.android.ipc.AtakBroadcast
import com.atakmap.android.maps.MapView
import com.atakmap.android.wickr.*
import com.atakmap.android.wickr.plugin.R
import com.atakmap.android.wickr.plugin.WickrTool
import com.atakmap.android.wickr.ui.adapters.MessageClickListener
import com.atakmap.android.wickr.ui.adapters.WickrMessageAdapter
import com.atakmap.android.wickr.ui.util.FileUtils
import com.atakmap.android.wickr.utils.SettingsManager
import com.wickr.android.api.WickrAPI
import com.wickr.android.api.WickrAPIObjects
import com.wickr.android.api.WickrAPIRequests
import kotlinx.android.synthetic.main.conversation_view.*
import kotlinx.android.synthetic.main.convo_management.*
import kotlinx.android.synthetic.main.item_wickr_message.*
import org.greenrobot.eventbus.Subscribe
import java.util.*
import kotlin.concurrent.timerTask


class ConvoFragment(
    val pluginContext: Context,
    var convo: WickrAPIObjects.WickrConvo?,
    val standAlone: Boolean = false
) : Fragment(), MessageClickListener {

    private var timer = Timer()
    private val requests: Requests = Requests(pluginContext)
    private val adapter: WickrMessageAdapter = WickrMessageAdapter(this, pluginContext)
    private val settingsManager: SettingsManager = SettingsManager(pluginContext)

    private var isLoading = true
    private var lastVisibleItemPosition = 0
    private var visibleItemCount: Int = 0
    private var visibleItemThreshold: Int = 5
    private var totalItemCount: Int = 0
    private var previousTotalItemCount: Int = 0
    private lateinit var expirationRefresher: MessageExpirationRefresher

    companion object {
        const val PAGE_SIZE: Int = 25
        const val TAG = "ConvoFragment"

        private var instance: ConvoFragment? = null

        fun newInstance(pluginContext: Context): ConvoFragment? {
            if (instance == null) {
                ConvoFragment.instance = ConvoFragment(pluginContext, null)
            }
            return instance
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return LayoutInflater.from(pluginContext)
            .inflate(R.layout.conversation_view, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        moreInfo.visibility = if (convo?.type == WickrAPIObjects.WickrConvo.ConvoType.ROOM ||
            convo?.type == WickrAPIObjects.WickrConvo.ConvoType.GROUP
        ) View.VISIBLE else View.GONE
        moreInfo.setOnClickListener {
            WickrMapComponent.EVENTBUS.post(RoomOrGroupDetailsEvent(convo))
        }
        if (convo?.hasUnreadMentionCount() == true) {
            val clear = Intent(WickrTool.BADGE_ACTION)
            clear.putExtra("count", convo?.unreadMentionCount?.times(-1) ?: 0)
            clear.putExtra("messageId", convo?.id)
            AtakBroadcast.getInstance().sendBroadcast(clear)
        }

        back_ib.setOnClickListener {
            WickrMapComponent.EVENTBUS.post(PopFragmentEvent())
        }

        conversationName_tv.text = convo?.title ?: "Conversation"

        messageList.apply {
            layoutManager = LinearLayoutManager(pluginContext, LinearLayoutManager.VERTICAL, true)
            adapter = this@ConvoFragment.adapter
            setHasFixedSize(true)

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    visibleItemCount = (layoutManager as LinearLayoutManager).childCount
                    totalItemCount = (layoutManager as LinearLayoutManager).itemCount
                    lastVisibleItemPosition =
                        (layoutManager as LinearLayoutManager).findLastVisibleItemPosition()

                    if (isLoading && totalItemCount > previousTotalItemCount) {
                        isLoading = false
                        previousTotalItemCount = totalItemCount
                    }

                    if (!isLoading && (lastVisibleItemPosition + visibleItemThreshold > totalItemCount)) {
                        isLoading = true
                        requestNewPage()
                    }
                }
            })
        }
        expirationRefresher = MessageExpirationRefresher(messageList, adapter)
        messageText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                send_ib.isEnabled = !s.isNullOrEmpty()

                val tintColor = if (send_ib.isEnabled) {
                    R.color.color_accent
                } else R.color.send_button_inactive
                send_ib.setColorFilter(pluginContext.getColor(tintColor), PorterDuff.Mode.SRC_ATOP)
            }
        })

        call_ib.setOnClickListener {
            if (convo?.id != null)
                requests.startCall(convo!!.id)
        }

        fileUploadButton.setOnClickListener {
            WickrMapComponent.EVENTBUS.post(
                SendFileInlineFragmentEvent(
                    convo?.usersList?.get(0)?.id ?: "", convo?.id ?: "", convo?.title ?: ""
                )
            )
        }

        voiceMessage.setOnClickListener {
            WickrMapComponent.EVENTBUS.post(
                SendVoiceMessageFragmentEvent(
                    convo?.usersList?.get(0)?.id ?: "", convo?.id ?: "", convo?.title ?: ""
                )
            )
        }


        send_ib.setOnClickListener {
            requests.sendTextMessage(messageText.text.toString(), convo?.id ?: "")
            messageText.setText("")
        }

        back_ib.setOnClickListener(View.OnClickListener {
            if (standAlone) {
                WickrMapComponent.EVENTBUS.post(RequestCloseDropDownEvent())
            } else {
                WickrMapComponent.EVENTBUS.post(PopFragmentEvent())
            }
        })
    }

    override fun onStart() {
        super.onStart()
        WickrMapComponent.EVENTBUS.register(this)
        expirationRefresher.start()
        timer = Timer()
        timer.schedule(timerTask {
            requests.subscribeToUpdates(true, convo?.id)
        }, 0, 10000)

        if (convo != null) {
            requests.convo(convo!!.id, PAGE_SIZE, 0)
        }
    }

    override fun onStop() {
        super.onStop()
        requests.subscribeToUpdates(false, convo?.id)
        timer.cancel()
        WickrMapComponent.EVENTBUS.unregister(this)
        expirationRefresher.stop()
    }

    @Subscribe
    internal fun onWickrAPIEvent(event: Any) {
        when (event) {
            is WickrMessageListEvent -> {
                messageList.post { adapter.addOrUpdateItems(event.messages) }
            }
            is WickrMessageUpdateResponse -> if (event.convoID == convo?.id) {
                messageList.post { adapter.addOrUpdateItems(event.message) }
                messageList.scrollToPosition(adapter.getItemPos(event.message))
            }
            is WickrConvoEditEvent -> if (event.convo.id == convo?.id) {
                if (event.convo.deleted) WickrMapComponent.EVENTBUS.post(PopFragmentEvent())
                else {
                    var shouldNotPop = false
                    for (u in event.convo.usersList) {
                        if (u.isSelf) {
                            shouldNotPop = true
                        }
                    }
                    if (!shouldNotPop) WickrMapComponent.EVENTBUS.post(PopFragmentEvent())
                }
            }
            is WickrConvoUpdateEvent -> if (event.convo.id == convo?.id) {
                // NOTE: Re-download file otherwise fail to get permission to file
                if (event.convo.lastVisibleMessage.fileMessage.state == WickrAPIObjects.WickrMessage.FileMessage.FileState.AVAILABLE) {
                    requests.transferFile(
                        event.convo.lastVisibleMessage.id,
                        event.convo.lastVisibleMessage.fileMessage
                    )
                }

                if (convo?.hasUnreadMentionCount() == false) {
                    val clear = Intent(WickrTool.BADGE_ACTION)
                    clear.putExtra("count", convo?.unreadMentionCount?.times(-1) ?: 0)
                    clear.putExtra("messageId", convo?.id)
                    AtakBroadcast.getInstance().sendBroadcast(clear)
                }
                refreshConvoData()
                messageList.scrollToPosition(adapter.getItemPos(event.convo.lastVisibleMessage))
            }


            is WickrMessageSendEvent -> if (event.convoID == convo?.id) {
                if (event.error != null) {
                    val errorMessage = "The API was unable to send the message: ${event.error.name}"
                    Log.e(TAG, errorMessage)
                    Toast.makeText(
                        MapView.getMapView().context,
                        "Failed to send message",
                        Toast.LENGTH_SHORT
                    ).show()
                    //Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                }
                if (event.message != null) {
                    messageList.post { adapter.addOrUpdateItems(event.message) }
                    messageList.scrollToPosition(adapter.itemCount)
                }
            }
        }
    }

    override fun onLockedMessageClicked(messageID: String, lockedMessageIds: List<String>) {
        val key = settingsManager.getKey() ?: return
        val unlockMessageRequest = WickrAPIRequests.UnlockMessageRequest.newBuilder()
            .setMessageID(messageID)
            .addAllLockedMessageIDs(lockedMessageIds)
            .build()
        val requestIntent =
            WickrAPI.createRequestIntent(Requests.PACKAGE_NAME, unlockMessageRequest, key)
        AtakBroadcast.getInstance().sendSystemBroadcast(requestIntent);
    }

    override fun onFileMessageClicked(
        messageID: String,
        fileMessage: WickrAPIObjects.WickrMessage.FileMessage
    ) {
        val fileState = fileMessage.state
        Log.d(TAG, "File state: ${fileState.name}")
        when (fileState) {
            WickrAPIObjects.WickrMessage.FileMessage.FileState.ERROR -> {
                Toast.makeText(
                    MapView.getMapView().context,
                    "There was an error processing the file, trying again",
                    Toast.LENGTH_SHORT
                ).show()
                requests.transferFile(messageID, fileMessage)
            }
            WickrAPIObjects.WickrMessage.FileMessage.FileState.NEEDS_DOWNLOAD -> {
                requests.transferFile(messageID, fileMessage)
            }
            WickrAPIObjects.WickrMessage.FileMessage.FileState.AVAILABLE -> {
                val uri = Uri.parse(fileMessage.uri)
                val fileUtils = FileUtils()
                fileUtils.externalOpen(uri, fileMessage.mimeType)
            }

            WickrAPIObjects.WickrMessage.FileMessage.FileState.UNKNOWN -> TODO()
            WickrAPIObjects.WickrMessage.FileMessage.FileState.IN_PROGRESS -> TODO()
        }
    }

    private fun requestNewPage() {
        val key = settingsManager.getKey()
        if (key == null) {
            adapter.clear()
            isLoading = true
            previousTotalItemCount = 0
            return
        }

        Log.d(
            TAG,
            "Requesting page of messages for $convo.id with count $PAGE_SIZE and offset ${adapter.itemCount}"
        )
        val getMessagesRequest = WickrAPIRequests.GetMessagesRequest.newBuilder()
            .setConvoID(convo?.id)
            .setCount(PAGE_SIZE)
            .setOffset(adapter.itemCount)
            .build()
        val requestIntent =
            WickrAPI.createRequestIntent(Requests.PACKAGE_NAME, getMessagesRequest, key)
        AtakBroadcast.getInstance().sendSystemBroadcast(requestIntent)
    }

    private fun convertTime(millis: Long): String {
        var converted = millis
        var convertedStr = "$converted seconds"
        if (converted > 60) {
            converted /= 60
            convertedStr = "$converted minutes"
            if (converted > 60) {
                converted /= 60
                convertedStr = "$converted hours"
                if (converted > 24) {
                    converted /= 24
                    convertedStr = "$converted days"
                }
            }
        }
        return convertedStr
    }

    private fun refreshConvoData() {
        messageText.hint = when {
            convo == null -> pluginContext.getString(R.string.loading_dialog_text)
            convo!!.burnOnRead > 0 && convo!!.burnOnRead <= convo!!.expiration -> "Burns in ${
                convertTime(
                    convo!!.burnOnRead
                )
            }"
            else -> "Expires in ${convertTime(convo!!.expiration)}"
        }
    }
}