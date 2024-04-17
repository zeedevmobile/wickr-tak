package com.atakmap.android.wickr.ui.adapters

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.atakmap.android.maps.MapItem
import com.atakmap.android.maps.MapView
import com.atakmap.android.maps.Marker
import com.atakmap.android.user.PlacePointTool
import com.atakmap.android.user.PlacePointTool.MarkerCreator
import com.atakmap.android.wickr.Requests
import com.atakmap.android.wickr.WickrUserAvatarUpdateEvent
import com.atakmap.android.wickr.formatAsSize
import com.atakmap.android.wickr.initials
import com.atakmap.android.wickr.plugin.R
import com.atakmap.android.wickr.ui.UserAvatarCache
import com.atakmap.android.wickr.utils.SettingsManager
import com.atakmap.coremap.maps.coords.GeoPoint
import com.atakmap.coremap.maps.coords.GeoPointMetaData
import com.atakmap.map.CameraController
import com.wickr.android.api.WickrAPIObjects.WickrMessage
import com.wickr.android.api.WickrAPIObjects.WickrMessage.*
import kotlinx.android.synthetic.main.item_reaction_layout.view.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class WickrMessageViewHolder(layout: View) : RecyclerView.ViewHolder(layout) {

    val userImageView: ImageView = itemView.findViewById(R.id.userImageView)
    val userImageText: TextView = itemView.findViewById(R.id.userImageText)
    val title: TextView = itemView.findViewById(R.id.title)
    val message: TextView = itemView.findViewById(R.id.message)
    val timestamp: TextView = itemView.findViewById(R.id.timestamp)
    val fileIcon: ImageView = itemView.findViewById(R.id.fileIcon)
    val fileExtensionText: TextView = itemView.findViewById(R.id.fileExtensionText)
    val fileLayout: LinearLayout = itemView.findViewById(R.id.fileLayout)
    val fileNameText: TextView = itemView.findViewById(R.id.fileNameText)
    val fileSizeText: TextView = itemView.findViewById(R.id.fileSizeText)
    val reactionsLayout: LinearLayout = itemView.findViewById(R.id.reactionsLayout)
    val downloadState: ImageView = itemView.findViewById(R.id.downloadStateBtn)
    val msgView: ConstraintLayout = itemView.findViewById(R.id.msg_view)
    val sendState: TextView = itemView.findViewById(R.id.sendState)
    val messageExpiration: TextView = itemView.findViewById(R.id.messageExpiration)
}

interface MessageClickListener {
    fun onLockedMessageClicked(messageID: String, lockedMessageIds: List<String>)
    fun onFileMessageClicked(messageID: String, fileMessage: FileMessage)
}

class LocationClickListener(private val message: WickrMessage) : View.OnClickListener {
    override fun onClick(v: View?) {
        val id = "Wickr-${message.id}"
        val exist: MapItem? = MapView.getMapView().rootGroup.deepFindItem("callsign", id)

        if (exist == null) {
            val mc: PlacePointTool.MarkerCreator = MarkerCreator(
                GeoPointMetaData(
                    GeoPoint(
                        message.locationMessage.latitude,
                        message.locationMessage.longitude
                    )
                )
            )
            //todo: what is a unique name for map marker
            mc.setCallsign(message.sender.username)
            mc.setUid(UUID.randomUUID().toString())
            mc.setType("a-f-G-U-C-I")
            mc.setHow("m-g")
            mc.setMetaString("entry", "user")
            mc.showCotDetails(false)
            mc.setNeverPersist(true)
            val m: Marker = mc.placePoint()
            m.refresh(MapView.getMapView().mapEventDispatcher, null, this.javaClass)
            CameraController.Programmatic.panTo(MapView.getMapView().renderer3, m.point, true)
        }

    }

}

