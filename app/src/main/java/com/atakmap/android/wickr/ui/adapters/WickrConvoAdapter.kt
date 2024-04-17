package com.atakmap.android.wickr.ui.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.atakmap.android.ipc.AtakBroadcast
import com.atakmap.android.wickr.WickrUserAvatarUpdateEvent
import com.atakmap.android.wickr.initials
import com.atakmap.android.wickr.plugin.R
import com.atakmap.android.wickr.plugin.WickrTool
import com.atakmap.android.wickr.primaryName
import com.atakmap.android.wickr.ui.UserAvatarCache
import com.atakmap.android.wickr.utils.SettingsManager
import com.wickr.android.api.WickrAPIObjects.*
import com.wickr.android.api.WickrAPIObjects.WickrMessage.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

interface ConvoClickListener {
    fun onConvoClicked(convo: WickrConvo)
}

class WickrConvoViewHolder(layout: View) : RecyclerView.ViewHolder(layout) {
    val userImageView: ImageView = itemView.findViewById(R.id.userImageView)
    val userImageText: TextView = itemView.findViewById(R.id.userImageText)
    val title: TextView = itemView.findViewById(R.id.title)
    val message: TextView = itemView.findViewById(R.id.message)
    val timestamp: TextView = itemView.findViewById(R.id.timestamp)
    val unreadMentionText: TextView = itemView.findViewById(R.id.unreadMentionCount)
}

class WickrConvoAdapter(private val clickListener: ConvoClickListener, private val pluginContext: Context) :
    RecyclerView.Adapter<WickrConvoViewHolder>() {

    private val dateFormat = SimpleDateFormat("hh:mma MM/dd", Locale.getDefault())
    private val items: ArrayList<WickrConvo> = arrayListOf()
    private var settingsManager: SettingsManager
    private var userAvatarCache: UserAvatarCache

    init {
        settingsManager = SettingsManager(pluginContext)
        userAvatarCache = UserAvatarCache.getInstance(pluginContext)!!
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WickrConvoViewHolder {
        val layout = LayoutInflater.from(pluginContext)
            .inflate(R.layout.item_wickr_convo, parent, false)
        return WickrConvoViewHolder(layout)
    }

    override fun onBindViewHolder(holder: WickrConvoViewHolder, position: Int) {
        val item = items[position]
        val message = if (item.hasLastVisibleMessage()) {
            item.lastVisibleMessage
        } else null
        refreshUserAvatar(holder, item)

        holder.itemView.setOnClickListener {
            clickListener.onConvoClicked(item)
        }
        if (item.unreadMessageCount > 0) {
            holder.unreadMentionText.text = item.unreadMessageCount.toString()
            holder.unreadMentionText.isVisible = true
        } else {
            holder.unreadMentionText.isVisible = false
        }
        holder.title.text = item.title
        holder.message.text = if (message != null) {
            when (message.contentsCase) {
                ContentsCase.TEXTMESSAGE -> message.textMessage.text
                ContentsCase.FILEMESSAGE -> message.fileMessage.fileName
                ContentsCase.CALLMESSAGE -> if (!item.activeCallID.isNullOrEmpty()) {
                    "Call Status = ${message.callMessage.status.name} Call ID = ${item.activeCallID}"
                } else "Call Status = ${message.callMessage.status.name}"
                ContentsCase.LOCATIONMESSAGE -> {
                    pluginContext.getString(R.string.show_on_map)
                }
                ContentsCase.CONTROLMESSAGE -> message.controlMessage.text
                else -> ""
            }
        } else ""
        if (message != null) {
            holder.message.text = "${message.sender.primaryName().split(" ")[0]}: ${holder.message.text}"
        }

        holder.timestamp.text = if (item.lastVisibleMessage != null && item.hasLastVisibleMessage()) {
            var formattedDate = dateFormat.format(item.lastVisibleMessage!!.timestamp)
            if (isToday(item.lastVisibleMessage!!.timestamp)) {
                formattedDate = formattedDate.substringBefore(" ")
            }
            formattedDate
        } else ""
    }

    private fun refreshUserAvatar(holder: WickrConvoViewHolder, item: WickrConvo) {
        if (item.hasLastVisibleMessage() || item.type == WickrConvo.ConvoType.DM) {
            var sender = if (item.type == WickrConvo.ConvoType.DM) item.getUsers(0) else item.lastVisibleMessage.sender
            var dmContact = item.getUsers(0)
            // not ideal
            if (item.type == WickrConvo.ConvoType.DM) {
                for (user in item.usersList) {
                    if (user.fullName.equals(item.title)) {
                        dmContact = user
                        sender = user
                    }
                }
            }
            var userAvatar = if (item.type == WickrConvo.ConvoType.DM) userAvatarCache.get(dmContact.id) else userAvatarCache.get(sender.id)
            if (userAvatar != null) {
                holder.userImageText.text = ""
                holder.userImageView.visibility = View.VISIBLE
                holder.userImageView.setImageBitmap(userAvatar)
            } else {
                holder.userImageText.text = if (item.type == WickrConvo.ConvoType.DM) dmContact.initials() else sender.initials()
                holder.userImageView.visibility = View.INVISIBLE
                holder.userImageView.setImageDrawable(null)
            }
        } else {
            holder.userImageText.text = ""
            holder.userImageView.visibility = View.INVISIBLE
            holder.userImageView.setImageDrawable(null)
        }
    }

    override fun getItemCount(): Int = items.size

    fun setItems(data: List<WickrConvo>) {
        items.clear()
        items.addAll(data)
        val clear = Intent(WickrTool.BADGE_ACTION)
        for (c in data) { // make sure the badge on the plugin icon is zeroed out
            if (!c.hasUnreadMentionCount()) {
                clear.putExtra("messageId", c.id)
                clear.putExtra("count", 0)
                AtakBroadcast.getInstance().sendBroadcast(clear)
            }
        }


        //val sum = Intent(WickrTool.BADGE_ACTION)
        //sum.putExtra("count", items.sumOf { it.unreadMessageCount })

        //AtakBroadcast.getInstance().sendBroadcast(sum)

        notifyDataSetChanged()
    }

    fun updateItems(data: List<WickrConvo>) {
        val updatedItems = ArrayList(items)
        for (convo in data) {
            if (convo.deleted) {
                updatedItems.removeIf { it.id == convo.id }
                continue
            }

            val index = updatedItems.indexOfFirst { it.id == convo.id }
            if (index >= 0) {
                updatedItems.removeAt(index)
                updatedItems.add(index, convo)
                continue
            }

            updatedItems.add(0, convo)
        }
        updatedItems.sortByDescending { it.updateTimestamp }
        setItems(updatedItems)
    }

    private fun isToday(messageTimestamp: Long): Boolean {
        val messageDate = Calendar.getInstance(Locale.getDefault()).apply {
            time = Date(messageTimestamp)
        }
        val todayDate = Calendar.getInstance(Locale.getDefault()).apply {
            time = Date(System.currentTimeMillis())
        }
        val sameYear = messageDate[Calendar.YEAR] == todayDate[Calendar.YEAR]
        val sameDayOfYear = messageDate[Calendar.DAY_OF_YEAR] == todayDate[Calendar.DAY_OF_YEAR]
        return sameYear && sameDayOfYear
    }
}
