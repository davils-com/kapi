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
 * The flows of an `oauth2` security scheme; at least one is declared.
 *
 * @param builder the builder whose state the element takes over.
 * @since 1.0.0
 */
public class OAuthFlows internal constructor(builder: OAuthFlowsBuilder) : Element(builder) {
    /**
     * The implicit flow.
     *
     * @since 1.0.0
     */
    public val implicit: OAuthFlow? = builder.implicit

    /**
     * The resource owner password flow.
     *
     * @since 1.0.0
     */
    public val password: OAuthFlow? = builder.password

    /**
     * The client credentials flow.
     *
     * @since 1.0.0
     */
    public val clientCredentials: OAuthFlow? = builder.clientCredentials

    /**
     * The authorization code flow.
     *
     * @since 1.0.0
     */
    public val authorizationCode: OAuthFlow? = builder.authorizationCode

    override val identity: List<Any?> get() = listOf(implicit, password, clientCredentials, authorizationCode)
}
