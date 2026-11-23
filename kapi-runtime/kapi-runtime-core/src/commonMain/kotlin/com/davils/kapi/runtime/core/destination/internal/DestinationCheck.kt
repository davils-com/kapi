/*
 * Copyright 2026 Davils
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.davils.kapi.runtime.core.destination.internal

import com.davils.kapi.runtime.core.destination.Destination
import com.davils.kapi.runtime.core.destination.DestinationDecision
import com.davils.kapi.runtime.core.destination.DestinationPolicy
import com.davils.kapi.runtime.core.destination.DestinationRefusal
import com.davils.kapi.runtime.core.destination.HostResolution
import com.davils.kapi.runtime.core.destination.IpAddress
import com.davils.kore.core.failure.attempted

internal class DestinationCheck(private val policy: DestinationPolicy) {
    suspend fun decide(destination: Destination): DestinationDecision {
        val early = screen(destination)
        if (early != null) return early
        val name = Hosts.normalized(destination.host)
        return when (val resolution = resolve(name)) {
            HostResolution.Skipped -> DestinationDecision.Allowed()
            is HostResolution.Failed -> refused(DestinationRefusal.Unresolved)
            is HostResolution.Resolved -> addresses(resolution.addresses)
        }
    }

    fun decideResolved(destination: Destination, addresses: List<IpAddress>): DestinationDecision =
        screen(destination) ?: addresses(addresses)

    fun addresses(addresses: List<IpAddress>): DestinationDecision {
        if (addresses.isEmpty()) return refused(DestinationRefusal.Unresolved)
        return when (addresses.all(IpAddress::isPublic)) {
            true -> DestinationDecision.Allowed(addresses)
            false -> refused(DestinationRefusal.ResolvesToPrivate)
        }
    }

    private fun screen(destination: Destination): DestinationDecision? {
        if (destination.scheme.lowercase() !in policy.schemes) return refused(DestinationRefusal.Scheme)
        return when (val host = Hosts.read(destination.host)) {
            Host.Invalid -> refused(DestinationRefusal.Host)
            is Host.Address -> trusted(host.address.toString()) ?: address(host.address)
            is Host.Name -> trusted(host.name) ?: name(host.name)
        }
    }

    private suspend fun resolve(name: String): HostResolution {
        val resolution = attempted { policy.resolver.resolve(name) }
        return resolution.getOrNull() ?: HostResolution.Failed("The resolver failed")
    }

    private fun trusted(host: String): DestinationDecision? {
        if (policy.hosts.isNotEmpty()) return listed(host)
        return DestinationDecision.Allowed().takeIf { policy.arePrivateNetworksAllowed }
    }

    private fun listed(host: String): DestinationDecision {
        val isListed = policy.hosts.any { pattern -> Hosts.matches(pattern, host) }
        if (!isListed) return refused(DestinationRefusal.HostNotAllowed)
        return DestinationDecision.Allowed()
    }

    private fun address(address: IpAddress): DestinationDecision {
        if (!address.isPublic) return refused(DestinationRefusal.NotPublic)
        return DestinationDecision.Allowed(listOf(address))
    }

    private fun name(name: String): DestinationDecision? {
        if (!Hosts.isPublicName(name)) return refused(DestinationRefusal.NotPublic)
        return null
    }

    private fun refused(refusal: DestinationRefusal): DestinationDecision = DestinationDecision.Refused(refusal)
}
