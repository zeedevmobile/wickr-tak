package com.atakmap.android.wickr.ui

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.atakmap.android.ipc.AtakBroadcast
import com.atakmap.android.maps.MapView
import com.atakmap.android.wickr.*
import com.atakmap.android.wickr.plugin.R
import com.atakmap.android.wickr.ui.adapters.ContactClickListener
import com.atakmap.android.wickr.ui.adapters.WickrUserAdapter
import com.atakmap.android.wickr.ui.pager.WickrContactPagingSource
import com.atakmap.android.wickr.utils.SettingsManager
import com.wickr.android.api.WickrAPI
import com.wickr.android.api.WickrAPIObjects
import com.wickr.android.api.WickrAPIRequests
import kotlinx.android.synthetic.main.contact_list.*
import kotlinx.android.synthetic.main.dialog_create_convo.view.*
import kotlinx.android.synthetic.main.dialog_loading.view.*
import kotlinx.android.synthetic.main.fragment_contacts.*
import kotlinx.coroutines.flow.*
import org.greenrobot.eventbus.Subscribe

class ContactsListFragment private constructor (private val pluginContext: Context, private val showSearchAndCreate: Boolean, private val newConvo: Boolean, private val convo: WickrAPIObjects.WickrConvo?) : Fragment(), ContactClickListener, IBackButtonHandler {

    companion object {
        const val PAGE_SIZE = 20

        const val EXTRA_MEMBERS = "members"
        const val EXTRA_MODERATORS = "moderators"
        const val EXTRA_MODERATORS_MODE = "moderatorsMode"
        const val EXTRA_IS_ROOM = "isRoom"
        const val EXTRA_VGROUPID = "vGroupID"

        const val TAG = "ContactsListFragment"
        private var instance: ContactsListFragment? = null

        fun newInstance(pluginContext: Context): ContactsListFragment? {
            if (instance == null) {
                ContactsListFragment.instance = ContactsListFragment(pluginContext, true, true, null)
            }
            return instance
        }

        fun newInstance(pluginContext: Context, showSearchAndCreate: Boolean = true, newConvo: Boolean = true, convo: WickrAPIObjects.WickrConvo? = null): ContactsListFragment? {
            return ContactsListFragment(pluginContext, showSearchAndCreate, newConvo, convo)
        }

        @JvmStatic
        fun changeRoomMembersModInstance(
            vGroupID: String,
            memberIDs: List<String>,
            moderatorIDs: List<String>,
            moderatorsMode: Boolean,
            isRoom: Boolean,
            pluginContext: Context
        ): ContactsListFragment {
            return ContactsListFragment(pluginContext, true, true, null).apply {
                this.arguments = bundleOf(
                    EXTRA_MEMBERS to memberIDs,
                    EXTRA_MODERATORS to moderatorIDs,
                    EXTRA_VGROUPID to vGroupID,
                    EXTRA_IS_ROOM to isRoom,
                    EXTRA_MODERATORS_MODE to moderatorsMode
                )
            }
        }
    }

    private lateinit var adapter: WickrUserAdapter
    private lateinit var pagingSource: WickrContactPagingSource
    private val queryFlow = MutableStateFlow("")
    private var waitingDialog: Dialog? = null

    private var vGroupID: String = ""
    private var convoMembers: ArrayList<String> = ArrayList()
    private var convoModerators: ArrayList<String> = ArrayList()
    private var moderatorsMode: Boolean = false
    private var isRoom: Boolean = false
    private val settingsManager: SettingsManager = SettingsManager(pluginContext)

    private val requests: Requests = Requests(pluginContext)

    // used to control paging for additional contacts
    private var isLoading = true
    private var lastVisibleItemPosition = 0
    private var visibleItemCount: Int = 0
    private var visibleItemThreshold: Int = 5
    private var totalItemCount: Int = 0
    private var previousTotalItemCount: Int = 0

