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

package com.davils.kapi.runtime.client.internal

import com.davils.kapi.runtime.client.call.CallFailure
import com.davils.kapi.runtime.core.destination.Destination
import com.davils.kapi.runtime.core.destination.DestinationDecision
import com.davils.kapi.runtime.core.destination.DestinationPolicy
import com.davils.kapi.runtime.core.destination.DestinationRefusal
import io.ktor.http.Url
import io.ktor.http.parseUrl

internal class Destinations(private val policy: DestinationPolicy) {
    suspend fun refusal(url: String): CallFailure? = when (val decision = policy.check(url)) {
        is DestinationDecision.Refused -> CallFailure.DestinationRefused(decision.refusal)
        is DestinationDecision.Allowed -> parsedRefusal(url, decision)
    }

    private suspend fun parsedRefusal(url: String, allowed: DestinationDecision.Allowed): CallFailure? {
        val parsed = parseUrl(url) ?: return CallFailure.DestinationRefused(DestinationRefusal.Url)
        val destination = destinationOf(parsed)
        val decision = allowed.addresses.takeIf(List<*>::isNotEmpty)?.let { addresses ->
            policy.checkResolved(destination, addresses)
        } ?: policy.check(destination)
        val refused = decision as? DestinationDecision.Refused
        return refused?.let { found -> CallFailure.DestinationRefused(found.refusal) }
    }

    companion object {
        fun destinationOf(url: Url): Destination {
            val port = url.specifiedPort.takeIf { number -> number > 0 }
            return Destination(scheme = url.protocol.name, host = url.host, port = port)
        }
    }
}
