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
import com.davils.kapi.spec.openapi.model.parameter.Header
import com.davils.kapi.spec.openapi.model.parameter.ParameterStyle
import com.davils.kore.core.annotation.DavilsDsl

/**
 * The builder of an [Encoding], filled by the readers of kapi.
 *
 * A builder is not safe to share between threads; the encoding it builds is.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class EncodingBuilder internal constructor() : ElementBuilder<Encoding>() {
    override val subject: String get() = "openapi encoding"

    /**
     * The value of `contentType`, the media types of the part.
     *
     * @since 1.0.0
     */
    public var contentType: String? = null
        set(value) {
            checkOpen()
            field = value
        }

    internal val headers: MutableMap<String, Referable<Header>> = linkedMapOf()

    /**
     * The value of `style`, the style the value is serialized in.
     *
     * @since 1.0.0
     */
    public var style: ParameterStyle? = null
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The value of `explode`; `null` to take the default of the style.
     *
     * @since 1.0.0
     */
    public var isExploded: Boolean? = null
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The value of `allowReserved`, whether reserved characters stay unencoded.
     *
     * @since 1.0.0
     */
    public var isReservedAllowed: Boolean = false
        set(value) {
            checkOpen()
            field = value
        }

    internal val encoding: MutableMap<String, Encoding> = linkedMapOf()

    internal val prefixEncoding: MutableList<Encoding> = mutableListOf()

    /**
     * The value of `itemEncoding`, the encoding of every item of a nested part that the prefix encodings do not
     * cover, written from OpenAPI 3.2 on.
     *
     * @since 1.0.0
     */
    public var itemEncoding: Encoding? = null
        set(value) {
            checkOpen()
            field = value
        }

    override fun assemble(): Encoding = Encoding(this)
}
