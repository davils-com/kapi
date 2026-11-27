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

import com.davils.konnect.internetprotocol.address.AddressCategory
import com.davils.konnect.internetprotocol.address.Ipv4Address
import com.davils.konnect.internetprotocol.address.Ipv6Address
import com.davils.konnect.internetprotocol.network.IpNetwork
import com.davils.konnect.internetprotocol.address.IpAddress as KonnectAddress

private const val IPV4_BYTES: Int = 4
private const val IPV6_BYTES: Int = 16
private const val SIX_TO_FOUR_OFFSET: Int = 2
private const val TAIL_OFFSET: Int = 12

private val mapped: IpNetwork = IpNetwork.parse("::ffff:0:0/96")
private val translated: IpNetwork = IpNetwork.parse("64:ff9b::/96")
private val sixToFour: IpNetwork = IpNetwork.parse("2002::/16")

internal object Addresses {
    fun isPublic(address: ByteArray): Boolean {
        val embedded = embedded(address)
        if (embedded != null) return isPublic(embedded)
        val read = konnectOf(address)
        return read != null && read.isPublic && read.category == AddressCategory.Public
    }

    fun konnectOf(address: ByteArray): KonnectAddress? {
        if (address.size == IPV4_BYTES) return Ipv4Address.fromByteArray(address)
        return address.takeIf { bytes -> bytes.size == IPV6_BYTES }?.let(Ipv6Address::fromByteArray)
    }

    private fun embedded(address: ByteArray): ByteArray? {
        val read = konnectOf(address)?.takeIf { found -> address.size == IPV6_BYTES && found is Ipv6Address }
            ?: return null
        val offset = TAIL_OFFSET.takeIf { read in mapped || read in translated }
            ?: SIX_TO_FOUR_OFFSET.takeIf { read in sixToFour }
        return offset?.let { start -> address.copyOfRange(fromIndex = start, toIndex = start + IPV4_BYTES) }
    }
}
