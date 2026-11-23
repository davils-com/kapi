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

import com.davils.kapi.spec.core.model.Element
import com.davils.kapi.spec.core.model.ElementBuilder

/**
 * A security scheme object: how a client proves who it is to the API. The `type` of the scheme decides the
 * subclass: [ApiKeyScheme] for `apiKey`, [HttpScheme] for `http`, [MutualTlsScheme] for `mutualTLS`, [OAuth2Scheme]
 * for `oauth2` and [OpenIdConnectScheme] for `openIdConnect`.
 *
 * Every scheme is immutable and safe to share between threads. A reading reports a scheme without a type or with a
 * type it does not know, and leaves the scheme out.
 *
 * @param builder the builder whose state the scheme takes over.
 * @since 1.0.0
 */
public sealed class SecurityScheme(
    builder: ElementBuilder<*>,
) : Element(builder) {
    /**
     * The `description` of the scheme, which may use CommonMark; `null` when the document leaves it out.
     *
     * @since 1.0.0
     */
    public abstract val description: String?

    /**
     * Whether the scheme is `deprecated` and should no longer be used; `false` when the document leaves it out and
     * before OpenAPI 3.2.
     *
     * @since 1.0.0
     */
    public abstract val isDeprecated: Boolean
}
