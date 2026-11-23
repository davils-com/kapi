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

import com.davils.kapi.spec.core.model.Element

/**
 * One flow of OAuth 2.0: its addresses and the scopes it offers.
 *
 * The addresses are not checked when they are read; a client calls them only through its destination policy.
 *
 * @param builder the builder whose state the element takes over.
 * @since 1.0.0
 */
public class OAuthFlow internal constructor(builder: OAuthFlowBuilder) : Element(builder) {
    /**
     * The address of the authorization endpoint, for implicit and authorization code.
     *
     * @since 1.0.0
     */
    public val authorizationUrl: String? = builder.authorizationUrl

    /**
     * The address of the token endpoint, for every flow but the implicit one.
     *
     * @since 1.0.0
     */
    public val tokenUrl: String? = builder.tokenUrl

    /**
     * The address to refresh tokens at.
     *
     * @since 1.0.0
     */
    public val refreshUrl: String? = builder.refreshUrl

    /**
     * The scopes the flow offers, each with its description.
     *
     * @since 1.0.0
     */
    public val availableScopes: Map<String, String> = builder.availableScopes.toMap()

    override val identity: List<Any?> get() = listOf(authorizationUrl, tokenUrl, refreshUrl, availableScopes)
}
