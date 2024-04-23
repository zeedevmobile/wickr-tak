package com.atakmap.android.wickr.plugin.domain

import com.atakmap.android.wickr.plugin.data.CapabilityRepo
import com.google.android.gms.wearable.Node

class GetCapableNodes(private val capabilityRepo: CapabilityRepo) {

    companion object {
        private const val CAPABILITY = "wear"
    }

    suspend operator fun invoke(): Set<Node> {
        return capabilityRepo.getNodesForCapability(
            CAPABILITY,
            capabilityRepo.getCapabilitiesForReachableNodes()
        )
    }
}
