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

@file:OptIn(DavilsInternal::class)

package com.davils.kapi.runtime.core.destination

import com.davils.kapi.runtime.core.destination.internal.Addresses
import com.davils.kapi.runtime.core.destination.internal.IpParser
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

private const val IPV4_BYTES: Int = 4

private const val IPV6_BYTES: Int = 16

private const val BYTE_BITS: Int = 8

private const val BYTE_MASK: Int = 0xFF

private const val RADIX: Int = 16

private const val MAPPED_PREFIX: Int = 10

private const val MAPPED_BYTES: Int = 12

/**
 * An IPv4 or IPv6 address, such as one a resolver found for a host name.
 *
 * An address is read strictly: IPv4 only as four decimal octets without leading zeros, so `0x7f.1`, `127.1` and
 * `010.0.0.1` are refused rather than guessed, and IPv6 by RFC 4291 §2.2 with an optional IPv4 tail, without a zone.
 * [toString] writes the canonical text of RFC 5952 for IPv6 and the dotted quad for IPv4. [isPublic] tells whether
 * the address is globally reachable, judging an IPv4-mapped, a NAT64 (`64:ff9b::/96`) and a 6to4 address by the IPv4
 * address it carries. An address is immutable and safe to share between threads.
 *
 * @since 1.0.0
 */
public class IpAddress private constructor(private val bytes: ByteArray) {
    /**
     * Whether this is an IPv4 address.
     *
     * @since 1.0.0
     */
    public val isIpv4: Boolean get() = bytes.size == IPV4_BYTES

    /**
     * Whether the address is globally reachable: no loopback, private, shared, link-local, multicast, documentation,
     * benchmarking, reserved, unique-local, site-local, discard or IETF protocol address, and none of the special
     * blocks of the IANA registries of RFC 6890, and for IPv6 inside `2000::/3`.
     *
     * @since 1.0.0
     */
    public val isPublic: Boolean get() = Addresses.isPublic(bytes)

    /**
     * Returns the 4 or 16 bytes of the address in network order.
     *
     * @return a copy of the bytes.
     * @since 1.0.0
     */
    public fun toByteArray(): ByteArray = bytes.copyOf()

    override fun equals(other: Any?): Boolean = other is IpAddress && bytes.contentEquals(other.bytes)

    override fun hashCode(): Int = bytes.contentHashCode()

    override fun toString(): String {
        if (isIpv4) return quad(bytes)
        return ipv6Text()
    }

    private fun ipv6Text(): String {
        val isMapped = (0 until MAPPED_PREFIX).all { index -> bytes[index].toInt() == 0 } &&
            (MAPPED_PREFIX until MAPPED_BYTES).all { index -> bytes[index].toInt() and BYTE_MASK == BYTE_MASK }
        if (isMapped) return "::ffff:" + quad(bytes.copyOfRange(MAPPED_BYTES, IPV6_BYTES))
        val words = (0 until IPV6_BYTES / 2).map { index ->
            ((bytes[index * 2].toInt() and BYTE_MASK) shl BYTE_BITS) or (bytes[index * 2 + 1].toInt() and BYTE_MASK)
        }
        return compressed(words)
    }

    private fun compressed(words: List<Int>): String {
        val run = longestZeroRun(words)
        val texts = words.map { word -> word.toString(RADIX) }
        if (run == null) return texts.joinToString(":")
        val head = texts.subList(0, run.first).joinToString(":")
        val tail = texts.subList(run.last + 1, texts.size).joinToString(":")
        return "$head::$tail"
    }

    private fun longestZeroRun(words: List<Int>): IntRange? {
        var best: IntRange? = null
        var index = 0
        while (index < words.size) {
            val end = runEnd(words, index)
            val size = end - index
            if (size > 1 && size > (best?.let { found -> found.last - found.first + 1 } ?: 0)) best = index until end
            index = maxOf(end, index + 1)
        }
        return best
    }

    private fun runEnd(words: List<Int>, start: Int): Int {
        var end = start
        while (end < words.size && words[end] == 0) end += 1
        return end
    }

    private fun quad(octets: ByteArray): String =
        octets.joinToString(".") { octet -> (octet.toInt() and BYTE_MASK).toString() }

    /**
     * Reads and makes addresses.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Reads [text] as an IPv4 or IPv6 address, without brackets.
         *
         * @param text the text, which may come from an attacker.
         * @return the address, or `null` if [text] is none by the strict grammar.
         * @since 1.0.0
         */
        public fun parseOrNull(text: String): IpAddress? = IpParser.parse(text)?.let(::IpAddress)

        /**
         * Reads [text] like [parseOrNull].
         *
         * @param text the text.
         * @return the address.
         * @throws IllegalArgumentException if [text] is no address.
         * @since 1.0.0
         */
        public fun parse(text: String): IpAddress =
            requireNotNull(parseOrNull(text)) { "${SafeText.quoted(text)} is no IP address" }

        /**
         * Makes the address of [bytes].
         *
         * @param bytes 4 bytes of IPv4 or 16 bytes of IPv6, in network order.
         * @return the address, or `null` for another number of bytes.
         * @since 1.0.0
         */
        public fun fromByteArrayOrNull(bytes: ByteArray): IpAddress? {
            val isAddress = bytes.size == IPV4_BYTES || bytes.size == IPV6_BYTES
            val copy = bytes.copyOf()
            return copy.takeIf { isAddress }?.let(::IpAddress)
        }
    }
}
