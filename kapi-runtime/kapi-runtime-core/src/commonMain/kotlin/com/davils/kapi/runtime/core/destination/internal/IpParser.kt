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

import com.davils.kapi.runtime.core.internal.FieldGrammar
import com.davils.kapi.runtime.core.internal.PercentCoding

private const val DOT: Char = '.'

private const val COLON: Char = ':'

private const val COMPRESSION: String = "::"

private const val IPV4_PARTS: Int = 4

private const val IPV4_BYTES: Int = 4

private const val IPV6_BYTES: Int = 16

private const val IPV6_GROUPS: Int = 8

private const val MOST_OCTET_DIGITS: Int = 3

private const val MOST_GROUP_DIGITS: Int = 4

private const val MOST_OCTET: Int = 255

private const val BYTE_BITS: Int = 8

private const val BYTE_MASK: Int = 0xFF

private const val RADIX: Int = 16

private const val MAX_TEXT_LENGTH: Int = 45

internal object IpParser {
    fun parse(text: String): ByteArray? {
        if (text.length > MAX_TEXT_LENGTH) return null
        return ipv6(text).takeIf { COLON in text } ?: ipv4(text).takeIf { COLON !in text }
    }

    fun ipv4(text: String): ByteArray? {
        val octets = text.split(DOT).map(::octet)
        val values = octets.filterNotNull().takeIf { found -> found.size == octets.size && found.size == IPV4_PARTS }
        return values?.let { found -> ByteArray(IPV4_BYTES) { index -> found[index].toByte() } }
    }

    private fun octet(part: String): Int? {
        val isDecimal = part.length in 1..MOST_OCTET_DIGITS && part.all(FieldGrammar::isDigit)
        val isCanonical = isDecimal && (part.length == 1 || part.first() != '0')
        val value = part.takeIf { isCanonical }?.toInt()
        return value?.takeIf { number -> number <= MOST_OCTET }
    }

    private fun ipv6(text: String): ByteArray? {
        val halves = text.split(COMPRESSION)
        val isCompressed = halves.size == 2
        val head = groups(halves.first(), isLast = !isCompressed)
        val tail = halves.getOrNull(1)?.let { half -> groups(half, isLast = true) } ?: emptyList<Int>().takeIf {
            !isCompressed
        }
        if (halves.size > 2 || head == null || tail == null) return null
        return words(head, tail, isCompressed)?.let(::bytesOf)
    }

    private fun words(head: List<Int>, tail: List<Int>, isCompressed: Boolean): List<Int>? {
        val count = head.size + tail.size
        val isComplete = (isCompressed && count < IPV6_GROUPS) || (!isCompressed && count == IPV6_GROUPS)
        return (head + List(maxOf(a = 0, b = IPV6_GROUPS - count)) { 0 } + tail).takeIf { isComplete }
    }

    private fun groups(text: String, isLast: Boolean): List<Int>? {
        if (text.isEmpty()) return emptyList()
        val parts = text.split(COLON)
        val last = parts.last()
        val embedded = last.takeIf { isLast && DOT in last }?.let(::ipv4)
        val hexParts = parts.dropLast(1).takeIf { embedded != null } ?: parts
        val hex = hexParts.map(::group)
        val isValid = hex.all { group -> group != null } && (embedded != null || DOT !in last)
        val words = hex.filterNotNull() + embeddedGroups(embedded)
        return words.takeIf { isValid }
    }

    private fun embeddedGroups(embedded: ByteArray?): List<Int> {
        if (embedded == null) return emptyList()
        return listOf(
            word(high = embedded[0], low = embedded[1]),
            word(high = embedded[2], low = embedded[IPV4_BYTES - 1]),
        )
    }

    private fun word(high: Byte, low: Byte): Int =
        ((high.toInt() and BYTE_MASK) shl BYTE_BITS) or (low.toInt() and BYTE_MASK)

    private fun group(text: String): Int? {
        val isHex = text.length in 1..MOST_GROUP_DIGITS && text.all(PercentCoding::isHex)
        return text.takeIf { isHex }?.toInt(RADIX)
    }

    private fun bytesOf(words: List<Int>): ByteArray {
        val bytes = ByteArray(IPV6_BYTES)
        words.forEachIndexed { index, value ->
            bytes[index * 2] = (value shr BYTE_BITS).toByte()
            bytes[index * 2 + 1] = value.toByte()
        }
        return bytes
    }
}
