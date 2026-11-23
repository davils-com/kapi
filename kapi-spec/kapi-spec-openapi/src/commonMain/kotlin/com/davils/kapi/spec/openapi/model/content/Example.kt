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

package com.davils.kapi.spec.openapi.model.content

import com.davils.kapi.spec.core.model.Element
import com.davils.kapi.spec.core.value.Value

public class Example internal constructor(builder: ExampleBuilder) : Element(builder) {
    public val summary: String? = builder.summary

    public val description: String? = builder.description

    public val value: Value? = builder.value

    public val externalValue: String? = builder.externalValue

    /**
     * The example as data that the schema validates, before it is serialized; `null` before OpenAPI 3.2.
     *
     * A reading reports it beside [value].
     *
     * @since 1.0.0
     */
    public val dataValue: Value? = builder.dataValue

    /**
     * The example as it is serialized on the wire, such as a query string or a line of JSON; `null` before
     * OpenAPI 3.2.
     *
     * A reading reports it beside [value] or [externalValue].
     *
     * @since 1.0.0
     */
    public val serializedValue: String? = builder.serializedValue

    override val identity: List<Any?>
        get() = listOf(summary, description, value, externalValue, dataValue, serializedValue)
}
