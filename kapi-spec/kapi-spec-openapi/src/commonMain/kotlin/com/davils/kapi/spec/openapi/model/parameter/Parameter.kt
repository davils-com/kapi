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

package com.davils.kapi.spec.openapi.model.parameter

import com.davils.kapi.spec.core.model.Element
import com.davils.kapi.spec.core.model.Referable
import com.davils.kapi.spec.core.value.Value
import com.davils.kapi.spec.openapi.model.content.Example
import com.davils.kapi.spec.openapi.model.content.MediaType
import com.davils.kapi.spec.openapi.model.schema.Schema

public class Parameter internal constructor(builder: ParameterBuilder) :
    Element(builder) {
    public val name: String = builder.name

    public val placement: ParameterPlacement = builder.placement

    public val description: String? = builder.description

    public val isRequired: Boolean = builder.isRequired

    public val isDeprecated: Boolean = builder.isDeprecated

    public val isEmptyValueAllowed: Boolean = builder.isEmptyValueAllowed

    public val style: ParameterStyle? = builder.style

    public val isExploded: Boolean? = builder.isExploded

    public val isReservedAllowed: Boolean = builder.isReservedAllowed

    public val schema: Schema? = builder.schema

    public val content: Map<String, MediaType> = builder.content.toMap()

    public val example: Value? = builder.example

    public val examples: Map<String, Referable<Example>> = builder.examples.toMap()

    override val identity: List<Any?>
        get() = listOf(
            name,
            placement,
            description,
            isRequired,
            isDeprecated,
            isEmptyValueAllowed,
            style,
            isExploded,
            isReservedAllowed,
            schema,
            content,
            example,
            examples,
        )
}
