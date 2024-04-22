package com.atakmap.android.wickr.wear.data

import com.google.android.gms.wearable.Node

interface MessageRepo {

    suspend fun sendMessage(message: String, node: Node, messagePath: String): Boolean
}
