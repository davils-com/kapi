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

import com.davils.kapi.runtime.core.codec.Codecs
import com.davils.kapi.runtime.core.codec.Decoding
import com.davils.kapi.runtime.server.internal.Scopes
import com.davils.kore.core.principal.Principal
import kotlinx.io.bytestring.encodeToByteString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull
import kotlin.time.Clock

private const val MOST_ANSWER_LENGTH: Int = 65_536

private const val JSON: String = "application/json"

/**
 * Authorizes a bearer scheme of RFC 6750 by token introspection of RFC 7662, for opaque tokens an authorization server
 * issued.
 *
 * The token is handed to [transport]; the answer is read as JSON within 64 KiB and the depth limit of the default
 * codecs. A token is denied with `invalid_token` when the answer is missing or malformed, when `active` is not `true`,
 * when `exp` has passed or `nbf` lies ahead by [clock], when [issuer] is set and `iss` differs, and when [audience] is
 * set and `aud` does not name it. The scopes come from `scope`, separated by spaces; a token that lacks one the
 * operation demands is forbidden with `insufficient_scope`. The caller is a principal of `sub`, or of `username` or
 * `client_id` when there is no subject, with the granted scopes as its roles.
 *
 * @param transport asks the introspection endpoint.
 * @param clock the clock of `exp` and `nbf`.
 * @param issuer the issuer the answer must name, or `null` for any.
 * @param audience the audience the answer must name, or `null` for any.
 * @since 1.0.0
 */
public class IntrospectionAuthorizer(
    private val transport: IntrospectionTransport,
    private val clock: Clock = Clock.System,
    private val issuer: String? = null,
    private val audience: String? = null,
) : Authorizer {
    override suspend fun authorize(attempt: AuthorizationAttempt): Authorization {
        val token = (attempt.credential as? PresentedCredential.Token)?.token
        val answer = token?.let { secret -> transport.introspect(secret) }?.takeIf { it.length <= MOST_ANSWER_LENGTH }
        val members = answer?.let(::membersOf)?.takeIf(::isAccepted)
        return members?.let { found ->
            Scopes.decide(
                granted = scopesOf(found),
                demanded = attempt.scopes,
            ) { principalOf(found) }
        }
            ?: Authorization.InvalidToken
    }

    override fun toString(): String = "IntrospectionAuthorizer"

    private fun isAccepted(members: JsonObject): Boolean {
        val now = clock.now().epochSeconds
        val isActive = literal(members, ACTIVE)?.booleanOrNull == true
        val expires = literal(members, EXPIRES)?.longOrNull
        val notBefore = literal(members, NOT_BEFORE)?.longOrNull
        val isTimely = (expires == null || expires > now) && (notBefore == null || notBefore <= now)
        val isIssued = issuer == null || text(members, ISSUER) == issuer
        return isActive && isTimely && isIssued && isForAudience(members)
    }

    private fun isForAudience(members: JsonObject): Boolean {
        val expected = audience ?: return true
        val listed = (members[AUDIENCE] as? JsonArray)?.mapNotNull { item -> (item as? JsonPrimitive)?.contentOrNull }
        return expected == text(members, AUDIENCE) || listed.orEmpty().contains(expected)
    }

    private fun scopesOf(members: JsonObject): Set<String> = Scopes.split(text(members, SCOPE))

    private fun principalOf(members: JsonObject): Principal? {
        val id = text(members, SUBJECT) ?: text(members, USERNAME) ?: text(members, CLIENT_ID)
        return Scopes.principalOf(id, scopesOf(members))
    }

    private companion object {
        const val ACTIVE: String = "active"
        const val EXPIRES: String = "exp"
        const val NOT_BEFORE: String = "nbf"
        const val ISSUER: String = "iss"
        const val AUDIENCE: String = "aud"
        const val SCOPE: String = "scope"
        const val SUBJECT: String = "sub"
        const val USERNAME: String = "username"
        const val CLIENT_ID: String = "client_id"

        fun membersOf(answer: String): JsonObject? {
            val decoding = Codecs.Default.decode(JsonObject.serializer(), answer.encodeToByteString(), JSON)
            return (decoding as? Decoding.Decoded)?.value
        }

        fun primitive(members: JsonObject, name: String): JsonPrimitive? = members[name] as? JsonPrimitive

        fun literal(members: JsonObject, name: String): JsonPrimitive? =
            primitive(members, name)?.takeUnless(JsonPrimitive::isString)

        fun text(members: JsonObject, name: String): String? =
            primitive(members, name)?.takeIf(JsonPrimitive::isString)?.contentOrNull
    }
}
