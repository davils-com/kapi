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

package com.davils.kapi.runtime.server.fixture

import com.davils.krypt.core.algorithm.KeyType
import com.davils.krypt.core.environment
import com.davils.krypt.core.policy.CryptoPolicy
import com.davils.krypt.primitive.Crypto
import com.davils.krypt.x509.Certificate
import com.davils.krypt.x509.Certificates
import kotlin.time.Clock
import kotlin.time.Instant

private const val NOW_SECONDS: Long = 1_800_000_000L

private const val LATER_SECONDS: Long = 1_900_000_000L

internal val NOW: Instant = Instant.fromEpochSeconds(NOW_SECONDS)

internal val fixedClock: Clock = object : Clock {
    override fun now(): Instant = NOW
}

internal val crypto: Crypto = Crypto(
    environment {
        clock = fixedClock
        policy = CryptoPolicy.Compatible
    },
)

internal suspend fun certificateOf(name: String): Certificate {
    val pair = crypto.keys.generate(KeyType.P256)
    val signer = crypto.signatures.signer(pair.privateKey)
    return Certificates(crypto).issue(signer) {
        subject { commonName(name) }
        publicKey = pair.publicKey
        notBefore = Instant.fromEpochSeconds(0L)
        notAfter = Instant.fromEpochSeconds(LATER_SECONDS)
    }
}
