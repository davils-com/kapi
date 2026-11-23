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

@file:OptIn(DavilsInternal::class)

package com.davils.kapi.spec.openapi.model.content

import com.davils.kapi.spec.core.collection.frozen
import com.davils.kapi.spec.core.model.Element
import com.davils.kapi.spec.core.model.Referable
import com.davils.kapi.spec.openapi.model.parameter.Header
import com.davils.kore.core.annotation.DavilsInternal

/**
 * A response object: one response of an operation, with its headers, its content and its links.
 *
 * The element is immutable and safe to share between threads.
 *
 * @since 1.0.0
 */
public class Response internal constructor(builder: ResponseBuilder) :
    Element(builder) {
    /**
     * The `summary` of the response in a few words; `null` when the document leaves it out and before OpenAPI 3.2.
     *
     * @since 1.0.0
     */
    public val summary: String? = builder.summary

    /**
     * The `description` of the response, which may use CommonMark; `null` when the document leaves it out, which
     * OpenAPI allows from 3.2 on and a reading of an older document reports.
     *
     * @since 1.0.0
     */
    public val description: String? = builder.description

    /**
     * The `headers` of the response by name, inline or by reference; empty when the document declares none. OpenAPI
     * ignores a `Content-Type` header here, which [content] describes.
     *
     * @since 1.0.0
     */
    public val headers: Map<String, Referable<Header>> = builder.headers.frozen()

    /**
     * The `content` of the response by media type, such as `application/json`, or by media type range; empty for a
     * response without a body. A media type is given by reference only from OpenAPI 3.2 on.
     *
     * A reading reports a key that is no media type.
     *
     * @since 1.0.0
     */
    public val content: Map<String, Referable<MediaType>> = builder.content.frozen()

    /**
     * The `links` from the response to other operations by name, inline or by reference; empty when the document
     * declares none.
     *
     * @since 1.0.0
     */
    public val links: Map<String, Referable<Link>> = builder.links.frozen()

    override val identity: List<Any?> get() = listOf(summary, description, headers, content, links)
}
