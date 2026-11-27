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
import com.davils.kontinuity.circuitbreaker.CircuitBreaker
import com.davils.kontinuity.circuitbreaker.circuitBreaker
import com.davils.kontinuity.core.Environment
import com.davils.kontinuity.core.filter.ResultFilter
import com.davils.kontinuity.core.filter.results

private val serverErrors: IntRange = 500..599

/**
 * Protects a server from calls of a client that keep failing, with a circuit breaker of kontinuity.
 *
 * Unlike a retry, protection applies to every call, whether it is idempotent or not, because it only decides whether
 * a call is sent at all; it never sends one twice. [results] records an [Attempt] as failed when it could not reach
 * the server, timed out or was answered with a status of 500 to 599; a 4xx answer, 429 included, is the server
 * working as intended and counts as a success. Every value here is immutable and safe to share.
 *
 * @since 1.2.0
 */
public object CallProtection {
    /**
     * The filter of a circuit breaker that records every failed attempt as a failure.
     *
     * @since 1.2.0
     */
    public val results: ResultFilter = results { failIf<Attempt> { attempt -> isFailure(attempt) } }

    /**
     * Returns whether [attempt] counts against the server: unreachable, timed out or answered with 500 to 599.
     *
     * @param attempt the attempt.
     * @return `true` if it failed.
     * @since 1.2.0
     */
    public fun isFailure(attempt: Attempt): Boolean = when (val exchange = attempt.exchange) {
        is Exchange.Answered -> exchange.status in serverErrors
        is Exchange.Failed -> exchange.failure is CallFailure.Unreachable || exchange.failure == CallFailure.TimedOut
    }

    /**
     * Builds a circuit breaker of kontinuity for calls that records failures by [results] and otherwise keeps the
     * defaults of kontinuity: it opens at a failure rate of half the calls of its sliding window, once the window has
     * seen its minimum number of calls.
     *
     * @param name the name of the breaker.
     * @param environment the environment of kontinuity, with its events, meters and time.
     * @return the breaker, to add to a policy with `circuitBreaker(...)`.
     * @since 1.2.0
     */
    public fun circuitBreaker(name: String, environment: Environment = Environment.Default): CircuitBreaker =
        circuitBreaker(name, environment) { results = CallProtection.results }
}
