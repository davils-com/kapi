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
import com.davils.kapi.runtime.server.request.PeerIdentity
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import com.davils.krypt.x509.Certificate

/**
 * The credential a request presents for a security scheme, as the server read it from where the scheme places it.
 *
 * Secrets stay in a [Secret], whose `toString` is masked, so no credential reaches a message by accident.
 *
 * @since 1.0.0
 */
public sealed interface PresentedCredential {
    /**
     * A bearer token of RFC 6750.
     *
     * @property token the token.
     * @since 1.0.0
     */
    public class Token(public val token: Secret) : PresentedCredential {
        override fun equals(other: Any?): Boolean = other is Token && token == other.token

        override fun hashCode(): Int = token.hashCode()

        override fun toString(): String = "Token($token)"
    }

    /**
     * An API key from a header field, a query parameter or a cookie.
     *
     * @property key the key.
     * @since 1.0.0
     */
    public class Key(public val key: Secret) : PresentedCredential {
        override fun equals(other: Any?): Boolean = other is Key && key == other.key

        override fun hashCode(): Int = key.hashCode()

        override fun toString(): String = "Key($key)"
    }

    /**
     * A user and a password of RFC 7617, read as UTF-8.
     *
     * @property user the user id, which may come from an attacker.
     * @property password the password.
     * @since 1.0.0
     */
    public class Basic(public val user: String, public val password: Secret) : PresentedCredential {
        private val parts: List<Any> get() = listOf(user, password)

        override fun equals(other: Any?): Boolean = other is Basic && parts == other.parts

        override fun hashCode(): Int = user.hashCode()

        @OptIn(DavilsInternal::class)
        override fun toString(): String = "Basic(${SafeText.text(user)}, $password)"
    }

    /**
     * The client certificate chain the transport verified, the leaf first.
     *
     * @param chain the chain.
     * @since 1.0.0
     */
    public class Certificates(chain: List<Certificate>) : PresentedCredential {
        /**
         * The chain, the leaf first.
         *
         * @since 1.0.0
         */
        public val chain: List<Certificate> = chain.toList()

        override fun equals(other: Any?): Boolean = other is Certificates && chain == other.chain

        override fun hashCode(): Int = chain.hashCode()

        override fun toString(): String = "Certificates(${chain.size})"
    }

    /**
     * The identity of a peer the transport verified, which stands in for a credential the request does not present.
     *
     * @property identity the identity.
     * @since 1.0.0
     */
    public class Peer(public val identity: PeerIdentity) : PresentedCredential {
        override fun equals(other: Any?): Boolean = other is Peer && identity == other.identity

        override fun hashCode(): Int = identity.hashCode()

        override fun toString(): String = "Peer($identity)"
    }
}
