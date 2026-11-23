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

private const val BYTE_BITS: Int = 8

private const val BYTE_MASK: Int = 0xFF

internal class AddressBlock(private val prefix: ByteArray, private val length: Int) {
    fun contains(address: ByteArray): Boolean {
        if (address.size != prefix.size) return false
        return (0 until length).all { bit -> bitAt(address, bit) == bitAt(prefix, bit) }
    }

    private fun bitAt(bytes: ByteArray, bit: Int): Int {
        val byte = bytes[bit / BYTE_BITS].toInt() and BYTE_MASK
        return (byte shr BYTE_BITS - 1 - bit % BYTE_BITS) and 1
    }

    companion object {
        fun of(cidr: String): AddressBlock {
            val prefix = IpParser.parse(cidr.substringBefore('/'))
            val length = cidr.substringAfter('/').toInt()
            return AddressBlock(requireNotNull(prefix) { "The block of an address is malformed" }, length)
        }
    }
}
