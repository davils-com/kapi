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
import com.davils.kapi.spec.core.model.Referable
import com.davils.kapi.spec.core.value.Value
import com.davils.kapi.spec.openapi.model.schema.Schema

/**
 * A media type object: the schema and the examples of a body or a parameter in one media type, such as
 * `application/json`.
 *
 * The element is immutable and safe to share between threads.
 *
 * @since 1.0.0
 */
public class MediaType internal constructor(builder: MediaTypeBuilder) :
    Element(builder) {
    /**
     * The `schema` of the whole content; `null` when the document leaves it out.
     *
     * @since 1.0.0
     */
    public val schema: Schema? = builder.schema

    /**
     * The `itemSchema`: the schema of each item of a sequential media type, such as an event of `text/event-stream`
     * or a line of `application/jsonl`; `null` when the document leaves it out and before OpenAPI 3.2.
     *
     * @since 1.0.0
     */
    public val itemSchema: Schema? = builder.itemSchema

    /**
     * The `example` of the content; `null` when the document leaves it out. A reading reports it beside [examples].
     *
     * @since 1.0.0
     */
    public val example: Value? = builder.example

    /**
     * The `examples` of the content by name, inline or by reference; empty when the document declares none.
     *
     * @since 1.0.0
     */
    public val examples: Map<String, Referable<Example>> = builder.examples.toMap()

    /**
     * The `encoding` of the properties of a `multipart` or `application/x-www-form-urlencoded` body, by the name of the
     * property; empty when the document declares none.
     *
     * @since 1.0.0
     */
    public val encoding: Map<String, Encoding> = builder.encoding.toMap()

    /**
     * The `prefixEncoding`: the encodings of the leading items of a sequential or `multipart` media type, by
     * position; empty when the document declares none and before OpenAPI 3.2.
     *
     * A reading reports it beside [encoding].
     *
     * @since 1.0.0
     */
    public val prefixEncoding: List<Encoding> = builder.prefixEncoding.toList()

    /**
     * The `itemEncoding`: the encoding of every item of a sequential or `multipart` media type that
     * [prefixEncoding] does not cover; `null` when the document leaves it out and before OpenAPI 3.2.
     *
     * A reading reports it beside [encoding].
     *
     * @since 1.0.0
     */
    public val itemEncoding: Encoding? = builder.itemEncoding

    override val identity: List<Any?>
        get() = listOf(schema, itemSchema, example, examples, encoding, prefixEncoding, itemEncoding)
}
