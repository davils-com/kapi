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

package com.davils.kapi.runtime.testing.server

import com.davils.kapi.runtime.server.operation.Operation
import com.davils.kapi.runtime.server.operation.Routes
import com.davils.kapi.runtime.server.reply.Reply
import com.davils.kapi.runtime.server.request.Request
import com.davils.kapi.runtime.server.request.RequestBuilder
import com.davils.kore.core.principal.withPrincipal

/**
 * Calls the handler of the operation [operation] of these routes with [request], as a mounted server would after it
 * read, verified and authorized the request, and returns its reply.
 *
 * No Ktor, no connection and no check of the server run: the test decides what the request carries, including its
 * principal, which the handler also finds as `currentPrincipal()` of Kore. The handler runs in the coroutine of the
 * caller, and whatever it throws reaches the caller unchanged, so a test sees the failure a mounted server would hide
 * behind a problem with status 500.
 *
 * @receiver the routes, such as a generated `PetsRoutes`.
 * @param operation the `operationId` of the operation.
 * @param request the request, such as one of [Request.of].
 * @return the reply of the handler.
 * @throws IllegalArgumentException if the routes have no operation [operation].
 * @since 1.0.0
 */
public suspend fun Routes.call(operation: String, request: Request): Reply {
    val found = operationOf(operation)
    val principal = request.principal ?: return found.handler(request)
    return withPrincipal(principal) { found.handler(request) }
}

/**
 * Calls the handler of the operation [operation] of these routes with a request on its method and [path] that
 * [configure] completes, and returns its reply; see [call] with a request.
 *
 * @receiver the routes, such as a generated `PetsRoutes`.
 * @param operation the `operationId` of the operation.
 * @param path the path of the request; the path template of the operation by default, which suits an operation
 * without path parameters.
 * @param configure sets the query, the path parameters, the header fields, the body, the principal and the server.
 * @return the reply of the handler.
 * @throws IllegalArgumentException if the routes have no operation [operation] or the request is invalid as
 * [Request.of] describes.
 * @since 1.0.0
 */
public suspend fun Routes.call(
    operation: String,
    path: String? = null,
    configure: RequestBuilder.() -> Unit = {},
): Reply {
    val found = operationOf(operation)
    val target = path ?: found.path
    val request = Request.of(operation = found.name, method = found.method, path = target, configure = configure)
    return call(operation, request)
}

private fun Routes.operationOf(name: String): Operation {
    val found = operations.firstOrNull { candidate -> candidate.name == name }
    return requireNotNull(found) { "The routes have no operation of that name" }
}
