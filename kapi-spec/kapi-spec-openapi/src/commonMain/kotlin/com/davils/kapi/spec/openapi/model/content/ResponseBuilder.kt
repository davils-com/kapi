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

import com.davils.kapi.spec.core.collection.KeyedMap
import com.davils.kapi.spec.core.model.ElementBuilder
import com.davils.kapi.spec.core.model.Referable
import com.davils.kapi.spec.openapi.model.parameter.Header
import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.annotation.DavilsInternal

/**
 * The builder of a [Response], filled by the readers of kapi.
 *
 * A builder is not safe to share between threads; the response it builds is.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class ResponseBuilder internal constructor() : ElementBuilder<Response>() {
    override val subject: String get() = "openapi response"

    /**
     * The value of `summary`, a short summary of the response, written from OpenAPI 3.2 on.
     *
     * @since 1.0.0
     */
    public var summary: String? = null
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The value of `description`, the description of the response, which OpenAPI requires before 3.2.
     *
     * @since 1.0.0
     */
    public var description: String? = null
        set(value) {
            checkOpen()
            field = value
        }

    internal val headers: MutableMap<String, Referable<Header>> = KeyedMap()

    internal val content: MutableMap<String, Referable<MediaType>> = KeyedMap()

    internal val links: MutableMap<String, Referable<Link>> = KeyedMap()

    override fun assemble(): Response = Response(this)
}
