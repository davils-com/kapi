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

package com.davils.kapi.runtime.server.internal

import com.davils.kapi.runtime.server.telemetry.ServerEvent
import com.davils.kapi.runtime.server.telemetry.ServerTopics
import com.davils.kapi.runtime.server.telemetry.TelemetrySettings
import com.davils.kollect.core.Family
import com.davils.kollect.core.MeterRegistry
import com.davils.kollect.core.meter.Counter
import com.davils.kollect.core.meter.Timer
import com.davils.kollect.core.trace.Tracer
import com.davils.konfetti.core.EventBus
import com.davils.konfetti.core.Topic
import com.davils.kore.core.failure.contained
import kotlin.time.Duration
import kotlin.time.TimeSource

private const val OPERATION: String = "operation"

private const val OUTCOME: String = "outcome"

private const val CHANNEL: String = "channel"

internal class ServerTelemetry(settings: TelemetrySettings) {
    val tracer: Tracer = settings.tracer

    val timeSource: TimeSource.WithComparableMarks = TimeSource.Monotonic

    private val requests: Family<Counter>? = settings.meters?.let(::requestsOf)

    private val durations: Family<Timer>? = settings.meters?.let(::durationsOf)

    private val sessions: Family<Counter>? = settings.meters?.let(::sessionsOf)

    private val topic: Topic<ServerEvent>? = settings.events?.let(::topicOf)

    fun served(operation: String, method: String, status: Int, duration: Duration) {
        requests?.run { with(operation, outcomeOf(status)).increment() }
        durations?.run { with(operation).record(duration) }
        emit { ServerEvent.Served(operation = operation, method = method, status = status, duration = duration) }
    }

    fun failed(operation: String, failure: Throwable) {
        emit { ServerEvent.Failed(operation, failure::class.simpleName ?: UNNAMED) }
    }

    fun opened(channel: String, protocol: String?) {
        sessions?.run { with(channel).increment() }
        emit { ServerEvent.ChannelOpened(channel, protocol) }
    }

    fun closed(channel: String, code: Int, duration: Duration) {
        emit { ServerEvent.ChannelClosed(channel = channel, code = code, duration = duration) }
    }

    private inline fun emit(event: () -> ServerEvent) {
        val target = topic?.takeIf { found -> found.hasConsumers } ?: return
        val built = event()
        contained { target.offer(built) }
    }

    private companion object {
        const val REQUESTS: String = "kapi.server.requests"
        const val DURATION: String = "kapi.server.request.duration"
        const val SESSIONS: String = "kapi.server.channel.sessions"
        const val UNNAMED: String = "Throwable"

        fun requestsOf(registry: MeterRegistry): Family<Counter> {
            val registration = registry.counter(REQUESTS) {
                description = "The requests of an operation by how they ended."
                labelKeys(OPERATION, OUTCOME)
            }
            return registration.family
        }

        fun sessionsOf(registry: MeterRegistry): Family<Counter> {
            val registration = registry.counter(SESSIONS) {
                description = "The sessions of a WebSocket channel."
                labelKeys(CHANNEL)
            }
            return registration.family
        }

        fun durationsOf(registry: MeterRegistry): Family<Timer> {
            val registration = registry.timer(DURATION) {
                description = "How long the requests of an operation took until their reply was handed to Ktor."
                labelKeys(OPERATION)
            }
            return registration.family
        }

        fun topicOf(bus: EventBus): Topic<ServerEvent> {
            if (ServerTopics.EVENTS !in bus) bus.declare(ServerTopics.EVENTS)
            return bus[ServerTopics.EVENTS]
        }

        fun outcomeOf(status: Int): String {
            if (status >= Statuses.SERVER_ERRORS) return "server_error"
            return "client_error".takeIf { status >= Statuses.CLIENT_ERRORS } ?: "success"
        }
    }
}
