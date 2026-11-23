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

package com.davils.kapi.runtime.core.security

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.failure.attempted
import com.davils.kore.core.text.SafeText
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope

/**
 * The credentials of a client: a [TokenSource] for every security scheme that needs one and the schemes the
 * connection meets by itself.
 *
 * [choose] picks the first security requirement of an operation the client can meet, [credential] hands out what a
 * scheme holds and refuses a credential that could inject a header field, and [refresh] asks a source for a new
 * credential after the server rejected one. Credentials are immutable and safe to share between threads; the sources
 * they call must be as well. [toString] names the schemes and never a credential.
 *
 * @since 1.0.0
 */
public class Credentials internal constructor(builder: CredentialsBuilder) : Validatable {
    private val peers: Set<String> = builder.peers.toSet()

    private val declared: List<String> = builder.sources.map { (name, _) -> name } + builder.peers

    private val sources: Map<String, TokenSource> = builder.sources.toMap()

    /**
     * The names of every scheme with a source or met by the connection.
     *
     * @since 1.0.0
     */
    public val schemes: Set<String> get() = sources.keys + peers

    /**
     * Chooses the first alternative of a security requirement whose schemes this client can all meet.
     *
     * @param alternatives the alternatives in the order of the document; an empty set allows anonymous access.
     * @return the chosen schemes, or `null` if no alternative can be met.
     * @since 1.0.0
     */
    public fun choose(alternatives: List<Set<String>>): Set<String>? =
        alternatives.firstOrNull { alternative -> alternative.all { scheme -> scheme in schemes } }

    /**
     * Returns what the scheme [scheme] holds now.
     *
     * The credential of a source is trimmed; one that is then blank or carries a control character is
     * [Credential.Malformed] and must not be sent. A source that throws is contained and counts as one without a
     * credential; a cancellation of the calling coroutine is passed on.
     *
     * @param scheme the name of the scheme.
     * @return the credential, [Credential.Peer] for a scheme the connection meets, or [Credential.Missing].
     * @since 1.0.0
     */
    public suspend fun credential(scheme: String): Credential {
        if (scheme in peers) return Credential.Peer
        return tokenOf(scheme)
    }

    /**
     * Asks the source of [scheme] for a new credential after the server rejected [rejected].
     *
     * @param scheme the name of the scheme.
     * @param rejected the credential the server rejected, or `null`.
     * @return `true` if the source is a [RefreshingTokenSource] and has a new credential; `false` if it throws.
     * @since 1.0.0
     */
    public suspend fun refresh(scheme: String, rejected: Secret?): Boolean {
        val source = sources[scheme] as? RefreshingTokenSource ?: return false
        return attempted { source.refresh(rejected) }.getOrNull() == true
    }

    override fun ValidationScope.validate() {
        if (declared.any(String::isBlank)) {
            fail(::schemes, "names a scheme that is blank", CredentialsViolationCodes.SCHEME_BLANK)
        }
        val counts = declared.groupingBy { name -> name }.eachCount()
        val repeated = counts.filterValues { count -> count > 1 }
        repeated.keys.forEach { name ->
            fail(
                ::schemes,
                "names the scheme ${SafeText.quoted(name)} more than once",
                CredentialsViolationCodes.SCHEME_DUPLICATE,
            )
        }
    }

    override fun toString(): String = "Credentials(${schemes.map(SafeText::text)})"

    private suspend fun tokenOf(scheme: String): Credential {
        val source = sources[scheme]
        val secret = source?.let { found -> attempted { found.token() }.getOrNull() } ?: return Credential.Missing
        return checked(secret.reveal().trim())
    }

    private fun checked(text: String): Credential {
        val isUsable = text.isNotEmpty() && text.none(Char::isISOControl)
        if (!isUsable) return Credential.Malformed
        return Credential.Present(Secret(text))
    }

    /**
     * The credentials without any scheme.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The credentials of a client that calls only operations open to anyone.
         *
         * @since 1.0.0
         */
        public val None: Credentials = CredentialsBuilder().build()
    }
}
