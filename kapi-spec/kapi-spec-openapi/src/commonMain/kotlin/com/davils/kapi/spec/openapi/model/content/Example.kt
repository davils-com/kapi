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

/**
 * An example object: a sample value of a parameter, a header or a media type, with a description.
 *
 * The element is immutable and safe to share between threads. A reading reports an example that gives its value
 * more than once, such as [value] beside [externalValue].
 *
 * @since 1.0.0
 */
public class Example internal constructor(builder: ExampleBuilder) : Element(builder) {
    /**
     * The `summary` of the example in a few words; `null` when the document leaves it out.
     *
     * @since 1.0.0
     */
    public val summary: String? = builder.summary

    /**
     * The `description` of the example, which may use CommonMark; `null` when the document leaves it out.
     *
     * @since 1.0.0
     */
    public val description: String? = builder.description

    /**
     * The example itself, the `value` as it was written; `null` when the document leaves it out.
     *
     * @since 1.0.0
     */
    public val value: Value? = builder.value

    /**
     * The `externalValue`: the URL of an example that cannot be written inline, as it was written and not fetched;
     * `null` when the document leaves it out.
     *
     * @since 1.0.0
     */
    public val externalValue: String? = builder.externalValue

    /**
     * The `dataValue`: the example as data that the schema validates, before it is serialized; `null` when the
     * document leaves it out and before OpenAPI 3.2.
     *
     * A reading reports it beside [value].
     *
     * @since 1.0.0
     */
    public val dataValue: Value? = builder.dataValue

    /**
     * The `serializedValue`: the example as it is serialized on the wire, such as a query string or a line of JSON;
     * `null` when the document leaves it out and before OpenAPI 3.2.
     *
     * A reading reports it beside [value] or [externalValue].
     *
     * @since 1.0.0
     */
    public val serializedValue: String? = builder.serializedValue

    override val identity: List<Any?>
        get() = listOf(summary, description, value, externalValue, dataValue, serializedValue)
}