class WickrMessageAdapter(private val clickListener: MessageClickListener, private val pluginContext: Context) :
    RecyclerView.Adapter<WickrMessageViewHolder>() {

    private val dateFormat = SimpleDateFormat("hh:mma MM/dd", Locale.getDefault())
    private val items: ArrayList<WickrMessage> = arrayListOf()
    private var settingsManager: SettingsManager
    private var userAvatarCache: UserAvatarCache
    private var requests = Requests(pluginContext)

    init {
        settingsManager = SettingsManager(pluginContext)
        userAvatarCache = UserAvatarCache.getInstance(pluginContext)!!
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WickrMessageViewHolder {
        val layout = LayoutInflater.from(pluginContext)
            .inflate(R.layout.item_wickr_message, parent, false)

        return WickrMessageViewHolder(layout)
    }

    override fun onBindViewHolder(holder: WickrMessageViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty()) {
            val userAvatarEvent = payloads[0] as? WickrUserAvatarUpdateEvent
            if (userAvatarEvent != null) {
                val item = items[position]
                if (item.sender.id == userAvatarEvent.userID) {
                    refreshUserAvatar(holder, item)
                }
                return
            }
        }
        super.onBindViewHolder(holder, position, payloads)
    }

    override fun onBindViewHolder(holder: WickrMessageViewHolder, position: Int) {
        val item = items[position]
        refreshUserAvatar(holder, item)

        if(item.sender.isSelf)
            holder.msgView.setBackgroundColor(Color.DKGRAY)
        else
            holder.msgView.setBackgroundColor(Color.BLACK)

        if (item.hasSendState()) {
            if(item.sendState == SendState.FAILED) {
                holder.sendState.visibility = View.VISIBLE
                holder.messageExpiration.visibility = View.GONE
            }
        }
        holder.title.text = item.sender.username
        holder.timestamp.text = dateFormat.format(item.timestamp)

        if (item.expirationTime > 0) {
            val expireDate = Date(item.expirationTime)
            val now = Date()
            val expireDiff = expireDate.time - now.time;
            var expireRes = TimeUnit.DAYS.convert(expireDiff, TimeUnit.MILLISECONDS)
            var expireLabel = "${TimeUnit.DAYS.convert(expireDiff, TimeUnit.MILLISECONDS)}D"
            if (expireRes <= 0) {
                expireRes = TimeUnit.HOURS.convert(expireDiff, TimeUnit.MILLISECONDS)
                expireLabel = "${TimeUnit.HOURS.convert(expireDiff, TimeUnit.MILLISECONDS)}H"
                if (expireRes <= 1) {
                    expireRes = TimeUnit.MINUTES.convert(expireDiff, TimeUnit.MILLISECONDS)
                    expireLabel = "${TimeUnit.MINUTES.convert(expireDiff, TimeUnit.MILLISECONDS)}M"
                    if (expireRes <= 1) {
                        expireLabel = "${TimeUnit.SECONDS.convert(expireDiff, TimeUnit.MILLISECONDS)}S"
                        holder.messageExpiration.setTextColor(pluginContext.getColor(R.color.red))
                    }

                }
            }
            holder.messageExpiration.text = expireLabel
        }

        when (item.contentsCase) {
            ContentsCase.LOCKEDMESSAGE -> {
                holder.message.visibility = View.VISIBLE
                holder.message.text = "<Locked> - click to unlock"

                holder.itemView.setOnClickListener {
                    val lockedMessageIds = items.takeWhile { it.contentsCase == ContentsCase.LOCKEDMESSAGE }
                        .filter { it.timestamp < item.timestamp }
                        .map { it.id }
                    clickListener.onLockedMessageClicked(item.id, lockedMessageIds)
                }
            }
            ContentsCase.TEXTMESSAGE -> {
                holder.message.visibility = View.VISIBLE
                holder.fileIcon.visibility = View.GONE
                holder.fileExtensionText.visibility = View.GONE
                holder.fileLayout.visibility = View.GONE
                holder.messageExpiration.visibility = View.VISIBLE
                holder.message.text = item.textMessage.text.highlightText(
                    ContextCompat.getColor(holder.itemView.context, R.color.white), item.textMessage.mentionsList
                )

                if (item.lastEditTimestamp != 0L) {
                    val timestampText = holder.timestamp.text
                    holder.timestamp.text = "Edited | $timestampText"
                }
                Linkify.addLinks(holder.message, Linkify.WEB_URLS or Linkify.MAP_ADDRESSES)
                bindMessageReactions(holder, item)
            }
            ContentsCase.FILEMESSAGE -> {
                holder.message.visibility = View.GONE
                holder.fileIcon.visibility = View.VISIBLE
                holder.fileExtensionText.visibility = View.VISIBLE
                holder.fileLayout.visibility = View.VISIBLE

                val fileName = item.fileMessage.fileName
                val fileSize = item.fileMessage.fileSize

                holder.fileNameText.text = fileName

                val extensionIndex = fileName.lastIndexOf(".")
                if (extensionIndex > -1) {
                    holder.fileExtensionText.text = fileName.substring(extensionIndex + 1).toUpperCase()
                }

                holder.fileSizeText.text = fileSize.formatAsSize()
                holder.fileSizeText.text = "${holder.fileSizeText.text}"
                holder.downloadState.setImageDrawable(getDrawable(item.fileMessage.state))


                if (item.fileMessage.hasProgress()) {
                    val percentage = "${item.fileMessage.progress}%"
                    holder.fileSizeText.text = "${holder.fileSizeText.text} ($percentage)"
                }

                bindMessageReactions(holder, item)
                holder.itemView.setOnClickListener {
                    clickListener.onFileMessageClicked(item.id, item.fileMessage)
                }
            }
            ContentsCase.CALLMESSAGE -> {
                holder.message.visibility = View.VISIBLE
                holder.fileIcon.visibility = View.GONE
                holder.fileExtensionText.visibility = View.GONE
                holder.fileLayout.visibility = View.GONE

                holder.message.text = "Call Status = ${item.callMessage.status.name}, Duration = ${item.callMessage.duration}"

                bindMessageReactions(holder, item)
            }
            ContentsCase.LOCATIONMESSAGE -> {
                holder.message.visibility = View.VISIBLE
                holder.fileIcon.visibility = View.GONE
                holder.fileExtensionText.visibility = View.GONE
                holder.fileLayout.visibility = View.VISIBLE

                holder.message.text = pluginContext.getString(R.string.show_on_map) + "\n\n"
                holder.message.setOnClickListener(LocationClickListener(item))

                holder.downloadState.setImageDrawable(pluginContext.getDrawable(R.drawable.pin_drop))
                holder.downloadState.visibility = View.VISIBLE
                bindMessageReactions(holder, item)
            }
            ContentsCase.CONTROLMESSAGE -> {
                holder.message.visibility = View.VISIBLE
                holder.fileIcon.visibility = View.GONE
                holder.fileExtensionText.visibility = View.GONE
                holder.fileLayout.visibility = View.GONE
                holder.title.visibility = View.GONE
                holder.message.text = item.controlMessage.text
                holder.messageExpiration.visibility = View.GONE
                clearReactionList(holder)
            }

            ContentsCase.CONTENTS_NOT_SET -> TODO()
        }
    }

    private fun convertTimeCompact(millis: Long) : String {
        var converted = millis / 1000
        var convertedStr = "$converted S"
        if (converted > 60) {
            converted /= 60
            convertedStr = "$converted M"
            if (converted > 60) {
                converted /= 60
                convertedStr = "$converted H"
                if (converted > 24) {
                    converted /= 24
                    convertedStr = "$converted D"
                }
            }
        }
        return convertedStr
    }

    private fun getDrawable(state: FileMessage.FileState) : Drawable? {
        when(state) {
            FileMessage.FileState.NEEDS_DOWNLOAD -> return pluginContext.getDrawable(R.drawable.file_download)
            FileMessage.FileState.IN_PROGRESS -> return pluginContext.getDrawable(R.drawable.downloading)
            FileMessage.FileState.AVAILABLE -> return pluginContext.getDrawable(R.drawable.file_open)
            FileMessage.FileState.UNKNOWN -> TODO()
            FileMessage.FileState.ERROR -> TODO()
        }
        return pluginContext.getDrawable(R.drawable.file_download)
    }

    private fun refreshUserAvatar(holder: WickrMessageViewHolder, item: WickrMessage) {
        val sender = item.sender

        val userAvatar = userAvatarCache.get(sender.id)
        if (userAvatar != null) {
            holder.userImageText.text = ""
            holder.userImageView.visibility = View.VISIBLE
            holder.userImageView.setImageBitmap(userAvatar)
        } else {
            holder.userImageText.text = sender.initials()
            holder.userImageView.visibility = View.INVISIBLE
            holder.userImageView.setImageDrawable(null)
        }
    }

    private fun bindMessageReactions(holder: WickrMessageViewHolder, message: WickrMessage) {
        clearReactionList(holder)

        holder.reactionsLayout.apply {
            this.visibility = if (message.reactionsList.isEmpty()) View.GONE else View.VISIBLE
            message.reactionsList.forEachIndexed { index, reactionData ->
                val reactionView = LayoutInflater.from(pluginContext).inflate(R.layout.item_reaction_layout, this, false).also {
                    this.addView(it, index)
                }

                reactionView.reactionIconTextView.text = reactionData.reaction
                reactionView.reactionTextView.text = "${reactionData.usersCount}"
            }
        }
    }

    private fun clearReactionList(holder: WickrMessageViewHolder) {
        holder.reactionsLayout.apply {
            this.visibility = View.GONE
            while (childCount > 0) {
                val childView = getChildAt(0)
                removeView(childView)
            }
        }
    }

    private fun CharSequence.highlightText(
        highlightColor: Int,
        mentions: List<TextMessage.MentionData> = emptyList(),
    ): CharSequence = SpannableStringBuilder(this).apply {
        mentions.asSequence().sortedByDescending { it.startIndex }
            .forEach {
                try {
                    if (it.startIndex >= 0 && it.endIndex <= length) {
                        if (it.hasUser()) {
                            val name = when {
                                it.user.hasCustomName() -> {
                                    it.user.customName
                                }
                                it.user.hasFullName() -> {
                                    it.user.fullName
                                }
                                else -> {
                                    it.user.username
                                }
                            }
                            replace(it.startIndex, it.endIndex, "@$name")
                            setSpan(ForegroundColorSpan(highlightColor), it.startIndex, it.startIndex + name.length + 1, 0)
                        } else {
                            val name = "@all"
                            replace(it.startIndex, it.endIndex, name)
                            setSpan(
                                ForegroundColorSpan(highlightColor),
                                it.startIndex,
                                it.startIndex + name.length,
                                0
                            )
                        }
                    }
                } catch (e: Exception) {
                    // mention contains garbage data. ignore it
                }
            }
    }

    override fun getItemCount(): Int = items.size

    fun clear() {
        items.clear()
        notifyDataSetChanged()
    }

    fun addOrUpdateItems(data: WickrMessage) {
        addOrUpdateItems(listOf(data))
    }

    fun getItemPos(data: WickrMessage) : Int {
        var cnt = 0;
        for (message in items) {
            if (message.id == data.id) {
                return cnt;
            }
            cnt++;
        }
        return items.count();
    }

    fun addOrUpdateItems(data: List<WickrMessage>) {
        for (message in data) {
            val index = items.indexOfFirst { it.id == message.id }
            if (index >= 0) {
                items.removeAt(index)
                if (!message.isDeleted) {
                    items.add(index, message)
                }
            } else if (!message.isDeleted) {
                items.add(items.size, message)
            }
        }
        items.sortByDescending { it.timestamp }
        notifyDataSetChanged()
    }
}
