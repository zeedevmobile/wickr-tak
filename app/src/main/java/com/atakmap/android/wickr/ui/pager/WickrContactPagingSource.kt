package com.atakmap.android.wickr.ui.pager

import android.content.Context
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.atakmap.android.ipc.AtakBroadcast
import com.atakmap.android.wickr.Requests
import com.atakmap.android.wickr.WickrContactListEvent
import com.atakmap.android.wickr.utils.SettingsManager
import com.wickr.android.api.WickrAPI
import com.wickr.android.api.WickrAPIObjects.WickrUser
import com.wickr.android.api.WickrAPIRequests.GetContactsRequest
import kotlinx.coroutines.CompletableDeferred
import org.greenrobot.eventbus.Subscribe

class WickrContactPagingSource(
    private val pluginContext: Context,
    private val query: String
) : PagingSource<Int, WickrUser>() {

    private var requestWaitingDeferred: CompletableDeferred<List<WickrUser>>? = null
    private val settingsManager: SettingsManager = SettingsManager(pluginContext)

    override fun getRefreshKey(state: PagingState<Int, WickrUser>): Int? {
        return null
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, WickrUser> {
        val key = settingsManager.getKey()
            ?: return LoadResult.Error(RuntimeException("No Api Key Found!"))

        val offset = params.key ?: 0
        requestWaitingDeferred = CompletableDeferred()
        val getContactsRequest = GetContactsRequest.newBuilder()
            .setQuery(query)
            .setCount(params.loadSize)
            .setOffset(offset)
            .build()
        val identifier = "Contacts-$offset"
        val requestIntent = WickrAPI.createRequestIntent(Requests.PACKAGE_NAME, getContactsRequest, key, identifier)
        AtakBroadcast.getInstance().sendSystemBroadcast(requestIntent)

        val result = requestWaitingDeferred?.await() ?: emptyList()
        requestWaitingDeferred = null
        val prevPage = if (offset > 0) (offset - params.loadSize).coerceAtLeast(0) else null
        val nextPage = if (result.isNotEmpty()) offset + result.size else null
        return LoadResult.Page(result, prevPage, nextPage)
    }

    @Subscribe
    internal fun onAppEvent(event: WickrContactListEvent) {
        requestWaitingDeferred?.complete(event.contacts)
    }
}
