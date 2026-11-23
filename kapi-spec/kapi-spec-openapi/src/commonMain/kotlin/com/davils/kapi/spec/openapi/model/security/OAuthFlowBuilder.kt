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
 * The builder of an [OAuthFlow], filled by the readers of kapi.
 *
 * A builder is not safe to share between threads; the flow it builds is.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class OAuthFlowBuilder internal constructor() : ElementBuilder<OAuthFlow>() {
    override val subject: String get() = "openapi oauth flow"

    /**
     * The value of `authorizationUrl`, the authorization endpoint.
     *
     * @since 1.0.0
     */
    public var authorizationUrl: String? = null

    /**
     * The value of `deviceAuthorizationUrl`, the device authorization endpoint, written from OpenAPI 3.2 on.
     *
     * @since 1.0.0
     */
    public var deviceAuthorizationUrl: String? = null

    /**
     * The value of `tokenUrl`, the token endpoint.
     *
     * @since 1.0.0
     */
    public var tokenUrl: String? = null

    /**
     * The value of `refreshUrl`, the endpoint that refreshes a token.
     *
     * @since 1.0.0
     */
    public var refreshUrl: String? = null

    internal val scopes: MutableMap<String, String> = linkedMapOf()

    override fun assemble(): OAuthFlow = OAuthFlow(this)
}
