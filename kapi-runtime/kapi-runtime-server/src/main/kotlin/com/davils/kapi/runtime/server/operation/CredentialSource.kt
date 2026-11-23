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

package com.davils.kapi.runtime.server.operation

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

/**
 * Where a security scheme of OpenAPI 3.2 §4.25 places its credential in a request.
 *
 * @since 1.0.0
 */
public sealed interface CredentialSource {
    /**
     * A bearer token in `Authorization: Bearer` of RFC 6750, as an `oauth2`, `openIdConnect` or `http` `bearer`
     * scheme sends it.
     *
     * @since 1.0.0
     */
    public object Bearer : CredentialSource {
        override fun toString(): String = "Bearer"
    }

    /**
     * A user and a password in `Authorization: Basic` of RFC 7617.
     *
     * @since 1.0.0
     */
    public object Basic : CredentialSource {
        override fun toString(): String = "Basic"
    }

    /**
     * An API key in the header field [name].
     *
     * @property name the name of the field, compared without case.
     * @since 1.0.0
     */
    public class Header(public val name: String) : CredentialSource {
        override fun equals(other: Any?): Boolean = other is Header && name.equals(other.name, ignoreCase = true)

        override fun hashCode(): Int = name.lowercase().hashCode()

        @OptIn(DavilsInternal::class)
        override fun toString(): String = "Header(${SafeText.text(name)})"
    }

    /**
     * An API key in the query parameter [name].
     *
     * @property name the name of the parameter.
     * @since 1.0.0
     */
    public class Query(public val name: String) : CredentialSource {
        override fun equals(other: Any?): Boolean = other is Query && name == other.name

        override fun hashCode(): Int = name.hashCode()

        @OptIn(DavilsInternal::class)
        override fun toString(): String = "Query(${SafeText.text(name)})"
    }

    /**
     * An API key in the cookie [name].
     *
     * @property name the name of the cookie.
     * @since 1.0.0
     */
    public class Cookie(public val name: String) : CredentialSource {
        override fun equals(other: Any?): Boolean = other is Cookie && name == other.name

        override fun hashCode(): Int = name.hashCode()

        @OptIn(DavilsInternal::class)
        override fun toString(): String = "Cookie(${SafeText.text(name)})"
    }

    /**
     * The client certificate chain of a `mutualTLS` scheme, which the transport verified and handed over in
     * [com.davils.kapi.runtime.server.PeerAttributes.certificates].
     *
     * @since 1.0.0
     */
    public object Certificate : CredentialSource {
        override fun toString(): String = "Certificate"
    }
}
