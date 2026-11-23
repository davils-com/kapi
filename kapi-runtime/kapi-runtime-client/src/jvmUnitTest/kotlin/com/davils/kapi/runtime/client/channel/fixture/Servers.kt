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

package com.davils.kapi.runtime.client.channel.fixture

import com.davils.kapi.runtime.client.ApiClient
import com.davils.kapi.runtime.client.apiClient
import com.davils.kapi.runtime.client.call.Timeout
import com.davils.kapi.runtime.core.destination.destinationPolicy
import com.davils.kapi.runtime.core.security.Secret
import com.davils.kapi.runtime.core.security.credentials
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import io.ktor.client.engine.cio.CIO as ClientEngine
import io.ktor.server.websocket.WebSockets as ServerWebSockets

internal const val PROTOCOL: String = "chat"

internal const val GONE: Short = 4_001

private const val HOST: String = "127.0.0.1"

private const val TIMEOUT_SECONDS: Int = 10

internal suspend fun <T> withServer(timeout: Duration = TIMEOUT_SECONDS.seconds, block: suspend (ApiClient) -> T): T {
    val server = embeddedServer(CIO, port = 0, host = HOST) {
        install(ServerWebSockets)
        routing {
            webSocket("/echo", protocol = PROTOCOL) {
                val authorization = call.request.headers["Authorization"].orEmpty()
                outgoing.send(Frame.Text("""{"text":"$authorization"}"""))
                val frame = incoming.receive() as Frame.Text
                outgoing.send(Frame.Text(frame.readText()))
                incoming.receiveCatching()
            }
            get("/slow") {
                delay(TIMEOUT_SECONDS.seconds)
                call.respondText("late")
            }
            webSocket("/gone", protocol = PROTOCOL) {
                close(CloseReason(GONE, "gone"))
            }
        }
    }
    server.start(wait = false)
    try {
        val connectors = server.engine.resolvedConnectors()
        val port = connectors.first().port
        return HttpClient(ClientEngine) {
            followRedirects = false
            install(WebSockets)
        }.use { http -> block(clientOf(http = http, port = port, timeout = timeout)) }
    } finally {
        server.stop(gracePeriodMillis = 0, timeoutMillis = 0)
    }
}

private fun clientOf(http: HttpClient, port: Int, timeout: Duration): ApiClient = apiClient(http) {
    baseUrl = "http://$HOST:$port"
    this.timeout = Timeout(request = timeout)
    destinations = destinationPolicy {
        schemes = setOf("http")
        hosts = setOf(HOST)
    }
    credentials = credentials { scheme("bearer") { Secret("t1") } }
}
