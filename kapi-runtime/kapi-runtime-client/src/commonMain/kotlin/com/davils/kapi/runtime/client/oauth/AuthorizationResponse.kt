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

import com.davils.kapi.runtime.core.codec.form.FormUrlEncoding
import com.davils.kapi.runtime.core.security.Secret
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.outcome.Outcome
import com.davils.kore.core.text.SafeText

private const val CODE: String = "code"

private const val STATE: String = "state"

private const val ISSUER: String = "iss"

private const val ERROR: String = "error"

private val unique: List<String> = listOf(CODE, STATE, ISSUER)

/**
 * What the authorization server handed back through the redirect URI: the authorization code, the state and, by
 * RFC 9207, the issuer.
 *
 * A response is immutable; [toString] never shows the code.
 *
 * @property code the authorization code.
 * @property state the state of the request.
 * @property issuer the `iss` parameter of RFC 9207, or `null`; when present it must name the issuer of the client, and
 * it must be present when the metadata of the server announce it ([ServerMetadata.isIssuerReturned]).
 * @since 1.0.0
 */
public class AuthorizationResponse(
    public val code: Secret,
    public val state: String,
    public val issuer: String? = null,
) {
    override fun toString(): String = "AuthorizationResponse(${SafeText.text(state)})"

    /**
     * Reads responses.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Reads the query of the URI the authorization server redirected to.
         *
         * @param uri the whole redirect URI, which may come from an attacker.
         * @return the response, or `null` for an error response, a query that cannot be read, one without a code or a
         * state, or one that names `code`, `state` or `iss` more than once (RFC 6749 §3.1).
         * @since 1.0.0
         */
        public fun fromRedirect(uri: String): AuthorizationResponse? {
            val query = uri.substringAfter('?', "").substringBefore('#')
            val fields = when (val decoded = FormUrlEncoding.decode(query)) {
                is Outcome.Success -> decoded.value
                is Outcome.Failure -> emptyList()
            }
            val code = fields.firstOrNull { (name, _) -> name == CODE }?.second
            val state = fields.firstOrNull { (name, _) -> name == STATE }?.second
            val issuer = fields.firstOrNull { (name, _) -> name == ISSUER }?.second
            val isError = fields.any { (name, _) -> name == ERROR }
            val isRepeated = unique.any { parameter -> fields.count { (name, _) -> name == parameter } > 1 }
            val isIncomplete = code.isNullOrEmpty() || state == null
            if (isError || isRepeated || isIncomplete) return null
            return AuthorizationResponse(code = Secret(code), state = state, issuer = issuer)
        }
    }
}
