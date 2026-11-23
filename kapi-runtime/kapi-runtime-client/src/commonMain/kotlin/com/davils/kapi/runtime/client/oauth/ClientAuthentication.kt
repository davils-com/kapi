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

import com.davils.kapi.runtime.core.security.Secret
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import com.davils.krypt.jose.JoseKey
import com.davils.krypt.jose.jws.JwsAlgorithm

/**
 * How a client proves who it is to the token, device and revocation endpoints of an authorization server.
 *
 * [toString] never shows a secret or a key.
 *
 * @since 1.0.0
 */
public sealed interface ClientAuthentication {
    /**
     * A public client, such as a mobile or desktop application, that names only its `client_id` in the form.
     *
     * @since 1.0.0
     */
    public object None : ClientAuthentication {
        override fun toString(): String = "None"
    }

    /**
     * `client_secret_basic` of RFC 6749 §2.3.1: the id and the secret, each form-encoded, in an `Authorization: Basic`
     * field.
     *
     * @property secret the client secret.
     * @since 1.0.0
     */
    public class SecretBasic(public val secret: Secret) : ClientAuthentication {
        override fun equals(other: Any?): Boolean = other is SecretBasic && secret == other.secret

        override fun hashCode(): Int = secret.hashCode()

        override fun toString(): String = "SecretBasic(${SafeText.REDACTED})"
    }

    /**
     * `client_secret_post` of RFC 6749 §2.3.1: the id and the secret as fields of the form.
     *
     * @property secret the client secret.
     * @since 1.0.0
     */
    public class SecretPost(public val secret: Secret) : ClientAuthentication {
        override fun equals(other: Any?): Boolean = other is SecretPost && secret == other.secret

        override fun hashCode(): Int = secret.hashCode()

        override fun toString(): String = "SecretPost(${SafeText.REDACTED})"
    }

    /**
     * `client_secret_jwt` of RFC 7523 §2.2 and OpenID Connect: a JWT assertion signed with an HMAC of the client
     * secret, which itself never travels.
     *
     * @property secret the client secret; at least as many bytes as the hash of [algorithm].
     * @property algorithm the MAC, [JwsAlgorithm.HS256] by default.
     * @since 1.0.0
     */
    public class SecretJwt(public val secret: Secret, public val algorithm: JwsAlgorithm = JwsAlgorithm.HS256) :
        ClientAuthentication {
        override fun equals(other: Any?): Boolean =
            other is SecretJwt && secret == other.secret && algorithm == other.algorithm

        override fun hashCode(): Int = algorithm.hashCode()

        override fun toString(): String = "SecretJwt(${algorithm.algorithmName})"
    }

    /**
     * `private_key_jwt` of RFC 7523 §2.2 and OpenID Connect: a JWT assertion signed with the private key of the
     * client, whose public key the authorization server knows.
     *
     * @property key the private key.
     * @property algorithm the asymmetric algorithm of the signature, such as [JwsAlgorithm.ES256].
     * @since 1.0.0
     */
    public class PrivateKeyJwt(public val key: JoseKey.Private, public val algorithm: JwsAlgorithm) :
        ClientAuthentication {
        override fun equals(other: Any?): Boolean =
            other is PrivateKeyJwt && key == other.key && algorithm == other.algorithm

        override fun hashCode(): Int = algorithm.hashCode()

        override fun toString(): String = "PrivateKeyJwt(${algorithm.algorithmName})"
    }
}
