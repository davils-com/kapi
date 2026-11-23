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

import com.davils.kapi.runtime.core.destination.IpAddress

private const val OPEN: String = "["

private const val CLOSE: String = "]"

private const val DOT: Char = '.'

private const val COLON: Char = ':'

private const val WILDCARD: String = "*."

private const val HEX: String = "0x"

private const val MAXIMUM_LENGTH: Int = 253

private const val MAXIMUM_LABEL_LENGTH: Int = 63

private const val HYPHEN: Char = '-'

private val labelChars: Set<Char> = (('a'..'z') + ('0'..'9') + listOf(HYPHEN, '_')).toSet()

private val privateNames: List<String> = listOf(
    "localhost",
    "local",
    "localdomain",
    "internal",
    "home.arpa",
    "test",
    "invalid",
    "lan",
    "intranet",
    "corp",
    "home",
)

internal object Hosts {
    fun read(text: String): Host {
        if (text.length > MAXIMUM_LENGTH + 2) return Host.Invalid
        val host = normalized(text)
        return literalOf(host)?.let(::address) ?: name(host)
    }

    fun normalized(text: String): String = text.lowercase().removeSuffix(DOT.toString())

    fun textOf(host: Host): String? = when (host) {
        Host.Invalid -> null
        is Host.Address -> host.address.toString()
        is Host.Name -> host.name
    }

    fun isPublicName(name: String): Boolean {
        val isQualified = DOT in name
        return isQualified && privateNames.none { private -> name == private || name.endsWith(DOT + private) }
    }

    fun isPattern(pattern: String): Boolean {
        val host = pattern.removePrefix(WILDCARD)
        return textOf(read(host)) == host
    }

    fun matches(pattern: String, host: String): Boolean {
        if (pattern.startsWith(WILDCARD)) return host.endsWith(pattern.substring(1))
        return host == pattern
    }

    private fun literalOf(host: String): String? {
        val isBracketed = host.startsWith(OPEN) && host.endsWith(CLOSE)
        val inner = host.removeSurrounding(prefix = OPEN, suffix = CLOSE)
        if (isBracketed) return inner.takeIf { text -> COLON in text }.orEmpty()
        return host.takeIf { COLON in host || OPEN in host || isNumeric(host.substringAfterLast(DOT)) }
    }

    private fun address(text: String): Host {
        val address = IpAddress.parseOrNull(text) ?: return Host.Invalid
        return Host.Address(address)
    }

    private fun name(host: String): Host {
        val labels = host.split(DOT)
        val isName = host.length <= MAXIMUM_LENGTH && labels.all(::isLabel)
        if (!isName) return Host.Invalid
        return Host.Name(host)
    }

    private fun isLabel(label: String): Boolean {
        val isBounded = !label.startsWith(HYPHEN) && !label.endsWith(HYPHEN)
        return label.length in 1..MAXIMUM_LABEL_LENGTH && isBounded && label.all { char -> char in labelChars }
    }

    private fun isNumeric(label: String): Boolean {
        val isDecimal = label.isNotEmpty() && label.all { char -> char in '0'..'9' }
        return isDecimal || label.startsWith(HEX)
    }
}
