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

package com.davils.kapi.runtime.server.webhook

import com.davils.krypt.primitive.key.PublicKey
import com.davils.krypt.primitive.key.SecretKey

/**
 * A key that verifies HTTP message signatures of RFC 9421, with the one algorithm it may be used with.
 *
 * Binding the algorithm to the key, as RFC 9421 §3.3.7 asks, keeps a sender from choosing a weaker one: the parameter
 * `alg` of a signature, when present, must name it.
 *
 * @since 1.0.0
 */
public sealed interface VerificationKey {
    /**
     * The algorithm of the key.
     *
     * @since 1.0.0
     */
    public val algorithm: MessageSignatureAlgorithm

    /**
     * A shared secret of `hmac-sha256`.
     *
     * @property key the secret, at least 128 bits.
     * @since 1.0.0
     */
    public class Shared(public val key: SecretKey) : VerificationKey {
        override val algorithm: MessageSignatureAlgorithm get() = MessageSignatureAlgorithm.HmacSha256

        override fun toString(): String = "Shared(${key.bits} bits)"
    }

    /**
     * A public key of an asymmetric algorithm.
     *
     * @property key the public key.
     * @property algorithm the algorithm, which fits the family of [key].
     * @throws IllegalArgumentException if [algorithm] is `hmac-sha256`.
     * @since 1.0.0
     */
    public class Public(public val key: PublicKey, override val algorithm: MessageSignatureAlgorithm) :
        VerificationKey {
        init {
            require(algorithm != MessageSignatureAlgorithm.HmacSha256) { "A public key verifies no HMAC" }
        }

        override fun toString(): String = "Public(${algorithm.id})"
    }
}
