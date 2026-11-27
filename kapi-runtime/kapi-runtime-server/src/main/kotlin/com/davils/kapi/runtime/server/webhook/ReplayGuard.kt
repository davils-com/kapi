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

import com.davils.kontinuity.cache.Cache
import com.davils.kontinuity.cache.cache
import com.davils.kontinuity.core.Environment
import kotlinx.io.bytestring.ByteString
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

private const val CACHE_NAME: String = "kapi-server-replays"
private const val DEFAULT_ENTRIES: Int = 100_000

/**
 * Remembers the signatures a [MessageSignatureVerifier] accepted, so a delivery captured on the way cannot be sent
 * again while its signature is still young enough to pass; the service provider interface of replay protection of
 * RFC 9421 §7.2.2.
 *
 * Without a guard, a signature can be replayed for as long as [SignaturePolicy.maxAge] and its `expires` allow. A
 * guard is code of the application, such as a set in a shared cache: it runs contained, and a failure refuses the
 * delivery. It must be safe to call from any coroutine at once and should forget a signature after its [until].
 *
 * @since 1.0.0
 */
public fun interface ReplayGuard {
    /**
     * Records [signature] as used and tells whether it was unused before.
     *
     * @param signature the bytes of the accepted signature, which identify the delivery.
     * @param until when the policy refuses the signature anyway, after which it need not be remembered.
     * @return `true` if [signature] was never used before, `false` for a replay.
     * @since 1.0.0
     */
    public suspend fun isFirstUse(signature: ByteString, until: Instant): Boolean

    /**
     * Makes replay guards.
     *
     * @since 1.1.0
     */
    public companion object {
        /**
         * Returns a guard that remembers signatures in memory, in a cache of kontinuity, until their [until] passes.
         *
         * The guard holds at most [maxEntries] signatures; when it is full, the signature used least recently is
         * forgotten first, so [maxEntries] must cover the deliveries a [SignaturePolicy.maxAge] spans. It protects one
         * process only: several instances of a service share a guard of their own, such as a set in a shared cache.
         *
         * @param maxEntries the most signatures remembered at once; 100,000 by default.
         * @param clock the clock [until] is measured against.
         * @param environment the environment of the cache, with its meters, events and time source.
         * @return the guard.
         * @throws IllegalArgumentException if [maxEntries] is not positive.
         * @since 1.1.0
         */
        public fun inMemory(
            maxEntries: Int = DEFAULT_ENTRIES,
            clock: Clock = Clock.System,
            environment: Environment = Environment.Default,
        ): ReplayGuard {
            require(maxEntries > 0) { "A replay guard remembers at least one signature" }
            val seen: Cache<ByteString, Instant> = cache(name = CACHE_NAME, environment = environment) {
                maximumSize = maxEntries
                expireAfter { _, until -> (until - clock.now()).coerceAtLeast(Duration.ZERO) }
            }
            return ReplayGuard { signature, until -> seen.putIfAbsent(signature, until) == null }
        }
    }
}
