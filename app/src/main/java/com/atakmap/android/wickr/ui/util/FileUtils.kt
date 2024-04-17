package com.atakmap.android.wickr.ui.util

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import com.atakmap.android.maps.MapView
import com.atakmap.android.wickr.ui.ConvoFragment
import java.io.File

class FileUtils {

    fun externalOpen(uri: Uri, mimeType: String) {
        Log.d(ConvoFragment.TAG, "Attempting to open file $uri")


        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val packageManager = MapView.getMapView().context.packageManager
        if (intent.resolveActivity(packageManager) != null) {
            packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).forEach {
                MapView.getMapView().context.grantUriPermission(it.activityInfo.packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            MapView.getMapView().context.startActivity(intent)
        } else Log.e(ConvoFragment.TAG, "No application installed to handle mimeType: ${mimeType}")
    }
}