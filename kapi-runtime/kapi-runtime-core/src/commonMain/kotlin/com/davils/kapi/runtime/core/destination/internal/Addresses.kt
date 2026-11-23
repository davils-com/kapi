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

private const val IPV4_BYTES: Int = 4

private const val SIX_TO_FOUR_OFFSET: Int = 2

private const val TAIL_OFFSET: Int = 12

private val notPublicV4: List<AddressBlock> = listOf(
    "0.0.0.0/8",
    "10.0.0.0/8",
    "100.64.0.0/10",
    "127.0.0.0/8",
    "169.254.0.0/16",
    "172.16.0.0/12",
    "192.0.0.0/24",
    "192.0.2.0/24",
    "192.88.99.0/24",
    "192.168.0.0/16",
    "198.18.0.0/15",
    "198.51.100.0/24",
    "203.0.113.0/24",
    "224.0.0.0/4",
    "240.0.0.0/4",
).map(AddressBlock::of)

private val globalUnicast: AddressBlock = AddressBlock.of("2000::/3")

private val notPublicV6: List<AddressBlock> = listOf(
    "2001::/23",
    "2001:db8::/32",
    "3fff::/20",
).map(AddressBlock::of)

private val mapped: AddressBlock = AddressBlock.of("::ffff:0:0/96")

private val translated: AddressBlock = AddressBlock.of("64:ff9b::/96")

private val sixToFour: AddressBlock = AddressBlock.of("2002::/16")

internal object Addresses {
    fun isPublic(address: ByteArray): Boolean {
        val embedded = embedded(address)
        if (embedded != null) return isPublic(embedded)
        return when (address.size == IPV4_BYTES) {
            true -> notPublicV4.none { block -> block.contains(address) }
            false -> globalUnicast.contains(address) && notPublicV6.none { block -> block.contains(address) }
        }
    }

    private fun embedded(address: ByteArray): ByteArray? {
        val offset = TAIL_OFFSET.takeIf { mapped.contains(address) || translated.contains(address) }
            ?: SIX_TO_FOUR_OFFSET.takeIf { sixToFour.contains(address) }
        return offset?.let { start -> address.copyOfRange(start, start + IPV4_BYTES) }
    }
}
