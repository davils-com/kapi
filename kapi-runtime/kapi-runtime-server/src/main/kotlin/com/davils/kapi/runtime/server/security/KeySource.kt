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

package com.davils.kapi.runtime.server.security

import com.davils.kontinuity.cache.Cache
import com.davils.kontinuity.cache.cache
import com.davils.kontinuity.core.Environment
import com.davils.kore.core.outcome.getOrNull
import com.davils.krypt.jose.JoseKey
import com.davils.krypt.jose.jwk.JsonWebKeys
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

private const val CACHE_NAME: String = "kapi-server-keys"
private val DEFAULT_LIFETIME: Duration = 5.minutes

/**
 * Hands out the keys that may verify the signatures of tokens, such as the keys of the JWKS of an authorization
 * server.
 *
 * A source is code of the application and runs contained; a failure denies the token. It must be safe to call from
 * any coroutine at once. A source that fetches keys over the network is wrapped in [cached], which keeps them in a
 * cache of kontinuity.
 *
 * @since 1.0.0
 */
public fun interface KeySource {
    /**
     * Returns the keys.
     *
     * @return the keys; the validator picks among them by `kid` and algorithm.
     * @since 1.0.0
     */
    public suspend fun keys(): List<JoseKey>

    /**
     * Makes key sources.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Returns a source of fixed keys.
         *
         * @param keys the keys.
         * @return the source.
         * @since 1.0.0
         */
        public fun of(keys: List<JoseKey>): KeySource {
            val fixed = keys.toList()
            return KeySource { fixed }
        }

        /**
         * Returns a source that reads a JSON Web Key Set of RFC 7517 §5, as [fetch] hands it out, every time keys are
         * asked for, within the limits of [keys]; a key the set holds but krypt cannot use, such as a private or an
         * unknown key, is left out.
         *
         * @param keys the reader of JSON Web Keys, with its crypto and its limits.
         * @param fetch returns the text of the set, such as the body of the `jwks_uri` of an authorization server.
         * @return the source.
         * @since 1.0.0
         */
        public fun jwks(keys: JsonWebKeys, fetch: suspend () -> String): KeySource = KeySource {
            val set = keys.decodeSet(fetch()).getOrNull()
            set?.keys.orEmpty().mapNotNull { jwk -> keys.key(jwk).getOrNull() }
        }

        /**
         * Returns a source that asks [source] once and hands out its keys for [lifetime], through a cache of
         * kontinuity: callers that arrive while the keys load wait for the same load instead of fetching again, and a
         * failure is never kept, so the next call asks [source] again.
         *
         * @param source the source to cache, such as [jwks].
         * @param lifetime how long the keys are handed out before [source] is asked again; 5 minutes by default.
         * @param environment the environment of the cache, with its meters, events and time source.
         * @return the cached source.
         * @throws IllegalArgumentException if [lifetime] is not positive and finite.
         * @since 1.1.0
         */
        public fun cached(
            source: KeySource,
            lifetime: Duration = DEFAULT_LIFETIME,
            environment: Environment = Environment.Default,
        ): KeySource {
            require(lifetime.isPositive() && lifetime.isFinite()) {
                "The lifetime of the keys must be positive and finite"
            }
            val keys: Cache<Unit, List<JoseKey>> = cache(name = CACHE_NAME, environment = environment) {
                maximumSize = 1
                expireAfterWrite = lifetime
            }
            return KeySource { keys.getOrLoad(Unit) { source.keys() } }
        }
    }
}
