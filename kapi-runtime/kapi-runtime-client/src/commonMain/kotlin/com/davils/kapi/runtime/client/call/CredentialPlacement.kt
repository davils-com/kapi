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

package com.davils.kapi.runtime.client.call

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

/**
 * Where the credential of a security scheme goes in a request, as the `securitySchemes` of an OpenAPI document
 * declare it.
 *
 * @since 1.0.0
 */
public sealed interface CredentialPlacement {
    /**
     * The field `Authorization: Bearer <token>` of RFC 6750, or `Authorization: DPoP <token>` with a proof of RFC 9449
     * when the client binds the scheme to a DPoP key.
     *
     * @since 1.0.0
     */
    public object Bearer : CredentialPlacement {
        override fun toString(): String = "Bearer"
    }

    /**
     * The field `Authorization: Basic <base64>` of RFC 7617; the credential is the `user:password` text.
     *
     * @since 1.0.0
     */
    public object Basic : CredentialPlacement {
        override fun toString(): String = "Basic"
    }

    /**
     * An API key in the header field [name].
     *
     * @property name the name of the field.
     * @since 1.0.0
     */
    public class Header(public val name: String) : CredentialPlacement {
        override fun equals(other: Any?): Boolean = other is Header && name == other.name

        override fun hashCode(): Int = name.hashCode()

        override fun toString(): String = "Header(${SafeText.text(name)})"
    }

    /**
     * An API key in the query parameter [name].
     *
     * @property name the name of the parameter.
     * @since 1.0.0
     */
    public class Query(public val name: String) : CredentialPlacement {
        override fun equals(other: Any?): Boolean = other is Query && name == other.name

        override fun hashCode(): Int = name.hashCode()

        override fun toString(): String = "Query(${SafeText.text(name)})"
    }

    /**
     * An API key in the cookie [name].
     *
     * @property name the name of the cookie.
     * @since 1.0.0
     */
    public class Cookie(public val name: String) : CredentialPlacement {
        override fun equals(other: Any?): Boolean = other is Cookie && name == other.name

        override fun hashCode(): Int = name.hashCode()

        override fun toString(): String = "Cookie(${SafeText.text(name)})"
    }
}
