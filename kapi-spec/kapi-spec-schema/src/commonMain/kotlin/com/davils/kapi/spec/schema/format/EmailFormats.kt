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

private const val MAXIMUM_LOCAL_OCTETS: Int = 64
private const val MAXIMUM_MAILBOX_OCTETS: Int = 254
private const val ATOM_SYMBOLS: String = "!#$%&'*+-/=?^_`{|}~"
private const val IPV6_TAG: String = "IPv6:"
private const val FIRST_PRINTABLE: Int = 0x20
private const val LAST_PRINTABLE: Int = 0x7E
private const val LAST_ASCII: Int = 0x7F

internal object EmailFormats {
    fun email(value: String): String? = mailboxReason(value, isInternational = false)

    fun idnEmail(value: String): String? = mailboxReason(value, isInternational = true)

    private fun mailboxReason(value: String, isInternational: Boolean): String? {
        val at = value.lastIndexOf('@')
        val isShaped = at > 0 && value.length <= MAXIMUM_MAILBOX_OCTETS && octets(value) <= MAXIMUM_MAILBOX_OCTETS
        if (!isShaped) return "an e-mail address has the form local@domain and at most 254 octets"
        val local = value.substring(startIndex = 0, endIndex = at)
        val domain = value.substring(at + 1)
        return localReason(local, isInternational) ?: domainReason(domain, isInternational)
    }

    private fun localReason(local: String, isInternational: Boolean): String? {
        val isQuoted = local.length >= 2 && local.startsWith('"') && local.endsWith('"')
        val content = local.substring(startIndex = 1, endIndex = (local.length - 1).coerceAtLeast(1))
        val isQuotedValid = isQuoted && isQuotedContent(content, isInternational)
        val isDotStringValid = !isQuoted && isDotString(local, isInternational)
        val isShort = octets(local) <= MAXIMUM_LOCAL_OCTETS
        val isValid = (isQuotedValid || isDotStringValid) && isShort
        return "the local part is a dot-atom or a quoted string of at most 64 octets".takeUnless { isValid }
    }

    private fun isDotString(local: String, isInternational: Boolean): Boolean {
        val atoms = local.split('.')
        return atoms.all { atom -> atom.isNotEmpty() && codePoints(atom).all { isAtomText(it, isInternational) } }
    }

    private fun isAtomText(point: Int, isInternational: Boolean): Boolean {
        val isAscii = point <= LAST_ASCII
        val char = point.toChar()
        val isAsciiAtom = isAscii && (isLetter(char) || isDigit(char) || char in ATOM_SYMBOLS)
        val isInternationalAtom = !isAscii && isInternational && isScalar(point)
        return isAsciiAtom || isInternationalAtom
    }

    private fun isQuotedContent(content: String, isInternational: Boolean): Boolean {
        val points = codePoints(content)
        var index = 0
        var isValid = true
        while (isValid && index < points.size) {
            val point = points[index]
            val isPair = point == '\\'.code
            val escaped = points.getOrNull(index + 1) ?: -1
            val isPairValid = isPair && escaped in FIRST_PRINTABLE..LAST_PRINTABLE
            val isTextValid = !isPair && isQuotedText(point, isInternational)
            isValid = isPairValid || isTextValid
            index += 2.takeIf { isPair } ?: 1
        }
        return isValid
    }

    private fun isQuotedText(point: Int, isInternational: Boolean): Boolean {
        val isAscii = point in FIRST_PRINTABLE..LAST_PRINTABLE && point != '"'.code && point != '\\'.code
        val isInternationalText = point > LAST_ASCII && isInternational && isScalar(point)
        return isAscii || isInternationalText
    }

    private fun domainReason(domain: String, isInternational: Boolean): String? {
        val isLiteral = domain.length >= 2 && domain.startsWith('[') && domain.endsWith(']')
        if (isLiteral) return literalReason(domain.substring(startIndex = 1, endIndex = domain.length - 1))
        val international: (String) -> String? = HostnameFormats::idnHostname
        val check = international.takeIf { isInternational } ?: HostnameFormats::hostname
        return check(domain)?.let { "the domain is not a valid hostname: $it" }
    }

    private fun literalReason(literal: String): String? {
        val hasTag = literal.startsWith(IPV6_TAG, ignoreCase = true)
        val isIpv6 = hasTag && AddressFormats.isIpv6(literal.substring(IPV6_TAG.length))
        val isIpv4 = AddressFormats.ipv4(literal) == null
        return "an address literal holds an IPv4 address or IPv6: and an IPv6 address".takeUnless { isIpv4 || isIpv6 }
    }

    private fun octets(text: String): Int = text.encodeToByteArray().size

    private fun isScalar(point: Int): Boolean = point !in Char.MIN_SURROGATE.code..Char.MAX_SURROGATE.code
}
