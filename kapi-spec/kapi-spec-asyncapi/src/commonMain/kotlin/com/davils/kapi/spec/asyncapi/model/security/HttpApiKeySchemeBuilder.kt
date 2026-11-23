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

package com.davils.kapi.spec.asyncapi.model.security

import com.davils.kapi.spec.core.model.ElementBuilder
import com.davils.kore.core.annotation.DavilsDsl

/**
 * The builder of [HttpApiKeyScheme]: collects its fields while a reader of kapi reads it.
 *
 * A builder is not safe to share between threads; the element it builds is.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class HttpApiKeySchemeBuilder internal constructor() : ElementBuilder<HttpApiKeyScheme>() {
    override val subject: String get() = "asyncapi http api key scheme"

    /**
     * A description of the scheme, which may use CommonMark.
     *
     * @since 1.0.0
     */
    public var description: String? = null

    /**
     * The name of the query parameter, the header or the cookie.
     *
     * @since 1.0.0
     */
    public var name: String = ""

    /**
     * Where the key is sent.
     *
     * @since 1.0.0
     */
    public var placement: HttpApiKeyPlacement = HttpApiKeyPlacement.Header

    override fun assemble(): HttpApiKeyScheme = HttpApiKeyScheme(this)
}
