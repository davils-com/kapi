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

package com.davils.kapi.runtime.server.security

import com.davils.kapi.runtime.core.security.Secret
import com.davils.kore.core.principal.Principal

/**
 * Authorizes an `apiKey` scheme of OpenAPI 3.2 §4.25 by the key a request presents in a header field, a query
 * parameter or a cookie.
 *
 * [lookup] finds the caller a key belongs to; it should compare keys in constant time, such as by a lookup of a digest
 * of the key, and must never reveal through its timing which part of a key was right. A key that [lookup] does not
 * know and every other kind of credential are denied. API keys carry no scopes: the operation decides with the
 * principal.
 *
 * @param lookup returns the caller [Secret] belongs to, or `null` if the key is unknown.
 * @since 1.0.0
 */
public class ApiKeyAuthorizer(private val lookup: suspend (Secret) -> Principal?) : Authorizer {
    override suspend fun authorize(attempt: AuthorizationAttempt): Authorization {
        val key = (attempt.credential as? PresentedCredential.Key)?.key
        val principal = key?.let { secret -> lookup(secret) }
        return principal?.let(Authorization::Granted) ?: Authorization.Unknown
    }

    override fun toString(): String = "ApiKeyAuthorizer"
}
