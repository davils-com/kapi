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
 * An OAuth flow object: the endpoints and the scopes of one grant of OAuth 2.0.
 *
 * The element is immutable and safe to share between threads. The addresses are kept as they were written and not
 * checked; a client calls them only through its destination policy.
 *
 * @since 1.0.0
 */
public class OAuthFlow internal constructor(builder: OAuthFlowBuilder) :
    Element(builder) {
    /**
     * The `authorizationUrl` of the authorization endpoint, which the implicit and the authorization code flows need;
     * `null` when the document leaves it out.
     *
     * @since 1.0.0
     */
    public val authorizationUrl: String? = builder.authorizationUrl

    /**
     * The `deviceAuthorizationUrl` of the device authorization endpoint, which the device authorization flow needs;
     * `null` for every other flow and before OpenAPI 3.2.
     *
     * @since 1.0.0
     */
    public val deviceAuthorizationUrl: String? = builder.deviceAuthorizationUrl

    /**
     * The `tokenUrl` of the token endpoint, which every flow but the implicit one needs; `null` when the document
     * leaves it out.
     *
     * @since 1.0.0
     */
    public val tokenUrl: String? = builder.tokenUrl

    /**
     * The `refreshUrl` that refreshes a token; `null` when the document leaves it out.
     *
     * @since 1.0.0
     */
    public val refreshUrl: String? = builder.refreshUrl

    /**
     * The `scopes` of the flow, each name with a short description, in the order of the document; empty when the flow
     * declares none.
     *
     * @since 1.0.0
     */
    public val scopes: Map<String, String> = builder.scopes.toMap()

    override val identity: List<Any?>
        get() = listOf(authorizationUrl, deviceAuthorizationUrl, tokenUrl, refreshUrl, scopes)
}
