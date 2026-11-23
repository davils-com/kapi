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

package com.davils.kapi.runtime.client.retry

import com.davils.kapi.runtime.client.call.CallFailure
import com.davils.kapi.runtime.client.call.Exchange
import com.davils.kapi.runtime.core.conditional.HttpDate
import com.davils.kapi.runtime.core.header.RetryAfter
import com.davils.kapi.runtime.core.ratelimit.RateLimits
import com.davils.kontinuity.core.Environment
import com.davils.kontinuity.core.backoff.Backoff
import com.davils.kontinuity.core.backoff.exponential
import com.davils.kontinuity.core.filter.ResultFilter
import com.davils.kontinuity.core.filter.results
import com.davils.kontinuity.retry.Retry
import com.davils.kontinuity.retry.RetryInterval
import com.davils.kontinuity.retry.retry
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private const val RETRY_AFTER: String = "retry-after"

private const val RATE_LIMIT: String = "ratelimit"

private const val DEFAULT_ATTEMPTS: Int = 3

private val defaultBackoff: Backoff = exponential(initial = 200.milliseconds, maximum = 10.seconds)

/**
 * Retries calls under a retry of kontinuity, as HTTP and the servers of an API ask.
 *
 * [results] retries an [Attempt] of a retryable call, one that is idempotent by its method or its document or carries
 * an `Idempotency-Key`, that ended with a status of [transientStatuses], could not reach the server or timed out.
 * [interval] waits what the server asked for in `Retry-After` of RFC 9110 §10.2.3 or, when a quota is spent, until
 * the `RateLimit` field of the IETF draft `draft-ietf-httpapi-ratelimit-headers` resets it, capped at a maximum, and
 * else the backoff of the retry. A call that is not retryable is never retried, whatever the policy says, because the
 * client runs it outside of the policy. Every value here is immutable and safe to share.
 *
 * @since 1.0.0
 */
public object CallRetry {
    /**
     * The statuses a server answers when a later attempt may succeed: 408, 425, 429, 500, 502, 503 and 504.
     *
     * @since 1.0.0
     */
    public val transientStatuses: Set<Int> = setOf(408, 425, 429, 500, 502, 503, 504)

    /**
     * The filter of a retry that fails, and so retries, every transient attempt of a retryable call.
     *
     * @since 1.0.0
     */
    public val results: ResultFilter = results { failIf<Attempt> { attempt -> isTransient(attempt) } }

    /**
     * Returns whether [attempt] is worth another try: its call is retryable and it ended with a transient status,
     * unreachable or timed out.
     *
     * @param attempt the attempt.
     * @return `true` to try again.
     * @since 1.0.0
     */
    public fun isTransient(attempt: Attempt): Boolean = attempt.call.isRetryable && isTransient(attempt.exchange)

    /**
     * Returns the interval that waits the delay the server asked for, at most [maximum].
     *
     * A `Retry-After` delay or date wins over a `RateLimit` reset; a value that cannot be read, a negative one and a
     * date in the past leave the wait to the backoff. The server decides the wait, so [maximum] caps it.
     *
     * @param maximum the longest wait a server may ask for.
     * @param clock the clock a `Retry-After` date is measured from.
     * @return the interval.
     * @since 1.0.0
     */
    public fun interval(maximum: Duration = 1.minutes, clock: Clock = Clock.System): RetryInterval =
        RetryInterval { _, _, result ->
            val answered = (result as? Attempt)?.exchange as? Exchange.Answered
            val delay = answered?.let { found -> serverDelay(found, clock) }
            delay?.coerceAtMost(maximum)
        }

    /**
     * Builds a retry of kontinuity for calls: [results] decides, [interval] waits for the server, else [backoff].
     *
     * @param name the name of the retry.
     * @param maxAttempts the most attempts, the first included.
     * @param backoff the wait when the server asks for none.
     * @param maximumServerDelay the longest wait a server may ask for.
     * @param environment the environment of kontinuity, with its events, meters and time.
     * @return the retry, to add to a policy with `retry(...)`.
     * @throws com.davils.kore.core.validation.report.ValidationException if [maxAttempts] or [backoff] are out of the
     * bounds of kontinuity.
     * @since 1.0.0
     */
    public fun retry(
        name: String,
        maxAttempts: Int = DEFAULT_ATTEMPTS,
        backoff: Backoff = defaultBackoff,
        maximumServerDelay: Duration = 1.minutes,
        environment: Environment = Environment.Default,
    ): Retry = retry(name, environment) {
        this.maxAttempts = maxAttempts
        this.backoff = backoff
        results = CallRetry.results
        interval = CallRetry.interval(maximumServerDelay)
    }

    private fun isTransient(exchange: Exchange): Boolean = when (exchange) {
        is Exchange.Answered -> exchange.status in transientStatuses
        is Exchange.Failed -> exchange.failure is CallFailure.Unreachable || exchange.failure == CallFailure.TimedOut
    }

    private fun serverDelay(answered: Exchange.Answered, clock: Clock): Duration? {
        val now = HttpDate(clock.now().epochSeconds)
        val retryAfter = answered.headers[RETRY_AFTER]?.let { text -> RetryAfter.parseOrNull(text, now) }
        val requested = retryAfter?.run { secondsFrom(now).takeIf { seconds -> seconds > 0 } }
        return requested?.seconds ?: resetOf(answered)
    }

    private fun resetOf(answered: Exchange.Answered): Duration? {
        val limits = answered.headers[RATE_LIMIT]?.let(RateLimits::parseLimits).orEmpty()
        val spent = limits.filter { limit -> limit.remaining <= 0 }
        val resets = spent.mapNotNull { limit -> limit.reset }
        val longest = resets.maxOrNull()?.takeIf { seconds -> seconds > 0 }
        return longest?.seconds
    }
}
