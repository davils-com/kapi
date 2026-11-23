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

import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.Clock
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Instant
import kotlin.time.TimeSource

/**
 * A clock for tests that moves only when the test moves it: a wall [Clock] for the crypto environment of krypt, which
 * signs client assertions and DPoP proofs with it, and a monotonic [TimeSource] for the expiry of tokens in
 * [com.davils.kapi.runtime.client.oauth.OAuthSettingsBuilder.timeSource] and the timing of
 * [com.davils.kapi.runtime.client.ClientSettingsBuilder.timeSource].
 *
 * ```kotlin
 * val clock = ManualClock()
 * val tokens = oauthTokens(http) { timeSource = clock; crypto = deterministicCrypto(clock = clock) }
 * clock.advance(1.hours)
 * ```
 *
 * Both readings advance together. A clock is safe to read and advance from several threads at once.
 *
 * @param start the instant of the wall clock at the start; [DEFAULT_START] by default.
 * @since 1.0.0
 */
public class ManualClock(private val start: Instant = DEFAULT_START) : Clock, TimeSource.WithComparableMarks {
    private val elapsed: AtomicLong = AtomicLong(0L)

    private val marks: TimeSource.WithComparableMarks = MarkSource { elapsed.load() }

    /**
     * Returns the instant of the wall clock.
     *
     * @return the instant.
     * @since 1.0.0
     */
    override fun now(): Instant = start + elapsed.load().nanoseconds

    /**
     * Marks the current point of the monotonic time source.
     *
     * @return the mark, which moves away from the present only when the clock is advanced.
     * @since 1.0.0
     */
    override fun markNow(): ComparableTimeMark = marks.markNow()

    /**
     * Moves both readings forward by [duration].
     *
     * @param duration how far to move, finite and not negative, in whole nanoseconds.
     * @throws IllegalArgumentException if [duration] is negative or infinite.
     * @since 1.0.0
     */
    public fun advance(duration: Duration) {
        require(duration.isFinite() && !duration.isNegative()) { "A clock moves forward by a finite duration" }
        elapsed.addAndFetch(duration.inWholeNanoseconds)
    }

    /**
     * Shows the instant of the wall clock.
     *
     * @return the text.
     * @since 1.0.0
     */
    override fun toString(): String = "ManualClock(${now()})"

    /**
     * The defaults of a manual clock.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The default start: 2026-01-01T00:00:00Z.
         *
         * @since 1.0.0
         */
        public val DEFAULT_START: Instant = Instant.fromEpochSeconds(1_767_225_600L)
    }
}
