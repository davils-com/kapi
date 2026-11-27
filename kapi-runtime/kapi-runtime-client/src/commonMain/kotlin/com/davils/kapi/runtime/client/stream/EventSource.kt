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

@file:OptIn(ExperimentalAtomicApi::class)

package com.davils.kapi.runtime.client.stream

import com.davils.kapi.runtime.client.ApiClient
import com.davils.kapi.runtime.client.ClientEvent
import com.davils.kapi.runtime.client.call.Call
import com.davils.kapi.runtime.client.call.CallFailure
import com.davils.kapi.runtime.client.call.Exchange
import com.davils.kapi.runtime.client.internal.StreamSink
import com.davils.kapi.runtime.client.resilienceEnvironment
import com.davils.kapi.runtime.core.framing.FramingFailure
import com.davils.kapi.runtime.core.framing.ServerSentEvent
import com.davils.kapi.runtime.core.framing.ServerSentEventReader
import com.davils.kontinuity.core.backoff.fixed
import com.davils.kontinuity.retry.RetryConfig
import com.davils.kontinuity.retry.retry
import com.davils.kore.core.outcome.Outcome
import kotlinx.coroutines.withContext
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private const val RETRY_NAME: String = "kapi-event-stream"

private const val ROUND: Int = RetryConfig.HARD_ATTEMPTS - 1

internal class EventSource(private val client: ApiClient, private val call: Call) {
    private val lastId = AtomicReference("")
    private val serverRetry = AtomicReference<Long?>(null)
    private val reconnects = AtomicInt(0)

    suspend fun connect(deliver: suspend (ServerSentEvent) -> Unit): Exchange {
        val reader = ServerSentEventReader(client.settings.framing)
        val handle: suspend (Outcome<List<ServerSentEvent>, FramingFailure>) -> CallFailure? = { outcome ->
            handled(outcome, deliver)
        }
        val sink = StreamSink(
            isStreamed = ::isEventStream,
            accept = { chunk -> handle(reader.feed(chunk)) },
            finish = { handle(reader.finish()) },
        )
        val exchange = withContext(sink) { client.streamed(resumed(call = call, lastId = lastId.load())) }
        lastId.store(reader.lastEventId.ifEmpty { lastId.load() })
        serverRetry.store(reader.retry ?: serverRetry.load())
        return exchange
    }

    suspend fun reconnecting(reconnection: Reconnection, deliver: suspend (ServerSentEvent) -> Unit): Exchange {
        var exchange = connect(deliver)
        var left = reconnection.maxReconnects
        while (left > 0 && isReconnectable(exchange)) {
            val round = left.coerceAtMost(ROUND)
            exchange = reconnected(reconnection = reconnection, round = round, previous = exchange, deliver = deliver)
            left -= round
        }
        return exchange
    }

    private suspend fun reconnected(
        reconnection: Reconnection,
        round: Int,
        previous: Exchange,
        deliver: suspend (ServerSentEvent) -> Unit,
    ): Exchange {
        val retry = retry(name = RETRY_NAME, environment = client.settings.resilienceEnvironment()) {
            maxAttempts = round + 1
            backoff = fixed(reconnection.delay.coerceAtMost(reconnection.maximumDelay))
            isFailingAfterMaxAttempts = false
            failures { ignore<Throwable>() }
            results { failIf<Exchange> { exchange -> isReconnectable(exchange) } }
            interval { _, _, _ -> announced(reconnection) }
        }
        val replays = AtomicInt(0)
        return retry.execute<Exchange> {
            val isFirst = replays.incrementAndFetch() == 1
            previous.takeIf { isFirst } ?: connect(deliver)
        }
    }

    private fun announced(reconnection: Reconnection): Duration {
        val wait = (serverRetry.load()?.milliseconds ?: reconnection.delay).coerceAtMost(reconnection.maximumDelay)
        val attempt = reconnects.incrementAndFetch()
        val isResumed = lastId.load().isNotEmpty()
        client.telemetry.emit {
            ClientEvent.Reconnecting(
                api = client.settings.name,
                operation = call.operation,
                attempt = attempt,
                delay = wait,
                isResumed = isResumed,
            )
        }
        return wait
    }

    private fun isReconnectable(exchange: Exchange): Boolean = when (exchange) {
        is Exchange.Failed -> exchange.failure is CallFailure.Unreachable || exchange.failure == CallFailure.TimedOut
        is Exchange.Answered -> isEventStream(exchange.status, exchange.headers)
    }
}
