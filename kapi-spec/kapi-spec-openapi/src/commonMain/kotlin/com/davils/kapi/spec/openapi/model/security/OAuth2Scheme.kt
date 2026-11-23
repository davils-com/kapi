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

public class OAuth2Scheme internal constructor(builder: OAuth2SchemeBuilder) :
    SecurityScheme(builder) {
    public override val description: String? = builder.description

    /**
     * Whether the scheme is deprecated and should no longer be used; `false` before OpenAPI 3.2.
     *
     * @since 1.0.0
     */
    public override val isDeprecated: Boolean = builder.isDeprecated

    public val flows: OAuthFlows = builder.flows

    /**
     * The address of the OAuth 2.0 authorization server metadata of RFC 8414; `null` before OpenAPI 3.2.
     *
     * The address is not checked when it is read; a client fetches it only through its destination policy.
     *
     * @since 1.0.0
     */
    public val oauth2MetadataUrl: String? = builder.oauth2MetadataUrl

    override val identity: List<Any?> get() = listOf(description, flows, oauth2MetadataUrl, isDeprecated)
}
