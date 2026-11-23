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

import com.davils.kapi.runtime.server.internal.Scopes
import com.davils.kore.core.outcome.getOrNull
import com.davils.kore.core.principal.Principal
import com.davils.krypt.jose.JsonValue
import com.davils.krypt.jose.jwt.Jwt
import com.davils.krypt.jose.jwt.JwtValidator
import com.davils.krypt.jose.jwt.ValidatedJwt

/**
 * Authorizes a bearer scheme of RFC 6750 whose tokens are JSON Web Tokens of RFC 7519, validated by the JOSE of krypt
 * with the best current practice of RFC 8725.
 *
 * The token is validated by [validator] with the keys of [keys]: its signature with the algorithms the validator
 * permits and never with a key the token carries, its type, its times against the clock of the crypto of [jwt] with
 * the clock skew of the validator, its issuer and its audience. A token that fails is denied with `invalid_token`. The
 * scopes it grants are read from the claim `scope`, a list separated by spaces as RFC 8693 §4.2 writes it, or from
 * the claim `scp`, a string or an array of strings; a token that lacks a scope the operation demands is forbidden with
 * `insufficient_scope`. The caller is [principalOf] the token, by default a principal of the claim `sub` with the
 * granted scopes as its roles; a token without a subject is denied. Only a bearer token is checked; every other kind
 * of credential is denied.
 *
 * @param validator the checks of the token, such as `jwtValidator { issuer = …; audience(…); algorithms(…) }`.
 * @param keys the keys that may verify the signature.
 * @param jwt the JSON Web Tokens of krypt with their crypto and limits.
 * @param principalOf returns the caller of a valid token, or `null` to deny it.
 * @since 1.0.0
 */
public class JwtAuthorizer(
    private val validator: JwtValidator,
    private val keys: KeySource,
    private val jwt: Jwt,
    private val principalOf: (ValidatedJwt) -> Principal? = ::defaultPrincipal,
) : Authorizer {
    override suspend fun authorize(attempt: AuthorizationAttempt): Authorization {
        val token = (attempt.credential as? PresentedCredential.Token)?.token ?: return Authorization.InvalidToken
        val validated = jwt.validate(token.reveal(), validator, keys.keys()).getOrNull()
        return validated?.let { found ->
            Scopes.decide(
                granted = scopesOf(found),
                demanded = attempt.scopes,
            ) { principalOf(found) }
        }
            ?: Authorization.InvalidToken
    }

    override fun toString(): String = "JwtAuthorizer"

    private companion object {
        const val SCOPE: String = "scope"
        const val SCP: String = "scp"

        fun scopesOf(token: ValidatedJwt): Set<String> {
            val scope = (token.claims[SCOPE] as? JsonValue.Text)?.value
            val scp = token.claims[SCP]
            val listed = (scp as? JsonValue.Array)?.items.orEmpty().mapNotNull { item ->
                (item as? JsonValue.Text)
                    ?.value
            }
            val text = (scp as? JsonValue.Text)?.value
            return Scopes.split(scope) + Scopes.split(text) + Scopes.split(listed.joinToString(" "))
        }

        fun defaultPrincipal(token: ValidatedJwt): Principal? = Scopes.principalOf(
            token.claims.subject,
            scopesOf(token),
        )
    }
}
