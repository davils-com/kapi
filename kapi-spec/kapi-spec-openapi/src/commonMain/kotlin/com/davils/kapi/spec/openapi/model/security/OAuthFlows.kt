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

/**
 * The OAuth flows object of an [OAuth2Scheme]: the grants of OAuth 2.0 it supports, each `null` when the document
 * leaves it out.
 *
 * The element is immutable and safe to share between threads.
 *
 * @since 1.0.0
 */
public class OAuthFlows internal constructor(builder: OAuthFlowsBuilder) :
    Element(builder) {
    /**
     * The `implicit` flow; `null` when the document leaves it out.
     *
     * @since 1.0.0
     */
    public val implicit: OAuthFlow? = builder.implicit

    /**
     * The `password` flow, the resource owner password credentials grant; `null` when the document leaves it out.
     *
     * @since 1.0.0
     */
    public val password: OAuthFlow? = builder.password

    /**
     * The `clientCredentials` flow; `null` when the document leaves it out.
     *
     * @since 1.0.0
     */
    public val clientCredentials: OAuthFlow? = builder.clientCredentials

    /**
     * The `authorizationCode` flow; `null` when the document leaves it out.
     *
     * @since 1.0.0
     */
    public val authorizationCode: OAuthFlow? = builder.authorizationCode

    /**
     * The `deviceAuthorization` flow of RFC 8628; `null` when the document leaves it out and before OpenAPI 3.2.

     *
     * @since 1.0.0
     */
    public val deviceAuthorization: OAuthFlow? = builder.deviceAuthorization

    override val identity: List<Any?>
        get() = listOf(implicit, password, clientCredentials, authorizationCode, deviceAuthorization)
}