    override fun onBackButtonPressed(): Boolean {
        if (!newConvo) return false
        resetSearch()
        return true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return LayoutInflater.from(pluginContext).inflate(R.layout.fragment_contacts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        contactSearch.visibility = if (showSearchAndCreate) View.VISIBLE else View.GONE
        createConvoBtn.visibility = if (showSearchAndCreate) View.VISIBLE else View.GONE
        cancelAddUsersBtn.visibility = if (!showSearchAndCreate) View.VISIBLE else View.GONE

        if (!settingsManager.isPaired()) {
            WickrMapComponent.EVENTBUS.post(WickrAPIUnpairedEvent())
        }

        if (arguments != null) {
            convoMembers = requireArguments().getStringArrayList(EXTRA_MEMBERS) ?: ArrayList()
            convoModerators = requireArguments().getStringArrayList(EXTRA_MODERATORS) ?: ArrayList()
            vGroupID = arguments?.getString(EXTRA_VGROUPID) ?: ""
            moderatorsMode = arguments?.getBoolean(EXTRA_MODERATORS_MODE) ?: false
            isRoom = arguments?.getBoolean(EXTRA_IS_ROOM) ?: false
        }

        moderatorsMode = !showSearchAndCreate
        if (convo != null) {
            vGroupID = convo.id
        }

        adapter = WickrUserAdapter(this, pluginContext)

        pagingSource = WickrContactPagingSource(pluginContext, "")


        contactSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (s?.isEmpty() == true) {
                    requestNewPage(currentOffset = 0)
                }
                if (s != null && s.length >= 2) {
                    requestNewPage(s.toString(), 0)
                }
            }
        })
        memberList.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = this@ContactsListFragment.adapter.apply {
                if (moderatorsMode && convoModerators.isNotEmpty()) {
                    setCheckedUsers(convoModerators)
                } else if (moderatorsMode.not() && convoMembers.isNotEmpty()) {
                    setCheckedUsers(convoMembers)
                }
                if (convo != null) {
                    addAllExclusions(convo.usersList.map { it.id })
                }
            }
            setHasFixedSize(true)
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    visibleItemCount = (layoutManager as LinearLayoutManager).childCount
                    totalItemCount = (layoutManager as LinearLayoutManager).itemCount
                    lastVisibleItemPosition = (layoutManager as LinearLayoutManager).findLastVisibleItemPosition()

                    if (isLoading && totalItemCount > previousTotalItemCount) {
                        isLoading = false
                        previousTotalItemCount = totalItemCount
                    }

                    if (!isLoading && (lastVisibleItemPosition + visibleItemThreshold > totalItemCount)) {
                        isLoading = true
                        requestNewPage(currentOffset = adapter?.itemCount ?: 0)
                    }
                }
            })
        }
        createConvoBtn.isEnabled = false
        createConvoBtn.setOnClickListener {
            when {
                vGroupID.isEmpty() -> WickrMapComponent.EVENTBUS.post(CreateConvoFragmentEvent(adapter.getCheckedUsers()))
            }

        }
        addUsersBtn.setOnClickListener(View.OnClickListener {
            val selectedList = adapter.getCheckedUsers();
            if (selectedList.isNotEmpty()) {
                requests.addUsersToConvo(selectedList, convo)
                showWaitingDialog()
            }
        })

        cancelAddUsersBtn.setOnClickListener {
            WickrMapComponent.EVENTBUS.post(PopFragmentEvent())
        }

        getView()?.setOnKeyListener(object : View.OnKeyListener {
            override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    resetSearch()
                    return true
                }
                return false
            }
        })
    }

    private fun showWaitingDialog() {
        val layoutInflater = LayoutInflater.from(pluginContext)
        val rootView = layoutInflater.inflate(R.layout.dialog_loading, null, false)
        rootView.loadingTextView.setText(R.string.waiting_dialog_text)
        waitingDialog = AlertDialog.Builder(MapView.getMapView().context)
            .setView(rootView)
            .setCancelable(true)
            .create().also { it.show() }
    }

    private fun hideWaitingDialog() {
        waitingDialog?.dismiss()
        waitingDialog = null
    }

    override fun onStart() {
        super.onStart()
        WickrMapComponent.EVENTBUS.register(this)
        WickrMapComponent.EVENTBUS.register(adapter)
        resetSearch(true)
        refreshContactFromWickr(queryFlow.value)
    }

    override fun onStop() {
        super.onStop()
        WickrMapComponent.EVENTBUS.unregister(this)
        WickrMapComponent.EVENTBUS.unregister(adapter)
        if (::pagingSource.isInitialized) {
            WickrMapComponent.EVENTBUS.unregister(pagingSource)
        }
        previousTotalItemCount = 0
        totalItemCount = 0
        visibleItemCount = 0
    }

    private fun refreshContactFromWickr(query: String) {
        if (::pagingSource.isInitialized) {
            WickrMapComponent.EVENTBUS.unregister(pagingSource)
        }

        pagingSource = WickrContactPagingSource(pluginContext, query)
        WickrMapComponent.EVENTBUS.register(pagingSource)

        val pager = Pager(PagingConfig(PAGE_SIZE, initialLoadSize = PAGE_SIZE)) { pagingSource }.flow
    }

    override fun onContactClick(contact: WickrAPIObjects.WickrUser) {
        if (newConvo) {
            showWaitingDialog()
            resetSearch(false)
            requests.createConvo(contact.id)
        }
    }

    override fun showFab() {
        if(newConvo)
            createConvoBtn.isEnabled = true
        else {
            addUsersBtn.visibility = View.VISIBLE
            cancelAddUsersBtn.visibility = View.VISIBLE
            if(adapter.getCheckedUsers().size == 1)
                addUsersBtn.setText("Add 1 User");
            else
                addUsersBtn.setText("Add " + adapter.getCheckedUsers().size + " Users");
        }
    }

    override fun hideFab() {
        if(newConvo)
            createConvoBtn.isEnabled = false
        else
            addUsersBtn.setVisibility(View.GONE);
    }

    @Subscribe
    internal fun onWickrAPIEvent(event: Any) {
        when (event) {
            is WickrConvoCreatedEvent -> {
                adapter.resetCheckedUsers()
                hideFab()
                hideWaitingDialog()

                if (event.error != null) {
                    Toast.makeText(
                        MapView.getMapView().context,
                        "Error creating convo: ${event.error.name}",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    WickrMapComponent.EVENTBUS.post(MessageFragmentEvent(event.convo!!))
                }
            }
            is WickrConvoEditEvent -> {
                hideWaitingDialog()
                if (convo != null) WickrMapComponent.EVENTBUS.post(PopFragmentEvent())
            }
            is WickrUserAvatarUpdateEvent -> {

                adapter.notifyItemRangeChanged(0, adapter.itemCount, event)
            }
            is WickrContactListEvent -> {
                contactLoading.visibility = View.GONE
                adapter.addOrUpdateItems(event.contacts)
            }
            is WickrInvalidRequestEvent -> {
                Toast.makeText(MapView.getMapView().context, "Error trying to create the request: ${event.error.name}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requestNewPage(search: String = "", currentOffset: Int) {
        val key = settingsManager.getKey()
        if (search == "")
            contactLoading.visibility = View.VISIBLE
        if (key == null) {
            //adapter.clear()
            isLoading = true
            previousTotalItemCount = 0
            return
        }

        Log.d(TAG, "Requesting page of contacts with count $PAGE_SIZE and offset ${adapter.itemCount}")
        var getContactsRequest: WickrAPIRequests.GetContactsRequest
        var id: String = ""
        if (search.isEmpty()) {
             getContactsRequest = WickrAPIRequests.GetContactsRequest.newBuilder()
                .setCount(PAGE_SIZE)
                .setOffset(currentOffset)
                .build()
        } else {
            id = WickrUserAdapter.SEARCH_ID
            getContactsRequest = WickrAPIRequests.GetContactsRequest.newBuilder()
                .setQuery(search)
                .setUseDirectory(true)
                .setCount(PAGE_SIZE)
                .setOffset(currentOffset)
                .build()
        }


        val requestIntent = WickrAPI.createRequestIntent(Requests.PACKAGE_NAME, getContactsRequest, key, id)
        AtakBroadcast.getInstance().sendSystemBroadcast(requestIntent)
    }

    fun resetSearch(getContacts: Boolean = true) {
        contactSearch.setText("")
        if(getContacts) requestNewPage(currentOffset = 0)
    }
}