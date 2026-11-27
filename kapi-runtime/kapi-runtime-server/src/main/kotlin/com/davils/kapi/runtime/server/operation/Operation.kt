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

package com.davils.kapi.runtime.server.operation

import com.davils.kapi.runtime.server.internal.PathTemplates
import com.davils.kapi.runtime.server.reply.Reply
import com.davils.kapi.runtime.server.request.Request
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

/**
 * An operation of an API that a server serves: its name, its method, its path template, its security and its
 * handler.
 *
 * The method is any token of RFC 9110 §9, kept exactly as written because methods are case-sensitive: the methods of
 * OpenAPI in upper case, `QUERY` and the arbitrary methods of the `additionalOperations` of OpenAPI 3.2, so `purge`
 * and `PURGE` are served as different methods. The path template starts with `/` and names its parameters in
 * braces, such as `/pets/{petId}`; a parameter name consists of letters, digits, `_`, `-` and `.`, so no name can
 * smuggle a routing wildcard of Ktor into the template. The security lists the alternatives of OpenAPI 3.2 §4.26: an
 * empty list asks for nothing, and an empty alternative lets a caller in without credentials. A delivery is a webhook
 * or a callback the server receives, which is verified by the
 * [com.davils.kapi.runtime.server.webhook.DeliveryVerifier] of the server before it is authorized.
 *
 * The handler is code of the application: it runs contained, and whatever it throws becomes a problem with status 500
 * that reveals nothing of the failure. An operation is immutable.
 *
 * @property name the `operationId`, which names the operation in telemetry.
 * @property method the method, such as `GET` or `QUERY`.
 * @property path the path template, such as `/pets/{petId}`.
 * @param security the alternatives of security requirements, in order.
 * @property isDelivery whether the operation receives a webhook or a callback.
 * @property rateLimit the rate limit of the operation, which applies in addition to the rate limit of the server, or
 * `null` for none; since 1.2.0.
 * @property handler answers a request that passed the checks of the server.
 * @throws IllegalArgumentException if [name] is blank, [method] is no token, or [path] is no path
 * template.
 * @since 1.0.0
 */
public class Operation(
    public val name: String,
    public val method: String,
    public val path: String,
    security: List<List<SchemeUse>> = emptyList(),
    public val isDelivery: Boolean = false,
    public val rateLimit: OperationRateLimit? = null,
    public val handler: suspend (Request) -> Reply,
) {
    /**
     * The alternatives of security requirements, in order.
     *
     * @since 1.0.0
     */
    public val security: List<List<SchemeUse>> = security.map(List<SchemeUse>::toList)

    init {
        require(name.isNotBlank()) { "An operation has a name" }
        require(PathTemplates.isMethod(method)) { "The method of an operation is a token" }
        require(PathTemplates.isTemplate(path)) { "The path of an operation is a template such as /pets/{petId}" }
    }

    /**
     * Creates an operation without a rate limit of its own; kept for code compiled against kapi 1.1.
     *
     * @param name the `operationId`.
     * @param method the method.
     * @param path the path template.
     * @param security the alternatives of security requirements, in order.
     * @param isDelivery whether the operation receives a webhook or a callback.
     * @param handler answers a request that passed the checks of the server.
     * @throws IllegalArgumentException if [name] is blank, [method] is no token, or [path] is no path template.
     * @since 1.0.0
     */
    @Deprecated(
        message = "Kept for binary compatibility; use the constructor with a rate limit",
        replaceWith = ReplaceWith("Operation(name, method, path, security, isDelivery, null, handler)"),
        level = DeprecationLevel.HIDDEN,
    )
    public constructor(
        name: String,
        method: String,
        path: String,
        security: List<List<SchemeUse>> = emptyList(),
        isDelivery: Boolean = false,
        handler: suspend (Request) -> Reply,
    ) : this(
        name = name,
        method = method,
        path = path,
        security = security,
        isDelivery = isDelivery,
        rateLimit = null,
        handler = handler,
    )

    @OptIn(DavilsInternal::class)
    override fun toString(): String = "Operation(${SafeText.text(name)}, $method ${SafeText.text(path)})"
}
