package com.atakmap.android.wickr.ui

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.collection.LruCache
import com.atakmap.android.ipc.AtakBroadcast
import com.atakmap.android.wickr.Requests
import com.atakmap.android.wickr.utils.SettingsManager
import com.wickr.android.api.WickrAPI
import com.wickr.android.api.WickrAPIRequests
import java.util.concurrent.atomic.AtomicBoolean


open class UserAvatarCache private constructor(
    private val context: Context,
    private val apiManager: SettingsManager
) : LruCache<String, Bitmap>(MAX_CACHE_SIZE) {

    companion object {
        private var instance : UserAvatarCache? = null
        private val initialized = AtomicBoolean()

        fun getInstance(pluginContext: Context) : UserAvatarCache? {
            if(instance == null) {
                instance = UserAvatarCache(pluginContext, SettingsManager(pluginContext))
            }
            return instance
        }
        private const val MAX_MEMORY_PERCENTAGE = 0.2
        private const val TAG = "UserAvatar"
        private val MAX_CACHE_SIZE: Int
            get() {
                val maxMemory = Runtime.getRuntime().maxMemory() / 1024L;
                return (maxMemory.toDouble() * MAX_MEMORY_PERCENTAGE).toInt().also {
                    Log.d(TAG,"Instantiating cache using ${it}KB max size")
                }
            }
    }

    private val usersWithoutAvatars = ArrayList<String>()

    override fun sizeOf(key: String, value: Bitmap): Int {
        return value.byteCount / 1024;
    }

    override fun create(key: String): Bitmap? {
        val value = super.create(key)
        if (value == null) {
            if (key in usersWithoutAvatars) return value
            val encryptionKey = apiManager.getKey()
            if (encryptionKey == null) return value
            Log.d(TAG,"Requesting new avatar from the API for $key")
            val identifier = "UserAvatar-$key"
            val userAvatarRequest = WickrAPIRequests.GetUserAvatarRequest.newBuilder()
                .setUserID(key)
                .build()
            val intent = WickrAPI.createRequestIntent(Requests.PACKAGE_NAME, userAvatarRequest, encryptionKey, identifier)
            AtakBroadcast.getInstance().sendSystemBroadcast(intent)
        }
        return value
    }

    fun setHasNoAvatar(userID: String) {
        if (userID !in usersWithoutAvatars) {
            usersWithoutAvatars.add(userID)
        }
    }
}
