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

/**
 * A request body object: the body an operation takes, in one or more media types.
 *
 * The element is immutable and safe to share between threads.
 *
 * @since 1.0.0
 */
public class RequestBody internal constructor(builder: RequestBodyBuilder) :
    Element(builder) {
    /**
     * The `description` of the body, which may use CommonMark; `null` when the document leaves it out.
     *
     * @since 1.0.0
     */
    public val description: String? = builder.description

    /**
     * The `content` of the body by media type, such as `application/json`, or by media type range, in the order of the
     * document; a media type is given by reference only from OpenAPI 3.2 on.
     *
     * A reading reports a key that is no media type.
     *
     * @since 1.0.0
     */
    public val content: Map<String, Referable<MediaType>> = builder.content.toMap()

    /**
     * Whether the body is `required` in a request; `false` when the document leaves it out.
     *
     * @since 1.0.0
     */
    public val isRequired: Boolean = builder.isRequired

    override val identity: List<Any?> get() = listOf(description, content, isRequired)
}
