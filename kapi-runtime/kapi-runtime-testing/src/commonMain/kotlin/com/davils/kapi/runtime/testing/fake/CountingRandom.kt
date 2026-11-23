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

@file:OptIn(ExperimentalAtomicApi::class)

package com.davils.kapi.runtime.testing.fake

import com.davils.krypt.core.RandomSource
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * Entropy for tests that is no entropy at all: every byte it draws is the next value of a counter, so that PKCE
 * verifiers, states, nonces, multipart boundaries and the identifiers of proofs repeat on every run.
 *
 * The byte at position `n` of all bytes drawn is `(seed + n) mod 256`. Hand it to [deterministicCrypto] or to an
 * [com.davils.krypt.core.EnvironmentBuilder] of a test. It is predictable by design and therefore never belongs
 * anywhere near production. A source is safe to draw from several threads at once: every draw takes its own range of
 * the counter.
 *
 * @param seed the value of the first byte.
 * @since 1.0.0
 */
public class CountingRandom(private val seed: Int = 0) : RandomSource {
    private val counter: AtomicLong = AtomicLong(0L)

    /**
     * How many bytes were drawn so far.
     *
     * @since 1.0.0
     */
    public val drawn: Long get() = counter.load()

    /**
     * Fills [target] with the next values of the counter.
     *
     * @param target the array to fill.
     * @since 1.0.0
     */
    override fun fill(target: ByteArray) {
        val start = counter.fetchAndAdd(target.size.toLong())
        target.indices.forEach { index -> target[index] = (seed + start + index).toByte() }
    }

    /**
     * Shows the seed and the bytes drawn.
     *
     * @return the text.
     * @since 1.0.0
     */
    override fun toString(): String = "CountingRandom(seed=$seed, drawn=${counter.load()})"
}
