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

/**
 * The algorithms of the HTTP Signature Algorithms registry of RFC 9421 §6.2.2 that a [MessageSignatureVerifier]
 * verifies.
 *
 * @property id the name of the algorithm in the parameter `alg`, such as `ed25519`.
 * @since 1.0.0
 */
public enum class MessageSignatureAlgorithm(public val id: String) {
    /**
     * HMAC with SHA-256 of RFC 9421 §3.3.3, with a shared secret.
     *
     * @since 1.0.0
     */
    HmacSha256("hmac-sha256"),

    /**
     * RSASSA-PSS with SHA-512 of RFC 9421 §3.3.1.
     *
     * @since 1.0.0
     */
    RsaPssSha512("rsa-pss-sha512"),

    /**
     * RSASSA-PKCS1-v1_5 with SHA-256 of RFC 9421 §3.3.2.
     *
     * @since 1.0.0
     */
    RsaV15Sha256("rsa-v1_5-sha256"),

    /**
     * ECDSA on the curve P-256 with SHA-256 of RFC 9421 §3.3.4, the signature as `r` and `s` of 32 bytes each.
     *
     * @since 1.0.0
     */
    EcdsaP256Sha256("ecdsa-p256-sha256"),

    /**
     * ECDSA on the curve P-384 with SHA-384 of RFC 9421 §3.3.5, the signature as `r` and `s` of 48 bytes each.
     *
     * @since 1.0.0
     */
    EcdsaP384Sha384("ecdsa-p384-sha384"),

    /**
     * EdDSA on Edwards25519 of RFC 9421 §3.3.6.
     *
     * @since 1.0.0
     */
    Ed25519("ed25519"),
}
