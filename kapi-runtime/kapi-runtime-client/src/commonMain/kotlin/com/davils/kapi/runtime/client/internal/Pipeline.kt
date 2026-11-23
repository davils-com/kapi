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

import com.davils.kapi.runtime.client.call.Call
import com.davils.kapi.runtime.client.call.CallFailure
import com.davils.kapi.runtime.client.call.Exchange
import com.davils.kapi.runtime.client.pipeline.CallPhase
import com.davils.kapi.runtime.client.pipeline.Interception
import com.davils.kapi.runtime.client.pipeline.RoundTrip
import com.davils.kore.core.failure.attempted
import com.davils.kore.core.outcome.Outcome

internal class Pipeline(
    private val interceptions: List<Interception>,
    private val authorization: Authorization,
    private val transport: Transport,
) {
    suspend fun run(call: Call): RoundTrip {
        val prepared = phase(CallPhase.Prepare, Passage(RoundTrip(call)))
        val authorized = phase(CallPhase.Authorize, authorized(prepared))
        val ready = phase(CallPhase.Send, authorized)
        return phase(CallPhase.Receive, sent(ready)).trip
    }

    private suspend fun authorized(passage: Passage): Passage {
        val trip = passage.trip
        if (passage.isStopped || trip.exchange != null) return passage
        return when (val outcome = authorization.of(trip.call)) {
            is Outcome.Failure -> {
                Passage(trip.withExchange(Exchange.Failed(outcome.failure)), isStopped = true)
            }

            is Outcome.Success -> {
                val credentialed = trip.call.withCredentials(outcome.value)
                Passage(RoundTrip(call = credentialed, granted = outcome.value))
            }
        }
    }

    private suspend fun sent(passage: Passage): Passage {
        val trip = passage.trip
        if (passage.isStopped || trip.exchange != null) return passage
        return Passage(trip.withExchange(transport.send(trip)))
    }

    private suspend fun phase(phase: CallPhase, passage: Passage): Passage {
        val chosen = interceptions.filter { interception -> interception.phase == phase }
        var current = passage
        var index = 0
        while (index < chosen.size && !current.isStopped) {
            current = intercepted(chosen[index], current.trip)
            index += 1
        }
        return current
    }

    private suspend fun intercepted(interception: Interception, trip: RoundTrip): Passage {
        val next = attempted { interception.interceptor.intercept(trip) }.getOrNull()
        val aborted = trip.withExchange(Exchange.Failed(CallFailure.Aborted(interception.name)))
        return next?.let { found -> Passage(found) } ?: Passage(aborted, isStopped = true)
    }

    private data class Passage(val trip: RoundTrip, val isStopped: Boolean = false)
}
