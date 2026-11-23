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
 * The builder of [OAuthFlows], filled by the readers of kapi.
 *
 * A builder is not safe to share between threads; the flows it builds are.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class OAuthFlowsBuilder internal constructor() : ElementBuilder<OAuthFlows>() {
    override val subject: String get() = "openapi oauth flows"

    /**
     * The value of `implicit`, the implicit flow.
     *
     * @since 1.0.0
     */
    public var implicit: OAuthFlow? = null

    /**
     * The value of `password`, the resource owner password credentials flow.
     *
     * @since 1.0.0
     */
    public var password: OAuthFlow? = null

    /**
     * The value of `clientCredentials`, the client credentials flow.
     *
     * @since 1.0.0
     */
    public var clientCredentials: OAuthFlow? = null

    /**
     * The value of `authorizationCode`, the authorization code flow.
     *
     * @since 1.0.0
     */
    public var authorizationCode: OAuthFlow? = null

    /**
     * The value of `deviceAuthorization`, the device authorization flow, written from OpenAPI 3.2 on.
     *
     * @since 1.0.0
     */
    public var deviceAuthorization: OAuthFlow? = null

    override fun assemble(): OAuthFlows = OAuthFlows(this)
}
