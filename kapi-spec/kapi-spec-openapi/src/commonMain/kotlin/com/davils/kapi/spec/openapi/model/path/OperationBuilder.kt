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

package com.davils.kapi.spec.openapi.model.path

import com.davils.kapi.spec.core.model.ElementBuilder
import com.davils.kapi.spec.core.model.Referable
import com.davils.kapi.spec.openapi.model.content.RequestBody
import com.davils.kapi.spec.openapi.model.content.Responses
import com.davils.kapi.spec.openapi.model.content.ResponsesBuilder
import com.davils.kapi.spec.openapi.model.info.ExternalDocs
import com.davils.kapi.spec.openapi.model.parameter.Parameter
import com.davils.kapi.spec.openapi.model.security.SecurityRequirement
import com.davils.kapi.spec.openapi.model.server.Server
import com.davils.kore.core.annotation.DavilsDsl

@DavilsDsl
public class OperationBuilder internal constructor() : ElementBuilder<Operation>() {
    override val subject: String get() = "openapi operation"

    public var operationId: String? = null

    public var summary: String? = null

    public var description: String? = null

    internal val tags: MutableList<String> = mutableListOf()

    public var externalDocs: ExternalDocs? = null

    internal val parameters: MutableList<Referable<Parameter>> = mutableListOf()

    public var requestBody: Referable<RequestBody>? = null

    public var responses: Responses = ResponsesBuilder().build()

    internal val callbacks: MutableMap<String, Referable<Callback>> = linkedMapOf()

    public var isDeprecated: Boolean = false

    public var security: List<SecurityRequirement>? = null

    internal val servers: MutableList<Server> = mutableListOf()

    override fun assemble(): Operation = Operation(this)
}
