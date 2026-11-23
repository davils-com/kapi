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

public class MediaType internal constructor(builder: MediaTypeBuilder) :
    Element(builder) {
    public val schema: Schema? = builder.schema

    /**
     * The schema of each item of a sequential media type, such as an event of `text/event-stream` or a line of
     * `application/jsonl`; `null` before OpenAPI 3.2.
     *
     * @since 1.0.0
     */
    public val itemSchema: Schema? = builder.itemSchema

    public val example: Value? = builder.example

    public val examples: Map<String, Referable<Example>> = builder.examples.toMap()

    public val encoding: Map<String, Encoding> = builder.encoding.toMap()

    /**
     * The encodings of the leading items of a sequential or multipart media type, by position; empty before
     * OpenAPI 3.2.
     *
     * A reading reports it beside [encoding].
     *
     * @since 1.0.0
     */
    public val prefixEncoding: List<Encoding> = builder.prefixEncoding.toList()

    /**
     * The encoding of every item of a sequential or multipart media type that [prefixEncoding] does not cover;
     * `null` before OpenAPI 3.2.
     *
     * A reading reports it beside [encoding].
     *
     * @since 1.0.0
     */
    public val itemEncoding: Encoding? = builder.itemEncoding

    override val identity: List<Any?>
        get() = listOf(schema, itemSchema, example, examples, encoding, prefixEncoding, itemEncoding)
}
