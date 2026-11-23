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
import com.davils.kapi.spec.core.model.Reference
import com.davils.kapi.spec.openapi.model.parameter.Parameter
import com.davils.kapi.spec.openapi.model.server.Server

public class PathItem internal constructor(builder: PathItemBuilder) :
    Element(builder) {
    public val reference: Reference? = builder.reference

    public val summary: String? = builder.summary

    public val description: String? = builder.description

    public val operations: Map<Method, Operation> = builder.operations.toMap()

    public val servers: List<Server> = builder.servers.toList()

    public val parameters: List<Referable<Parameter>> = builder.parameters.toList()

    override val identity: List<Any?> get() = listOf(reference, summary, description, operations, servers, parameters)
}
