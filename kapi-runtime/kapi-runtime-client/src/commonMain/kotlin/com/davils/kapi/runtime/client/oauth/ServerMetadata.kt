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

@file:OptIn(DavilsInternal::class)

package com.davils.kapi.runtime.client.oauth

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

/**
 * The metadata of an authorization server by RFC 8414 §2 or OpenID Connect Discovery 1.0, as far as a client needs it.
 *
 * Metadata are only accepted when their `issuer` is exactly the issuer the client was configured with and every
 * endpoint is `https`, or `http` on this machine. Metadata are immutable.
 *
 * @property issuer the issuer identifier.
 * @property tokenEndpoint the token endpoint.
 * @property authorizationEndpoint the authorization endpoint, or `null`.
 * @property deviceAuthorizationEndpoint the device authorization endpoint of RFC 8628, or `null`.
 * @property revocationEndpoint the revocation endpoint of RFC 7009, or `null`.
 * @property codeChallengeMethods the PKCE methods the server supports; empty when it names none.
 * @since 1.0.0
 */
public class ServerMetadata internal constructor(
    public val issuer: String,
    public val tokenEndpoint: String,
    public val authorizationEndpoint: String?,
    public val deviceAuthorizationEndpoint: String?,
    public val revocationEndpoint: String?,
    public val codeChallengeMethods: List<String>,
) {
    override fun toString(): String = "ServerMetadata(${SafeText.text(issuer)})"
}
