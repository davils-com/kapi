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
import com.davils.kapi.spec.openapi.model.parameter.Header
import com.davils.kapi.spec.openapi.model.parameter.ParameterStyle

/**
 * An encoding object: how one property or item of a `multipart` or `application/x-www-form-urlencoded` body is
 * serialized.
 *
 * The element is immutable and safe to share between threads.
 *
 * @since 1.0.0
 */
public class Encoding internal constructor(builder: EncodingBuilder) :
    Element(builder) {
    /**
     * The `contentType` of the part, a media type or a comma-separated list of them such as `image/png, image/jpeg`;
     * `null` when the document leaves it out, so that the default for the type of the property applies.
     *
     * @since 1.0.0
     */
    public val contentType: String? = builder.contentType

    /**
     * The `headers` of a `multipart` part by name, inline or by reference; empty when the encoding declares none.
     * OpenAPI ignores a `Content-Type` header here, which [contentType] describes.
     *
     * @since 1.0.0
     */
    public val headers: Map<String, Referable<Header>> = builder.headers.toMap()

    /**
     * The `style` the value is serialized in, as for a query parameter; `null` when the document leaves it out.
     *
     * @since 1.0.0
     */
    public val style: ParameterStyle? = builder.style

    /**
     * Whether arrays and objects are `explode`d into a pair per item or property; `null` when the document leaves it
     * out, so that the default of the style applies, which is `true` for [ParameterStyle.Form] and `false` otherwise.
     *
     * @since 1.0.0
     */
    public val isExploded: Boolean? = builder.isExploded

    /**
     * Whether the value may hold the reserved characters of RFC 3986 unencoded, from `allowReserved`; `false` when the
     * document leaves it out.
     *
     * @since 1.0.0
     */
    public val isReservedAllowed: Boolean = builder.isReservedAllowed

    /**
     * The `encoding` of the properties of a nested `multipart` part, by name; empty when the document declares none
     * and before OpenAPI 3.2.
     *
     * A reading reports it beside [prefixEncoding] or [itemEncoding].
     *
     * @since 1.0.0
     */
    public val nestedEncoding: Map<String, Encoding> = builder.encoding.toMap()

    /**
     * The `prefixEncoding`: the encodings of the leading items of a nested part, by position; empty when the
     * document declares none and before OpenAPI 3.2.
     *
     * @since 1.0.0
     */
    public val prefixEncoding: List<Encoding> = builder.prefixEncoding.toList()

    /**
     * The `itemEncoding`: the encoding of every item of a nested part that [prefixEncoding] does not cover; `null`
     * when the document leaves it out and before OpenAPI 3.2.
     *
     * @since 1.0.0
     */
    public val itemEncoding: Encoding? = builder.itemEncoding

    override val identity: List<Any?>
        get() = listOf(
            contentType,
            headers,
            style,
            isExploded,
            isReservedAllowed,
            nestedEncoding,
            prefixEncoding,
            itemEncoding,
        )
}
