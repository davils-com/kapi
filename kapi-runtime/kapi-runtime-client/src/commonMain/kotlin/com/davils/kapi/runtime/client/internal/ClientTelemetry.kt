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

import com.davils.kapi.runtime.client.ClientEvent
import com.davils.kapi.runtime.client.ClientSettings
import com.davils.kapi.runtime.client.call.Call
import com.davils.kapi.runtime.client.call.Exchange
import com.davils.kollect.core.Family
import com.davils.kollect.core.meter.Counter
import com.davils.kollect.core.meter.Timer
import com.davils.kollect.core.trace.SpanKind
import com.davils.kollect.core.trace.SpanOutcome

private const val SPAN: String = "kapi.client.call"

private const val CALLS: String = "kapi.client.calls"

private const val DURATION: String = "kapi.client.call.duration"

private const val API: String = "api"

private const val OPERATION: String = "operation"

private const val OUTCOME: String = "outcome"

private const val API_ATTRIBUTE: String = "kapi.api"

private const val OPERATION_ATTRIBUTE: String = "kapi.operation"

private const val OUTCOME_ATTRIBUTE: String = "kapi.outcome"

private const val STATUS_ATTRIBUTE: String = "http.response.status_code"

internal class ClientTelemetry(private val settings: ClientSettings) {
    private val calls: Family<Counter>? = settings.meters?.run {
        val declared = counter(CALLS) {
            description = "The calls of an operation by their outcome."
            labelKeys(API, OPERATION, OUTCOME)
        }
        declared.family
    }

    private val durations: Family<Timer>? = settings.meters?.run {
        val declared = timer(DURATION) {
            description = "How long a call took, its retries and redirects included."
            labelKeys(API, OPERATION)
        }
        declared.family
    }

    private val events: EventSink<ClientEvent> = EventSink(settings.events, ClientEvent.TOPIC)

    suspend fun calling(call: Call, block: suspend () -> Exchange): Exchange {
        val started = settings.timeSource.markNow()
        val span = settings.tracer.start(SPAN, SpanKind.Client)
        var outcome = SpanOutcome.Error
        try {
            span.attribute(key = API_ATTRIBUTE, value = settings.name)
            span.attribute(key = OPERATION_ATTRIBUTE, value = call.operation)
            val exchange = block()
            val tag = CallOutcome.of(exchange)
            span.attribute(key = OUTCOME_ATTRIBUTE, value = tag.tag)
            val answered = exchange as? Exchange.Answered
            answered?.let { found -> span.attribute(key = STATUS_ATTRIBUTE, value = "${found.status}") }
            outcome = SpanOutcome.Ok.takeIf { tag == CallOutcome.Success || tag == CallOutcome.ClientError } ?: outcome
            calls?.run { with(settings.name, call.operation, tag.tag).increment() }
            durations?.run { with(settings.name, call.operation).record(started.elapsedNow()) }
            return exchange
        } finally {
            span.end(outcome)
        }
    }

    fun emit(event: () -> ClientEvent) {
        events.emit(event)
    }
}
