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
import com.davils.kore.core.annotation.DavilsDsl

/**
 * The builder of a [RequestBody], filled by the readers of kapi.
 *
 * A builder is not safe to share between threads; the request body it builds is.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class RequestBodyBuilder internal constructor() :
    ElementBuilder<RequestBody>() {
    override val subject: String get() = "openapi request body"

    /**
     * The value of `description`, the description of the body.
     *
     * @since 1.0.0
     */
    public var description: String? = null

    internal val content: MutableMap<String, Referable<MediaType>> = linkedMapOf()

    /**
     * The value of `required`, whether a request must carry the body.
     *
     * @since 1.0.0
     */
    public var isRequired: Boolean = false

    override fun assemble(): RequestBody = RequestBody(this)
}
