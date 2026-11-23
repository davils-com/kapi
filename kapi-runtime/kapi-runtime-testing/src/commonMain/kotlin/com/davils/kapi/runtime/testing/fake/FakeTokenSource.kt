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

@file:OptIn(ExperimentalAtomicApi::class, DavilsInternal::class)

package com.davils.kapi.runtime.testing.fake

import com.davils.kapi.runtime.core.security.RefreshingTokenSource
import com.davils.kapi.runtime.core.security.Secret
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * A token source for tests: it hands out a fixed token, counts how often it is asked, and answers every refresh with
 * the next [RefreshOutcome] of its script.
 *
 * ```kotlin
 * val tokens = FakeTokenSource("t1", listOf(RefreshOutcome.Rotate("t2")))
 * val client = apiClient(http) { credentials = credentials { scheme("bearer", tokens) } }
 * ```
 *
 * Once the script is used up, every further refresh is [RefreshOutcome.Refuse]. A source is safe to share between
 * coroutines and threads: every call updates its state atomically. It is a test double and never belongs on a
 * production class path.
 *
 * @param token the token handed out until a refresh rotates it, or `null` for a source without a token.
 * @param script the outcomes of the refreshes, in order.
 * @since 1.0.0
 */
public class FakeTokenSource(token: String?, script: List<RefreshOutcome> = emptyList()) : RefreshingTokenSource {
    private val lock: Mutex = Mutex()

    private val state: AtomicReference<TokenState> = AtomicReference(
        TokenState(token = token, script = script.toList()),
    )

    /**
     * The token the source hands out now, or `null` if it has none.
     *
     * @since 1.0.0
     */
    public val current: String? get() = state.load().token

    /**
     * How often [token] was called.
     *
     * @since 1.0.0
     */
    public val tokenRequests: Int get() = state.load().tokenRequests

    /**
     * How often [refresh] was called.
     *
     * @since 1.0.0
     */
    public val refreshRequests: Int get() = state.load().rejected.size

    /**
     * The tokens that the callers of [refresh] reported as rejected, in order.
     *
     * @since 1.0.0
     */
    public val rejected: List<Secret?> get() = state.load().rejected

    /**
     * Returns the current token and counts the request.
     *
     * @return the current token, or `null` if the source has none.
     * @since 1.0.0
     */
    override suspend fun token(): Secret? {
        val after = changed { found -> found.asked() }
        return after.token?.let(::Secret)
    }

    /**
     * Applies the next outcome of the script.
     *
     * @param rejected the token the server rejected, recorded in [FakeTokenSource.rejected].
     * @return `true` for [RefreshOutcome.Rotate] and [RefreshOutcome.Keep], `false` for [RefreshOutcome.Refuse] and
     * once the script is used up.
     * @throws IllegalStateException for [RefreshOutcome.Fail].
     * @since 1.0.0
     */
    override suspend fun refresh(rejected: Secret?): Boolean {
        val after = changed { found -> found.refreshed(rejected) }
        val outcome = after.last
        check(outcome !is RefreshOutcome.Fail) { outcome.toString() }
        return outcome is RefreshOutcome.Rotate || outcome is RefreshOutcome.Keep
    }

    private suspend fun changed(change: (TokenState) -> TokenState): TokenState = lock.withLock {
        val after = change(state.load())
        state.store(after)
        after
    }

    /**
     * Shows the counts, never the token.
     *
     * @return the text.
     * @since 1.0.0
     */
    override fun toString(): String {
        val found = state.load()
        val token = SafeText.REDACTED.takeIf { found.token != null } ?: "none"
        return "FakeTokenSource(token=$token, tokenRequests=${found.tokenRequests}, " +
            "refreshRequests=${found.rejected.size})"
    }
}
