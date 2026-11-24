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

import com.davils.kapi.runtime.core.problem.Problem
import com.davils.kapi.runtime.core.problem.ProblemSerializer
import com.davils.kapi.runtime.server.Kapi
import com.davils.kapi.runtime.server.ServerSettingsBuilder
import com.davils.kapi.runtime.server.channel.ChannelLimits
import com.davils.kapi.runtime.server.operation
import com.davils.kapi.runtime.server.operation.Operation
import com.davils.kapi.runtime.server.reply.Reply
import com.davils.kapi.runtime.server.request.Request
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.routing.Route
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.runTestApplication
import io.ktor.server.websocket.WebSockets
import kotlinx.serialization.json.Json

internal const val OK: Int = 200

internal const val JSON: String = "application/json"

internal suspend fun serve(
    settings: ServerSettingsBuilder.() -> Unit = {},
    transport: Application.() -> Unit = {},
    routes: Route.() -> Unit,
    block: suspend ApplicationTestBuilder.() -> Unit,
) {
    runTestApplication {
        application {
            install(Kapi, settings)
            install(WebSockets) { maxFrameSize = ChannelLimits.Default.maxFrameBytes.toLong() }
            transport()
            routing { routes() }
        }
        block()
    }
}

internal suspend fun serveOne(
    handler: suspend (Request) -> Reply,
    settings: ServerSettingsBuilder.() -> Unit = {},
    block: suspend ApplicationTestBuilder.() -> Unit,
) {
    val served = Operation(name = "probe", method = "POST", path = "/probe", handler = handler)
    serve(settings = settings, routes = { operation(served) }, block = block)
}

internal suspend fun problemOf(response: HttpResponse): Problem =
    Json.decodeFromString(ProblemSerializer, response.bodyAsText())
