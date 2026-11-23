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
 * Where an application sends the user to authorize a client, by the authorization code grant of RFC 6749 §4.1 with
 * the proof key of RFC 7636.
 *
 * The [url] carries the `state` and the `code_challenge` of the method `S256`, never the verifier, which stays in the
 * token source. A request is immutable.
 *
 * @property url the authorization URL to open in the browser of the user.
 * @property state the state the redirect back must carry.
 * @property redirectUri the URI the authorization server redirects back to.
 * @since 1.0.0
 */
public class AuthorizationRequest internal constructor(
    public val url: String,
    public val state: String,
    public val redirectUri: String,
) {
    override fun toString(): String = "AuthorizationRequest(${SafeText.text(redirectUri)})"
}
