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

/**
 * A security scheme of type `oauth2`: the flows of OAuth 2.0 and the scopes it needs.
 *
 * @param builder the builder whose state the element takes over.
 * @since 1.0.0
 */
public class OAuth2Scheme internal constructor(builder: OAuth2SchemeBuilder) : SecurityScheme(builder) {
    override val type: SecuritySchemeType get() = SecuritySchemeType.OAuth2

    /**
     * A description of the scheme, which may use CommonMark.
     *
     * @since 1.0.0
     */
    public override val description: String? = builder.description

    /**
     * The flows the scheme supports.
     *
     * @since 1.0.0
     */
    public val flows: OAuthFlows = builder.flows

    /**
     * The scopes a client needs, each one of the available scopes of a flow.
     *
     * @since 1.0.0
     */
    public val scopes: List<String> = builder.scopes.toList()

    override val identity: List<Any?> get() = listOf(description, flows, scopes)
}
