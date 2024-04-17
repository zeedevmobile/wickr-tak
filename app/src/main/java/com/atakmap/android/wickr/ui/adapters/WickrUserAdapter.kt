package com.atakmap.android.wickr.ui.adapters

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.atakmap.android.wickr.Requests
import com.atakmap.android.wickr.WickrContactSearchListEvent
import com.atakmap.android.wickr.WickrUserAvatarUpdateEvent
import com.atakmap.android.wickr.initials
import com.atakmap.android.wickr.plugin.R
import com.atakmap.android.wickr.ui.UserAvatarCache
import com.atakmap.android.wickr.utils.SettingsManager
import com.wickr.android.api.WickrAPIObjects.*
import org.greenrobot.eventbus.Subscribe


class WickrUserViewHolder(layout: View) : RecyclerView.ViewHolder(layout) {
    val userImageView: ImageView = itemView.findViewById(R.id.userImageView)
    val userImageText: TextView = itemView.findViewById(R.id.userImageText)
    val fullNameTextView: TextView = itemView.findViewById(R.id.fullNameTextView)
    val userNameTextView: TextView = itemView.findViewById(R.id.userNameTextView)
    val userCheckBox: CheckBox = itemView.findViewById(R.id.userCheckBox)
    val namesLayout: LinearLayout = itemView.findViewById(R.id.namesLayout)
}

class WickrUserDiffCallback : DiffUtil.ItemCallback<WickrUser>() {
    override fun areItemsTheSame(oldItem: WickrUser, newItem: WickrUser): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: WickrUser, newItem: WickrUser): Boolean {
        return false // TODO support avatars then re-enable this
    }
}

interface ContactClickListener {
    fun onContactClick(contact: WickrUser)
    fun showFab()
    fun hideFab()
}

class WickrUserAdapter(private val contactClickListener: ContactClickListener?, private val pluginContext: Context) :
    RecyclerView.Adapter<WickrUserViewHolder>() {

    private var checkedUserList: ArrayList<String> = arrayListOf()
    private var settingsManager: SettingsManager
    private var userAvatarCache: UserAvatarCache
    private var items: ArrayList<WickrUser> = arrayListOf()
    var filtered: ArrayList<WickrUser> = arrayListOf()
    var backup: ArrayList<WickrUser> = arrayListOf()
    val requests = Requests(pluginContext)
    private var exclusionList: ArrayList<String> = arrayListOf()

    init {
        settingsManager = SettingsManager(pluginContext)
        userAvatarCache = UserAvatarCache.getInstance(pluginContext)!!
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WickrUserViewHolder {
        val layout = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_wickr_user, parent, false)
        return WickrUserViewHolder(layout)
    }

    override fun onBindViewHolder(holder: WickrUserViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty()) {
            val userAvatarEvent = payloads[0] as? WickrUserAvatarUpdateEvent
            if (userAvatarEvent != null) {
                val item = items[position]
                if (item != null && item.id == userAvatarEvent.userID) {
                    refreshUserAvatar(holder, item)
                }
                return
            }
        }
        super.onBindViewHolder(holder, position, payloads)
    }

    override fun onBindViewHolder(holder: WickrUserViewHolder, position: Int) {
        val item = items[position]

        refreshUserAvatar(holder, item)
        holder.fullNameTextView.text = item.fullName.let {
            if (!it.isNullOrBlank()) {
                if (item.isBot) {
                    "$it [BOT]"
                } else it
            } else item.username
        }
        holder.userNameTextView.text = item.username

        //FIXME
        //holder.userNameTextView.isVisible = holder.userNameTextView.text != holder.fullNameTextView.text

        holder.userCheckBox.isChecked = checkedUserList.contains(item.id)

        holder.namesLayout.setOnClickListener {
            contactClickListener?.onContactClick(item)
        }

        holder.userCheckBox.setOnClickListener {
            onContactSelectedForConvo(item)
        }
    }

    private fun refreshUserAvatar(holder: WickrUserViewHolder, item: WickrUser) {
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

    private fun onContactSelectedForConvo(contact: WickrUser) {
        if (checkedUserList.contains(contact.id).not()) {
            checkedUserList.add(contact.id)
        } else if (checkedUserList.contains(contact.id)) {
            checkedUserList.remove(contact.id)
        }

        if (checkedUserList.isEmpty()) {
            contactClickListener?.hideFab()
        } else {
            contactClickListener?.showFab()
        }
    }

    fun getCheckedUsers(): List<String> {
        return checkedUserList
    }

    fun setCheckedUsers(checkedUsers: ArrayList<String>) {
        checkedUserList = checkedUsers
    }

    fun resetCheckedUsers() {
        checkedUserList.clear()
    }

    fun addAllExclusions(exclusions: List<String>) {
        exclusionList.addAll(exclusions)
        setItems(items)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun setItems(data: List<WickrUser>) {
        items.clear()
        var included: ArrayList<WickrUser> = arrayListOf()
        for (u in data) {
            if (!exclusionList.contains(u.id)) {
                included.add(u)
            }
        }
        items.addAll(included)
        notifyDataSetChanged()
    }

    fun addOrUpdateItems(data: List<WickrUser>) {
        for (user in data) {
            if (!exclusionList.contains(user.id)) {
                val index = items.indexOfFirst { it.id == user.id }
                if (index >= 0) {
                    items.removeAt(index)
                    if (!user.isSelf) {
                        items.add(index, user)
                    }
                } else {
                    items.add(items.size, user)
                }
            }
        }
        notifyDataSetChanged()
    }

    @Subscribe
    internal fun onWickrAPIEvent(event: Any) {
        when(event) {
            is WickrContactSearchListEvent -> {
                Log.d(TAG, "Search retured ${event.contacts.size} contacts")
                backup.clear()
                backup.addAll(items)
                setItems(event.contacts)
            }
        }
    }

    companion object {
        val SEARCH_ID = "contact-search"
        private val TAG = WickrUserAdapter.javaClass.simpleName
    }
}

