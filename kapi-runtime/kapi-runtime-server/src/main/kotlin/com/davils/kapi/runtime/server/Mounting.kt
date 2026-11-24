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

package com.davils.kapi.runtime.server

import com.davils.kapi.runtime.server.channel.ChannelEndpoint
import com.davils.kapi.runtime.server.internal.ChannelGate
import com.davils.kapi.runtime.server.internal.ChannelRunner
import com.davils.kapi.runtime.server.internal.Exchange
import com.davils.kapi.runtime.server.operation.Operation
import com.davils.kapi.runtime.server.operation.Routes
import io.ktor.http.HttpMethod
import io.ktor.server.application.plugin
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.route
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket

private const val UNBOUNDED_FRAMES: String =
    "The WebSockets plugin reads frames larger than the maxFrameBytes of the channel; lower its maxFrameSize"

/**
 * Serves every operation and channel of [routes] below this route, such as `routing { mount(PetsRoutes(service)) }`.
 *
 * @receiver the route to mount below.
 * @param routes the generated route class.
 * @throws io.ktor.server.application.MissingApplicationPluginException if [Kapi] is not installed, or `WebSockets`
 * of Ktor is not installed and [routes] has channels.
 * @throws IllegalArgumentException if [routes] has channels and the `maxFrameSize` of the `WebSockets` plugin is
 * larger than the `maxFrameBytes` of one of them.
 * @since 1.0.0
 */
public fun Route.mount(routes: Routes) {
    routes.operations.forEach { operation -> operation(operation) }
    routes.channels.forEach { endpoint -> channel(endpoint) }
}

/**
 * Serves [operation] on its method and path template below this route.
 *
 * The method may be any token, such as `QUERY` or a method of the `additionalOperations` of OpenAPI 3.2.
 *
 * @receiver the route to mount below.
 * @param operation the operation.
 * @throws io.ktor.server.application.MissingApplicationPluginException if [Kapi] is not installed.
 * @since 1.0.0
 */
public fun Route.operation(operation: Operation) {
    val exchange = Exchange(application.plugin(Kapi), operation)
    route(operation.path, HttpMethod.parse(operation.method)) {
        handle { exchange.serve(call) }
    }
}

/**
 * Serves the WebSocket channel [endpoint] on its path below this route, as a `GET` that upgrades.
 *
 * @receiver the route to mount below.
 * @param endpoint the channel.
 * Ktor reads the first frames of a session before a limit set on the session applies, so the `WebSockets` plugin
 * itself must read no frame larger than [ChannelEndpoint.limits] allows, such as
 * `install(WebSockets) { maxFrameSize = 65_536 }`.
 *
 * @throws io.ktor.server.application.MissingApplicationPluginException if [Kapi] or `WebSockets` of Ktor is not
 * installed.
 * @throws IllegalArgumentException if the `maxFrameSize` of the `WebSockets` plugin is larger than the
 * `maxFrameBytes` of [endpoint].
 * @since 1.0.0
 */
public fun Route.channel(endpoint: ChannelEndpoint<*, *>) {
    val sockets = application.plugin(WebSockets)
    require(sockets.maxFrameSize <= endpoint.limits.maxFrameBytes) { UNBOUNDED_FRAMES }
    val runner = runnerOf(application.plugin(Kapi), endpoint)
    route(endpoint.path, HttpMethod.Get) {
        install(ChannelGate) { admit = runner::admit }
        endpoint.protocols.ifEmpty { listOf(null) }.forEach { protocol ->
            webSocket(protocol) { runner.run(this, protocol) }
        }
        handle { runner.refuseProtocol(call) }
    }
}

private fun <Receive : Any, Send : Any> runnerOf(
    server: ApiServer,
    endpoint: ChannelEndpoint<Receive, Send>,
): ChannelRunner<Receive, Send> = ChannelRunner(server, endpoint)
