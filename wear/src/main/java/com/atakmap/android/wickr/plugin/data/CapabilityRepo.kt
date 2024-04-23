package com.atakmap.android.wickr.plugin.data

import com.google.android.gms.wearable.Node

interface CapabilityRepo {

    suspend fun getNodesForCapability(
        capability: String,
        allCapabilities: Map<Node, Set<String>>
    ): Set<Node>

    suspend fun getCapabilitiesForReachableNodes(): Map<Node, Set<String>>
}
