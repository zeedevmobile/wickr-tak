package com.atakmap.android.wickr.plugin.data

import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Node
import kotlinx.coroutines.tasks.await

class DefaultCapabilityRepo(private val capabilityClient: CapabilityClient) : CapabilityRepo {

    override suspend fun getNodesForCapability(
        capability: String, allCapabilities: Map<Node, Set<String>>
    ): Set<Node> {
        return allCapabilities.filterValues { capability in it }.keys
    }

    override suspend fun getCapabilitiesForReachableNodes(): Map<Node, Set<String>> {

        val allCapabilities =
            capabilityClient.getAllCapabilities(CapabilityClient.FILTER_REACHABLE).await()

        return allCapabilities.flatMap { (capability, capabilityInfo) ->
            capabilityInfo.nodes.map {
                it to capability
            }
        }.groupBy(keySelector = { it.first }, valueTransform = { it.second })
            .mapValues { it.value.toSet() }
    }
}
