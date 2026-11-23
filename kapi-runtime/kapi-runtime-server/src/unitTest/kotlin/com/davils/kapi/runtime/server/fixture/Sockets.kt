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

package com.davils.kapi.runtime.server.fixture

import com.davils.kapi.runtime.server.Kapi
import com.davils.kapi.runtime.server.ServerSettingsBuilder
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.Route
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.net.Socket

private const val HOST: String = "127.0.0.1"

internal suspend fun onCio(
    settings: ServerSettingsBuilder.() -> Unit = {},
    transport: Application.() -> Unit = {},
    routes: Route.() -> Unit,
    block: suspend (Int) -> Unit,
) {
    val server = embeddedServer(CIO, port = 0, host = HOST) {
        install(Kapi, settings)
        install(WebSockets)
        transport()
        routing { routes() }
    }
    server.startSuspend(wait = false)
    try {
        val port = server.engine
            .resolvedConnectors()
            .first()
            .port
        block(port)
    } finally {
        withContext(NonCancellable) { server.stopSuspend(gracePeriodMillis = 0L, timeoutMillis = 0L) }
    }
}

internal suspend fun statusLineOf(
    port: Int,
    request: String,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
): String = withContext(dispatcher) {
    Socket(HOST, port).use { socket ->
        socket.getOutputStream().write(request.encodeToByteArray())
        val reader = socket.getInputStream().bufferedReader()
        reader.readLine().orEmpty()
    }
}
