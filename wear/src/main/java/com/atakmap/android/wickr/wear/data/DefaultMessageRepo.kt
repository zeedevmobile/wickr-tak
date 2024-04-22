package com.atakmap.android.wickr.wear.data

import android.util.Log
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Node
import kotlinx.coroutines.tasks.await
import java.nio.charset.Charset

class DefaultMessageRepo(private val messageClient: MessageClient) : MessageRepo {

    override suspend fun sendMessage(message: String, node: Node, messagePath: String): Boolean {
        val nodeId = node.id
        var result = false
        nodeId.also { id ->
            messageClient.sendMessage(
                    id, messagePath, message.toByteArray(charset = Charset.defaultCharset())
                ).apply {
                    addOnSuccessListener {
                        Log.i("XXXXX", "sendMessage OnSuccessListener")
                        result = true
                    }
                    addOnFailureListener {
                        Log.i("XXXXX", "sendMessage OnFailureListener")
                        result = false
                    }
                }.await()
            Log.i("XXXXX", "result: $result")
            return result
        }
    }
}
