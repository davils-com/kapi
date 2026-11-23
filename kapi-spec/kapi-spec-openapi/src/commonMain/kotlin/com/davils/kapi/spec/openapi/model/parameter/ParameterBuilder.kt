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

import com.davils.kapi.spec.core.model.ElementBuilder
import com.davils.kapi.spec.core.model.Referable
import com.davils.kapi.spec.core.value.Value
import com.davils.kapi.spec.openapi.model.content.Example
import com.davils.kapi.spec.openapi.model.content.MediaType
import com.davils.kapi.spec.openapi.model.schema.Schema
import com.davils.kore.core.annotation.DavilsDsl

@DavilsDsl
public class ParameterBuilder internal constructor() : ElementBuilder<Parameter>() {
    override val subject: String get() = "openapi parameter"

    public var name: String = ""

    public var placement: ParameterPlacement = ParameterPlacement.Query

    public var description: String? = null

    public var isRequired: Boolean = false

    public var isDeprecated: Boolean = false

    public var isEmptyValueAllowed: Boolean = false

    public var style: ParameterStyle? = null

    public var isExploded: Boolean? = null

    public var isReservedAllowed: Boolean = false

    public var schema: Schema? = null

    internal val content: MutableMap<String, Referable<MediaType>> = linkedMapOf()

    public var example: Value? = null

    internal val examples: MutableMap<String, Referable<Example>> = linkedMapOf()

    override fun assemble(): Parameter = Parameter(this)
}
