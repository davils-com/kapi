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

package com.davils.kapi.runtime.server.telemetry

import com.davils.kapi.runtime.server.channel
import com.davils.kapi.runtime.server.channel.ChannelEndpoint
import com.davils.kapi.runtime.server.channel.MessageCodec
import com.davils.kapi.runtime.server.fixture.Note
import com.davils.kapi.runtime.server.fixture.RecordingTracer
import com.davils.kapi.runtime.server.fixture.serve
import com.davils.kapi.runtime.server.operation
import com.davils.kapi.runtime.server.operation.Operation
import com.davils.kapi.runtime.server.reply.reply
import com.davils.kollect.core.meterRegistry
import com.davils.kollect.core.snapshot.SumPoint
import com.davils.kollect.core.trace.SpanKind
import com.davils.kollect.core.trace.SpanOutcome
import com.davils.konfetti.core.eventBus
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.get
import io.ktor.websocket.close
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import io.ktor.client.plugins.websocket.WebSockets as ClientWebSockets

private const val GET: String = "GET"

private const val TEAPOT_PATH: String = "/teapot"

private const val FAIL_PATH: String = "/fail"

private const val ANONYMOUS_PATH: String = "/anonymous"

private const val ROOM: String = "/room"

private val expected: List<String> = listOf(
    "Failed(fail, IllegalStateException)",
    "Served(fail, GET, 500, ",
    "Failed(anonymous, Throwable)",
    "Served(anonymous, GET, 500, ",
    "ChannelOpened(room, none)",
    "ChannelClosed(room, 1000, ",
)

private const val OK: Int = 200

private const val TEAPOT: Int = 418

private const val SERVER_ERROR: Int = 500

private val operations: List<Operation> = listOf(
    Operation(name = "ok", method = GET, path = "/ok") { reply(OK) },
    Operation(name = "teapot", method = GET, path = TEAPOT_PATH) { reply(TEAPOT) },
    Operation(name = "fail", method = GET, path = FAIL_PATH) { error("secret") },
    Operation(name = "anonymous", method = GET, path = ANONYMOUS_PATH) { throw object : IllegalStateException() {} },
)

private val room: ChannelEndpoint<Note, Note> = ChannelEndpoint(
    name = "room",
    path = ROOM,
    receive = MessageCodec(Note.serializer()),
    send = MessageCodec(Note.serializer()),
) { }

class TelemetryTest : FunSpec({
    context("a server") {
        test("counts and times the requests of an operation by how they ended, and the sessions of a channel") {
            val registry = meterRegistry()
            serve(
                settings = { telemetry { meters(registry) } },
                routes = {
                    operations.forEach { served -> operation(served) }
                    channel(room)
                },
            ) {
                listOf("/ok", TEAPOT_PATH, FAIL_PATH).forEach { path -> client.get(path) }
                createClient { install(ClientWebSockets) }.use { sockets -> sockets.webSocket(ROOM) { close() } }
            }
            val families = registry.snapshot().families.associateBy { family -> family.descriptor.name.value }
            val outcomes = families.getValue("kapi.server.requests").points.map { point ->
                point.labels.values.joinToString("/") + "=" + (point as SumPoint).value
            }

            outcomes.sorted() shouldBe listOf("fail/server_error=1.0", "ok/success=1.0", "teapot/client_error=1.0")
            families.getValue("kapi.server.request.duration").points.size shouldBe TIMED
            families.getValue("kapi.server.channel.sessions").points.size shouldBe 1
        }

        test("traces every request in a span of kind server with its operation, method and status") {
            val tracer = RecordingTracer()
            serve(
                settings = { telemetry { this.tracer = tracer } },
                routes = { operations.forEach { operation(it) } },
            ) {
                client.get(TEAPOT_PATH)
            }
            val span = tracer.spans.single()

            span.operation shouldBe "kapi.server.request"
            span.kind shouldBe SpanKind.Server
            span.outcome shouldBe SpanOutcome.Ok
            span.attributes shouldBe mapOf(
                "kapi.operation" to "teapot",
                "http.request.method" to "GET",
                "http.response.status_code" to "418",
            )
        }

        test("publishes its events on its topic while someone listens, without what the request carried") {
            val scope = CoroutineScope(SupervisorJob())
            val bus = eventBus(scope)
            val received = Channel<ServerEvent>(Channel.UNLIMITED)
            bus.declare(ServerTopics.EVENTS)
            bus[ServerTopics.EVENTS].consume("test") { handle { envelope -> received.send(envelope.payload) } }
            try {
                serve(
                    settings = { telemetry { events(bus) } },
                    routes = {
                        operations.forEach { served -> operation(served) }
                        channel(room)
                    },
                ) {
                    client.get(FAIL_PATH)
                    client.get(ANONYMOUS_PATH)
                    createClient { install(ClientWebSockets) }.use { sockets -> sockets.webSocket(ROOM) { close() } }
                }
                val events = withTimeout(5.seconds) { List(EVENTS) { received.receive() } }
                val shown = events.map(ServerEvent::toString)

                shown.zip(expected).forEach { (event, start) -> event shouldStartWith start }
                shown.joinToString().contains("secret") shouldBe false
            } finally {
                withContext(NonCancellable) { bus.close() }
                scope.cancel()
            }
        }
    }

    context("the settings of telemetry") {
        test("show what they report to") {
            TelemetrySettings.None.toString() shouldBe "TelemetrySettings(meters=false, events=false)"
            val configured = TelemetrySettingsBuilder().apply { meters(meterRegistry()) }

            configured.build().toString() shouldBe "TelemetrySettings(meters=true, events=false)"
            ServerTopics.EVENTS.name.toString() shouldContain "kapi.server"
            val opened = ServerEvent.ChannelOpened(channel = "c", protocol = "p")

            opened.toString() shouldBe "ChannelOpened(c, p)"
            ServerEvent.ChannelClosed("c", CLOSED, 1.milliseconds).toString() shouldBe "ChannelClosed(c, 1000, 1ms)"
        }
    }

    context("a server whose bus has nobody listening") {
        test("declares its topic and builds no event, and names a failure of no name by its kind") {
            val scope = CoroutineScope(SupervisorJob())
            val bus = eventBus(scope)
            val anonymous = Operation(name = "anonymous", method = GET, path = ANONYMOUS_PATH) {
                throw object : IllegalStateException() {}
            }
            try {
                serve(settings = { telemetry { events(bus) } }, routes = { operation(anonymous) }) {
                    client.get(ANONYMOUS_PATH).status.value shouldBe SERVER_ERROR
                }

                (ServerTopics.EVENTS in bus) shouldBe true
            } finally {
                withContext(NonCancellable) { bus.close() }
                scope.cancel()
            }
        }
    }
})

private const val EVENTS: Int = 6

private const val TIMED: Int = 3
private const val CLOSED: Int = 1_000
