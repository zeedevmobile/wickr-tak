package com.atakmap.android.wickr.ui

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.atakmap.android.ipc.AtakBroadcast
import com.atakmap.android.maps.MapView
import com.atakmap.android.wickr.*
import com.atakmap.android.wickr.plugin.R
import com.atakmap.android.wickr.ui.adapters.WickrRoomMembershipAdapter
import com.atakmap.android.wickr.utils.SettingsManager
import com.wickr.android.api.WickrAPI
import com.wickr.android.api.WickrAPIObjects
import com.wickr.android.api.WickrAPIRequests
import kotlinx.android.synthetic.main.convo_management.*
import org.greenrobot.eventbus.Subscribe

class MembershipFragment(private val pluginContext: Context, private val convo: WickrAPIObjects.WickrConvo?) : Fragment() {

    private lateinit var adapter: WickrRoomMembershipAdapter
    private val settingsManager = SettingsManager(pluginContext)
    private var justLeave = true
    private val requests = Requests(pluginContext)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return LayoutInflater.from(pluginContext).inflate(R.layout.convo_management, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (convo != null) requests.convo(convo.id, 1, 0)

        backRoomManagement.setOnClickListener {
            WickrMapComponent.EVENTBUS.post(PopFragmentEvent())
        }


        var selfUser: WickrAPIObjects.WickrUser? = null
        for (u in convo!!.usersList) {
            if (u.isSelf) {
                selfUser = u
                break
            }
        }
        for (id in convo!!.moderatorsList) {
            if (selfUser?.id == id) {
                justLeave = false
            }
        }
        leaveOrDeleteRoomBtn.text = if (justLeave) pluginContext.getString(R.string.leave_room) else pluginContext.getString(R.string.delete_room)
        leaveOrDeleteRoomBtn.setOnClickListener {

            showDeleteLeaveConvoDialog(justLeave)
        }

        adapter = WickrRoomMembershipAdapter(pluginContext, !justLeave)
        adapter.setConvo(convo!!)
        titleText.text = convo.title ?: "Unknown"
        descText.text = convo.description ?: ""

        memberList.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = this@MembershipFragment.adapter
            setHasFixedSize(true)
        }

        addMembersBtn.isEnabled = justLeave.not()
        addMembersBtn.setOnClickListener{
            WickrMapComponent.EVENTBUS.post(SelectContactsEvent(false, convo))
        }
    }

    override fun onStart() {
        super.onStart()
        WickrMapComponent.EVENTBUS.register(this)
        requests.subscribeToUpdates(true, convo?.id)
    }

    override fun onStop() {
        super.onStop()
        WickrMapComponent.EVENTBUS.unregister(this)
        requests.subscribeToUpdates(false, convo?.id)
    }

    @Subscribe
    internal fun onWickrApiEvent(event: Any) {
        when(event) {
            is WickrConvoEditEvent -> {
                adapter.setConvo(event.convo)
            }
            is WickrConvoUpdateEvent -> {
                adapter.setConvo(event.convo)
            }
        }
    }

    private fun showDeleteLeaveConvoDialog(isLeave: Boolean) {
        val alertDialog = AlertDialog.Builder(MapView.getMapView().context)
            .setTitle(if (isLeave) pluginContext.getString(R.string.leave_dialog_title) else pluginContext.getString(R.string.delete_dialog_title))
            .setMessage(if (isLeave) pluginContext.getString(R.string.leave_dialog_message) else pluginContext.getString(R.string.delete_dialog_message))
            .setNegativeButton(pluginContext.getString(R.string.cancel)) { dialog, _ -> dialog.dismiss() }
            .setPositiveButton(pluginContext.getString(R.string.submit)) { dialog, _ ->
                sendConvoDeleteLeaveRequest(isLeave)
                dialog.dismiss()
            }

        alertDialog.show()
    }

    private fun sendConvoDeleteLeaveRequest(isLeave: Boolean) {
        val convo = convo!!
        val key = settingsManager.getKey() ?: return

        val wickrConvoEdit = if (isLeave) {
            WickrAPIObjects.WickrConvoEdit.newBuilder().apply {
                setLeave(true)
            }
        } else {
            WickrAPIObjects.WickrConvoEdit.newBuilder().apply {
                setDelete(true)
            }
        }

        val editConvoRequest = WickrAPIRequests.EditConvoRequest.newBuilder()
            .setConvoID(convo.id)
            .setChanges(wickrConvoEdit)
            .build()
        val requestIntent = WickrAPI.createRequestIntent(Requests.PACKAGE_NAME, editConvoRequest, key)
        AtakBroadcast.getInstance().sendSystemBroadcast(requestIntent)
        WickrMapComponent.EVENTBUS.post(PopFragmentEvent())
    }
}