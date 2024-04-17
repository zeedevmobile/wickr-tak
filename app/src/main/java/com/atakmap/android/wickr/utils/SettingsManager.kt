package com.atakmap.android.wickr.utils

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.util.Base64
import com.atakmap.android.maps.MapView
import com.atakmap.android.wickr.WickrAPIReceiver
import com.atakmap.android.wickr.WickrAPIUnpairedEvent
import com.atakmap.android.wickr.WickrMapComponent

class SettingsManager constructor(private val pluginContext: Context) {
    companion object {
        private const val PREFS_FILE_NAME = "WickrApiPrefs"
        private const val PREFS_KEY_ENCRYPTION_KEY = "apiEncryptionKey"
        private const val PREFS_MAIN_FRAG_POS = "mainFragmentPos"
        private const val PREFS_PKG = "WickrPkg"
    }

    private val prefs: SharedPreferences by lazy {
        MapView.getMapView().context.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)
    }

    fun isPaired(): Boolean = prefs.contains(PREFS_KEY_ENCRYPTION_KEY)

    fun saveKey(key: ByteArray): Boolean = prefs.edit()
        .putString(PREFS_KEY_ENCRYPTION_KEY, key.toBase64String())
        .commit()

    fun saveMainFragPos(pos: Int): Boolean = prefs.edit().putInt(PREFS_MAIN_FRAG_POS, pos).commit()

    fun getMainFragPos(): Int = prefs.getInt(PREFS_MAIN_FRAG_POS, 0)

    fun savePref(id: String, value: String) = prefs.edit().putString(id, value).commit()

    fun getPref(id: String): String? = prefs.getString(id, "")

    fun getKey(): ByteArray? {
        if (!isPaired()) return null
        return prefs.getString(PREFS_KEY_ENCRYPTION_KEY, null)!!.fromBase64String()
    }

    fun clear() {
        prefs.edit().clear().commit()
    }

    private fun ByteArray.toBase64String(): String {
        return Base64.encodeToString(this, Base64.DEFAULT)
    }

    private fun String.fromBase64String(): ByteArray {
        return Base64.decode(this, Base64.DEFAULT)
    }
}