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

package com.davils.kapi.runtime.client.dpop

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.krypt.jose.JoseKey
import com.davils.krypt.jose.jwk.JsonWebKeys
import com.davils.krypt.jose.jws.JwsAlgorithm
import com.davils.krypt.primitive.Crypto
import com.davils.krypt.primitive.key.PublicKey

private val symmetric: Set<JwsAlgorithm> = setOf(JwsAlgorithm.HS256, JwsAlgorithm.HS384, JwsAlgorithm.HS512)

/**
 * Makes the DPoP proofs of RFC 9449 for the key pair of [key] and [publicKey].
 *
 * @param crypto the cryptography of krypt that signs the proofs, draws their identifiers and tells their time.
 * @param key the private key; it stays in the proofs and is never shown.
 * @param publicKey the public key of [key], which every proof carries as a JWK.
 * @param algorithm the algorithm of the signatures, such as [JwsAlgorithm.ES256]; it must be asymmetric.
 * @return the proofs.
 * @throws IllegalArgumentException if [algorithm] is a MAC, which RFC 9449 §4.2 forbids.
 * @since 1.0.0
 */
public fun dpopProofs(
    crypto: Crypto,
    key: JoseKey.Private,
    publicKey: PublicKey,
    algorithm: JwsAlgorithm,
): DpopProofs {
    require(algorithm !in symmetric) { "A DPoP proof is signed with a private key, never with a MAC" }
    val keys = JsonWebKeys(crypto)
    val jwk = keys.of(publicKey)
    return DpopProofs(crypto = crypto, key = key, algorithm = algorithm, publicKey = keys.members(jwk))
}
