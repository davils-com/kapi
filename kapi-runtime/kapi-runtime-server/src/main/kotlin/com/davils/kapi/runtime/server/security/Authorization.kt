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

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.principal.Principal
import com.davils.kore.core.text.SafeText

private const val NONE: String = "none"

/**
 * What an [Authorizer] decides about the credential of a scheme.
 *
 * @since 1.0.0
 */
public sealed interface Authorization {
    /**
     * The credential is valid and grants every scope the operation demands.
     *
     * @property principal the caller, which the handler finds as `currentPrincipal()` of Kore.
     * @since 1.0.0
     */
    public class Granted(public val principal: Principal) : Authorization {
        override fun equals(other: Any?): Boolean = other is Granted && principal == other.principal

        override fun hashCode(): Int = principal.hashCode()

        override fun toString(): String = "Granted($principal)"
    }

    /**
     * The credential is missing, malformed, unknown, expired or revoked; the request is refused with 401
     * (Unauthorized) and a challenge.
     *
     * For a bearer token, [error] is an error code of RFC 6750 §3.1, such as `invalid_token` or `invalid_request`,
     * which the challenge carries with [description]. The description is shown to the client: it names what is wrong
     * in general terms and never what the token held. Characters RFC 6750 does not allow in it are dropped.
     *
     * @property error the error code, or `null` for a request that presented no credential.
     * @property description a text for a developer, or `null`.
     * @since 1.0.0
     */
    public class Denied(public val error: String? = null, public val description: String? = null) : Authorization {
        private val parts: List<String?> get() = listOf(error, description)

        override fun equals(other: Any?): Boolean = other is Denied && parts == other.parts

        override fun hashCode(): Int = parts.hashCode()

        @OptIn(DavilsInternal::class)
        override fun toString(): String =
            "Denied(${error?.let(SafeText::text) ?: NONE}, ${description?.let(SafeText::text) ?: NONE})"
    }

    /**
     * The credential is valid, but its holder may not do this, such as a token without a scope the operation
     * demands; the request is refused with 403 (Forbidden) and, for a bearer token, the error `insufficient_scope`
     * of RFC 6750 §3.1 with [scopes].
     *
     * @param scopes the scopes that would grant the request; empty when no scope would.
     * @since 1.0.0
     */
    public class Forbidden(scopes: Set<String> = emptySet()) : Authorization {
        /**
         * The scopes that would grant the request.
         *
         * @since 1.0.0
         */
        public val scopes: Set<String> = scopes.toSet()

        override fun equals(other: Any?): Boolean = other is Forbidden && scopes == other.scopes

        override fun hashCode(): Int = scopes.hashCode()

        @OptIn(DavilsInternal::class)
        override fun toString(): String = "Forbidden(${scopes.map(SafeText::text)})"
    }

    /**
     * The decisions every authorizer shares.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The denial of a bearer token that is malformed, expired, revoked or unknown: `invalid_token` of RFC 6750.
         *
         * @since 1.0.0
         */
        public val InvalidToken: Denied = Denied(error = "invalid_token", description = "The token is not valid")

        /**
         * The denial of a credential that is unknown or wrong, without an error code.
         *
         * @since 1.0.0
         */
        public val Unknown: Denied = Denied()
    }
}
