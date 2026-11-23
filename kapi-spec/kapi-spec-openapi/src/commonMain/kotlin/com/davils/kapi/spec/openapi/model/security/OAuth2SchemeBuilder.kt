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

@DavilsDsl
public class OAuth2SchemeBuilder internal constructor() :
    ElementBuilder<OAuth2Scheme>() {
    override val subject: String get() = "openapi oauth2 scheme"

    public var description: String? = null

    /**
     * Whether the scheme is deprecated and should no longer be used, written from OpenAPI 3.2 on.
     *
     * @since 1.0.0
     */
    public var isDeprecated: Boolean = false

    public var flows: OAuthFlows = OAuthFlowsBuilder().build()

    /**
     * The address of the authorization server metadata, written from OpenAPI 3.2 on.
     *
     * @since 1.0.0
     */
    public var oauth2MetadataUrl: String? = null

    override fun assemble(): OAuth2Scheme = OAuth2Scheme(this)
}
