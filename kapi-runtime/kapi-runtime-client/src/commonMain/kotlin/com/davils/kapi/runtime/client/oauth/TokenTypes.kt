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

package com.davils.kapi.runtime.client.oauth

/**
 * The token type identifiers of RFC 8693 §3 for a token exchange.
 *
 * @since 1.0.0
 */
public object TokenTypes {
    /**
     * An OAuth 2 access token.
     *
     * @since 1.0.0
     */
    public const val ACCESS_TOKEN: String = "urn:ietf:params:oauth:token-type:access_token"

    /**
     * An OAuth 2 refresh token.
     *
     * @since 1.0.0
     */
    public const val REFRESH_TOKEN: String = "urn:ietf:params:oauth:token-type:refresh_token"

    /**
     * An OpenID Connect ID token.
     *
     * @since 1.0.0
     */
    public const val ID_TOKEN: String = "urn:ietf:params:oauth:token-type:id_token"

    /**
     * A JWT of RFC 7519.
     *
     * @since 1.0.0
     */
    public const val JWT: String = "urn:ietf:params:oauth:token-type:jwt"
}
