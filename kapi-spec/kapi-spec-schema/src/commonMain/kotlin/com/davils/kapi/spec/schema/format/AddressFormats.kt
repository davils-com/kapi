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

package com.davils.kapi.spec.schema.format

private const val IPV4_PARTS: Int = 4
private const val MAXIMUM_OCTET_DIGITS: Int = 3
private const val LAST_OCTET: Int = 255
private const val IPV6_GROUPS: Int = 8
private const val MAXIMUM_GROUP_DIGITS: Int = 4
private const val MAXIMUM_IPV6_LENGTH: Int = 45
private const val COMPRESSION: String = "::"
private const val EMBEDDED_GROUPS: String = "0:0"

internal object AddressFormats {
    fun ipv4(value: String): String? {
        val parts = value.split('.')
        val isValid = parts.size == IPV4_PARTS && parts.all(::isOctet)
        return "an IPv4 address is four decimal octets without leading zeros, such as 192.0.2.1".takeUnless { isValid }
    }

    fun ipv6(value: String): String? {
        val isShort = value.length <= MAXIMUM_IPV6_LENGTH
        val isValid = isShort && isIpv6(value)
        return "an IPv6 address has eight hexadecimal groups or fewer with one ::, and no zone".takeUnless { isValid }
    }

    fun isIpv6(value: String): Boolean {
        val lastColon = value.lastIndexOf(':')
        if (lastColon < 0) return false
        val tail = value.substring(lastColon + 1)
        val hasEmbeddedIpv4 = tail.contains('.')
        val prefix = value.substring(startIndex = 0, endIndex = lastColon + 1)
        val hexadecimal = (prefix + EMBEDDED_GROUPS).takeIf { hasEmbeddedIpv4 } ?: value
        val isEmbeddedValid = !hasEmbeddedIpv4 || ipv4(tail) == null
        return isEmbeddedValid && isHexadecimalIpv6(hexadecimal)
    }

    private fun isHexadecimalIpv6(value: String): Boolean {
        val compression = value.indexOf(COMPRESSION)
        val hasSecondCompression = compression >= 0 && value.indexOf(COMPRESSION, compression + 1) >= 0
        if (hasSecondCompression) return false
        val head = value.substring(startIndex = 0, endIndex = compression.takeIf { it >= 0 } ?: value.length)
        val tail = value.substring((compression + COMPRESSION.length).takeIf { compression >= 0 } ?: value.length)
        val groups = groups(head) + groups(tail)
        val isCompressed = compression >= 0
        val isCountRight = (!isCompressed && groups.size == IPV6_GROUPS) || (isCompressed && groups.size < IPV6_GROUPS)
        return isCountRight && groups.all(::isGroup)
    }

    private fun groups(text: String): List<String> {
        if (text.isEmpty()) return emptyList()
        return text.split(':')
    }

    private fun isGroup(group: String): Boolean = group.length in 1..MAXIMUM_GROUP_DIGITS && group.all(::isHexDigit)

    private fun isOctet(part: String): Boolean {
        val isShaped = part.length in 1..MAXIMUM_OCTET_DIGITS && isAllDigits(part)
        val hasLeadingZero = part.length > 1 && part.startsWith('0')
        return isShaped && !hasLeadingZero && part.toInt() <= LAST_OCTET
    }
}
