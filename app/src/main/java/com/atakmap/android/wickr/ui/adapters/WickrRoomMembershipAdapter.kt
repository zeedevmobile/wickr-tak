package com.atakmap.android.wickr.ui.adapters

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.atakmap.android.maps.MapView
import com.atakmap.android.wickr.Requests
import com.atakmap.android.wickr.WickrContactSearchListEvent
import com.atakmap.android.wickr.WickrUserAvatarUpdateEvent
import com.atakmap.android.wickr.initials
import com.atakmap.android.wickr.plugin.R
import com.atakmap.android.wickr.ui.UserAvatarCache
import com.atakmap.android.wickr.utils.SettingsManager
import com.wickr.android.api.WickrAPIObjects.*
import org.greenrobot.eventbus.Subscribe


class MembershipUserViewHolder(layout: View) : RecyclerView.ViewHolder(layout) {
    val userImageView: ImageView = itemView.findViewById(R.id.userImageView)
    val userImageText: TextView = itemView.findViewById(R.id.userImageText)
    val fullNameTextView: TextView = itemView.findViewById(R.id.fullNameTextView)
    val userNameTextView: TextView = itemView.findViewById(R.id.userNameTextView)
    val moderator: TextView = itemView.findViewById(R.id.roleType)
    val memberLayout: ConstraintLayout = itemView.findViewById(R.id.memberLayout)
}

data class UserModeratorWrapper(val user: WickrUser, var isModerator: Boolean)

class WickrRoomMembershipAdapter(private val pluginContext: Context, private val isModerator: Boolean = false) :
    RecyclerView.Adapter<MembershipUserViewHolder>() {
    private var settingsManager: SettingsManager
    private var userAvatarCache: UserAvatarCache
    private var items: ArrayList<UserModeratorWrapper> = arrayListOf()
    val requests = Requests(pluginContext)
    private var convo: WickrConvo? = null

    init {
        settingsManager = SettingsManager(pluginContext)
        userAvatarCache = UserAvatarCache.getInstance(pluginContext)!!
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MembershipUserViewHolder {
        val layout = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_wickr_user_membership, parent, false)
        return MembershipUserViewHolder(layout)
    }

    override fun onBindViewHolder(holder: MembershipUserViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty()) {
            val userAvatarEvent = payloads[0] as? WickrUserAvatarUpdateEvent
            if (userAvatarEvent != null) {
                val item = items[position]
                if (item != null && item.user.id == userAvatarEvent.userID) {
                    refreshUserAvatar(holder, item.user)
                }
                return
            }
        }
        super.onBindViewHolder(holder, position, payloads)
    }

    override fun onBindViewHolder(holder: MembershipUserViewHolder, position: Int) {
        val item = items[position]

        refreshUserAvatar(holder, item.user)
        holder.fullNameTextView.text = item.user.fullName.let {
            if (!it.isNullOrBlank()) {
                if (item.user.isBot) {
                    "$it [BOT]"
                } else it
            } else item.user.username
        }
        holder.userNameTextView.text = item.user.username
        holder.moderator.visibility = if (item.isModerator) View.VISIBLE else View.GONE
        if (isModerator) {
            holder.memberLayout.setOnLongClickListener {
                val alertDialog = AlertDialog.Builder(MapView.getMapView().context)
                    .setTitle(pluginContext.getString(R.string.confirm_user_delete))
                    .setMessage(pluginContext.getString(R.string.user_delete_text))
                    .setNegativeButton(pluginContext.getString(R.string.cancel)) { dialog, _ -> dialog.dismiss() }
                    .setPositiveButton(pluginContext.getString(R.string.ok)) { dialog, _ ->
                        //sendConvoDeleteLeaveRequest(isLeave)
                        requests.removeUserFromConvo(item.user.id, convo)
                        dialog.dismiss()
                    }

                alertDialog.show()
                true
            }
        }
    }

    private fun refreshUserAvatar(holder: MembershipUserViewHolder, item: WickrUser) {
        val userAvatar = userAvatarCache.get(item.id)
        if (userAvatar != null) {
            holder.userImageText.text = ""
            holder.userImageView.visibility = View.VISIBLE
            holder.userImageView.setImageBitmap(userAvatar)
        } else {
            holder.userImageText.text = item.initials()
            holder.userImageView.visibility = View.INVISIBLE
            holder.userImageView.setImageDrawable(null)
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun setConvo(data: WickrConvo) {
        items.clear()
        convo = data
        for (u in data.usersList) {
            var isModerator = false
            if (data.moderatorsCount > 0){
                if (data.moderatorsList.contains(u.id)) {
                    isModerator = true
                }
            }
            items.add(UserModeratorWrapper(u, isModerator))
        }

        notifyDataSetChanged()
    }

    fun addOrUpdateItems(data: List<WickrUser>) {
        for (user in data) {
            val index = items.indexOfFirst { it.user.id == user.id }
            if (index >= 0) {
                items.removeAt(index)
                if (!user.isSelf) {
                    items.add(index, UserModeratorWrapper(user, false))
                }
            } else {
                items.add(items.size, UserModeratorWrapper(user, false))
            }
        }
        notifyDataSetChanged()
    }
}

