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

package com.davils.kapi.runtime.testing.fake

import com.davils.krypt.core.RandomSource
import com.davils.krypt.core.environment
import com.davils.krypt.primitive.Crypto
import kotlin.time.Clock

/**
 * Returns a crypto object whose clock and entropy a test controls, for
 * [com.davils.kapi.runtime.client.oauth.OAuthSettingsBuilder.crypto] and DPoP proofs, so that PKCE verifiers, states,
 * the identifiers of client assertions and proofs and every time they carry repeat on every run.
 *
 * Keys, signatures and digests still come from the platform. The randomness is predictable by design: a crypto
 * object from this function never belongs anywhere near production.
 *
 * @param clock the wall clock of the environment; a fresh [ManualClock] by default.
 * @param random the entropy of the environment; a fresh [CountingRandom] by default.
 * @return the crypto object.
 * @since 1.0.0
 */
public fun deterministicCrypto(clock: Clock = ManualClock(), random: RandomSource = CountingRandom()): Crypto {
    val environment = environment {
        this.clock = clock
        this.random = random
    }
    return Crypto(environment)
}
