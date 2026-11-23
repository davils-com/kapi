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

package com.davils.kapi.runtime.core.security

/**
 * What [Credentials] hold for a security scheme at the moment a request is made.
 *
 * @since 1.0.0
 */
public sealed interface Credential {
    /**
     * The scheme has no source or its source has no credential.
     *
     * @since 1.0.0
     */
    public object Missing : Credential {
        override fun toString(): String = "Missing"
    }

    /**
     * The scheme has the credential [secret], trimmed and free of control characters.
     *
     * @property secret the credential.
     * @since 1.0.0
     */
    public class Present(public val secret: Secret) : Credential {
        override fun equals(other: Any?): Boolean = other is Present && secret == other.secret

        override fun hashCode(): Int = secret.hashCode()

        override fun toString(): String = "Present($secret)"
    }

    /**
     * The source handed out a credential that is blank or carries a control character, such as a line break that
     * could inject a header field; it is never sent.
     *
     * @since 1.0.0
     */
    public object Malformed : Credential {
        override fun toString(): String = "Malformed"
    }

    /**
     * The scheme is met by the connection itself, such as mutual TLS, and needs no credential of its own.
     *
     * @since 1.0.0
     */
    public object Peer : Credential {
        override fun toString(): String = "Peer"
    }
}
