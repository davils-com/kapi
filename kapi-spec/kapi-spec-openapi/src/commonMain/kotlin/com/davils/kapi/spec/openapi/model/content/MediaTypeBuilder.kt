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

import com.davils.kapi.spec.core.model.ElementBuilder
import com.davils.kapi.spec.core.model.Referable
import com.davils.kapi.spec.core.value.Value
import com.davils.kapi.spec.openapi.model.schema.Schema
import com.davils.kore.core.annotation.DavilsDsl

/**
 * The builder of a [MediaType], filled by the readers of kapi.
 *
 * A builder is not safe to share between threads; the media type it builds is.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class MediaTypeBuilder internal constructor() : ElementBuilder<MediaType>() {
    override val subject: String get() = "openapi media type"

    /**
     * The value of `schema`, the schema of the whole content.
     *
     * @since 1.0.0
     */
    public var schema: Schema? = null

    /**
     * The value of `itemSchema`, the schema of each item of a sequential media type, written from OpenAPI 3.2 on.
     *
     * @since 1.0.0
     */
    public var itemSchema: Schema? = null

    /**
     * The value of `example`, an example of the content.
     *
     * @since 1.0.0
     */
    public var example: Value? = null

    internal val examples: MutableMap<String, Referable<Example>> = linkedMapOf()

    internal val encoding: MutableMap<String, Encoding> = linkedMapOf()

    internal val prefixEncoding: MutableList<Encoding> = mutableListOf()

    /**
     * The value of `itemEncoding`, the encoding of every item that the prefix encodings do not cover, written from
     * OpenAPI 3.2 on.
     *
     * @since 1.0.0
     */
    public var itemEncoding: Encoding? = null

    override fun assemble(): MediaType = MediaType(this)
}
