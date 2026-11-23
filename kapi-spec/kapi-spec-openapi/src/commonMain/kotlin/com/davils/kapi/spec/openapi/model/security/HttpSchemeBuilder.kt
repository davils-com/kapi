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

package com.davils.kapi.spec.openapi.model.security

import com.davils.kapi.spec.core.model.ElementBuilder
import com.davils.kore.core.annotation.DavilsDsl

/**
 * The builder of an [HttpScheme], filled by the readers of kapi.
 *
 * A builder is not safe to share between threads; the scheme it builds is.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class HttpSchemeBuilder internal constructor() : ElementBuilder<HttpScheme>() {
    override val subject: String get() = "openapi http scheme"

    /**
     * The value of `description`, the description of the scheme.
     *
     * @since 1.0.0
     */
    public var description: String? = null
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The value of `deprecated`, whether the scheme should no longer be used, written from OpenAPI 3.2 on.
     *
     * @since 1.0.0
     */
    public var isDeprecated: Boolean = false
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The value of `scheme`, the HTTP authentication scheme; empty until it is set.
     *
     * @since 1.0.0
     */
    public var scheme: String = ""
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The value of `bearerFormat`, a hint how a bearer token is formatted.
     *
     * @since 1.0.0
     */
    public var bearerFormat: String? = null
        set(value) {
            checkOpen()
            field = value
        }

    override fun assemble(): HttpScheme = HttpScheme(this)
}
