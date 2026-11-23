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

import com.davils.kapi.spec.core.model.Element
import com.davils.kapi.spec.core.model.Referable
import com.davils.kapi.spec.openapi.model.content.RequestBody
import com.davils.kapi.spec.openapi.model.content.Responses
import com.davils.kapi.spec.openapi.model.info.ExternalDocs
import com.davils.kapi.spec.openapi.model.parameter.Parameter
import com.davils.kapi.spec.openapi.model.security.SecurityRequirement
import com.davils.kapi.spec.openapi.model.server.Server

public class Operation internal constructor(builder: OperationBuilder) :
    Element(builder) {
    public val operationId: String? = builder.operationId

    public val summary: String? = builder.summary

    public val description: String? = builder.description

    public val tags: List<String> = builder.tags.toList()

    public val externalDocs: ExternalDocs? = builder.externalDocs

    public val parameters: List<Referable<Parameter>> = builder.parameters.toList()

    public val requestBody: Referable<RequestBody>? = builder.requestBody

    public val responses: Responses = builder.responses

    public val callbacks: Map<String, Referable<Callback>> = builder.callbacks.toMap()

    public val isDeprecated: Boolean = builder.isDeprecated

    public val security: List<SecurityRequirement>? = builder.security?.toList()

    public val servers: List<Server> = builder.servers.toList()

    override val identity: List<Any?>
        get() = listOf(
            operationId,
            summary,
            description,
            tags,
            externalDocs,
            parameters,
            requestBody,
            responses,
            callbacks,
            isDeprecated,
            security,
            servers,
        )
}
