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

package com.davils.kapi.runtime.core.destination.internal

import com.davils.kapi.runtime.core.destination.Destination
import com.davils.kapi.runtime.core.destination.DestinationRefusal
import com.davils.kapi.runtime.core.internal.FieldGrammar
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.outcome.Outcome
import com.davils.kore.core.uri.UriReference

private const val AT: Char = '@'

private const val COLON: Char = ':'

private const val CLOSE: Char = ']'

private const val OPEN: Char = '['

private const val MOST_PORT: Int = 65_535

private const val MOST_PORT_DIGITS: Int = 5

internal object Urls {
    fun destinationOf(url: String): Outcome<Destination, DestinationRefusal> {
        val reference = UriReference.parseOrNull(url)
        val scheme = reference?.scheme
        val authority = reference?.authority?.takeIf(String::isNotEmpty)
        if (scheme == null || authority == null) return Outcome.Failure(DestinationRefusal.Url)
        return fromAuthority(scheme, authority)
    }

    fun redirectOf(from: String, location: String): Outcome<Destination, DestinationRefusal> {
        val base = UriReference.parseOrNull(from)?.takeIf(UriReference::isAbsolute)
        val target = UriReference.parseOrNull(location)?.let { reference -> base?.resolve(reference) }
        return target?.let { resolved -> destinationOf(resolved.toString()) } ?: Outcome.Failure(DestinationRefusal.Url)
    }

    private fun fromAuthority(scheme: String, authority: String): Outcome<Destination, DestinationRefusal> {
        if (AT in authority) return Outcome.Failure(DestinationRefusal.UserInfo)
        val portStart = portStartOf(authority)
        val host = authority.substring(0, portStart)
        val portText = authority.substring(minOf(portStart + 1, authority.length))
        val hasPort = portStart < authority.length
        val port = portText.takeIf { text -> hasPort && text.isNotEmpty() }?.let(::portOf)
        val isPortValid = !hasPort || portText.isEmpty() || port != null
        val destination = Destination(scheme.lowercase(), host, port).takeIf { isPortValid && host.isNotEmpty() }
        return destination?.let { found -> Outcome.Success(found) } ?: Outcome.Failure(DestinationRefusal.Url)
    }

    private fun portStartOf(authority: String): Int {
        val searchFrom = authority.indexOf(CLOSE).takeIf { authority.startsWith(OPEN) } ?: 0
        val colon = authority.indexOf(COLON, maxOf(searchFrom, 0))
        return colon.takeIf { index -> index >= 0 } ?: authority.length
    }

    private fun portOf(text: String): Int? {
        val isDigits = text.length <= MOST_PORT_DIGITS && text.all(FieldGrammar::isDigit)
        val port = text.takeIf { isDigits }?.toInt()
        return port?.takeIf { number -> number <= MOST_PORT }
    }
}
