package com.atakmap.android.wickr.plugin.data

import android.util.Log
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Node
import kotlinx.coroutines.tasks.await
import java.nio.charset.Charset

class DefaultMessageRepo(private val messageClient: MessageClient) : MessageRepo {

    companion object {
        private const val TAG = "DefaultMessageRepo"
    }

    override suspend fun sendMessage(message: String, node: Node, messagePath: String): Boolean {
        val nodeId = node.id
        var result = false
        nodeId.also { id ->
            messageClient.sendMessage(
                id, messagePath, message.toByteArray(charset = Charset.defaultCharset())
            ).apply {
                addOnSuccessListener {
                    Log.i(TAG, "sendMessage OnSuccessListener")
                    result = true
                }
                addOnFailureListener {
                    Log.i(TAG, "sendMessage OnFailureListener")
                    result = false
                }
            }.await()
            Log.i(TAG, "result: $result")
            return result
        }
    }
}
