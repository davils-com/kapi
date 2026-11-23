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

/**
 * A header object: a header of a response or of a `multipart` part, described like a header parameter without
 * a name or a location, which the key it is declared under gives.
 *
 * The element is immutable and safe to share between threads. A reading reports a header that has both or neither
 * of [schema] and [content], or a [content] with more than one media type.
 *
 * @since 1.0.0
 */
public class Header internal constructor(builder: HeaderBuilder) : Element(builder) {
    /**
     * The `description` of the header, which may use CommonMark; `null` when the document leaves it out.
     *
     * @since 1.0.0
     */
    public val description: String? = builder.description

    /**
     * Whether the header is `required`; `false` when the document leaves it out.
     *
     * @since 1.0.0
     */
    public val isRequired: Boolean = builder.isRequired

    /**
     * Whether the header is `deprecated` and should no longer be used; `false` when the document leaves it out.
     *
     * @since 1.0.0
     */
    public val isDeprecated: Boolean = builder.isDeprecated

    /**
     * The `style` the value is serialized in, which OpenAPI allows only as [ParameterStyle.Simple]; `null` when the
     * document leaves it out.
     *
     * @since 1.0.0
     */
    public val style: ParameterStyle? = builder.style

    /**
     * Whether arrays and objects are `explode`d into a value per item or property; `null` when the document leaves it
     * out, which means `false`.
     *
     * @since 1.0.0
     */
    public val isExploded: Boolean? = builder.isExploded

    /**
     * The `schema` of the value; `null` when the header is described by [content] instead.
     *
     * @since 1.0.0
     */
    public val schema: Schema? = builder.schema

    /**
     * The `content` of the value by media type, which holds exactly one entry when the header is described by it;
     * empty when the header is described by [schema].
     *
     * @since 1.0.0
     */
    public val content: Map<String, Referable<MediaType>> = builder.content.toMap()

    /**
     * The `example` of the value; `null` when the document leaves it out. A reading reports it beside [examples].
     *
     * @since 1.0.0
     */
    public val example: Value? = builder.example

    /**
     * The `examples` of the value by name, inline or by reference; empty when the document declares none.
     *
     * @since 1.0.0
     */
    public val examples: Map<String, Referable<Example>> = builder.examples.toMap()

    override val identity: List<Any?>
        get() = listOf(
            description,
            isRequired,
            isDeprecated,
            style,
            isExploded,
            schema,
            content,
            example,
            examples,
        )
}
