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

package com.davils.kapi.runtime.server.operation

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

/**
 * The rate limit of one operation, such as the `x-kapi-rate-limit` of its document: how many requests one
 * [key] may send to the operation per period.
 *
 * It applies in addition to the rate limit of the server, so a request has to pass both and the stricter one wins;
 * a request over it is answered at once with `429 Too Many Requests`, the code
 * [com.davils.kapi.runtime.server.problem.ServerCodes.RATE_LIMITED] and a `Retry-After` of the period. At most 10,000
 * keys per operation are counted at once, and the requests of a new key are refused while every counted key is still
 * active, so a flood of keys cannot lift the limit. A rate limit is immutable.
 *
 * @property requests the requests one key may send per period, from 1 to [MOST_REQUESTS].
 * @property per the period, positive and at most [MOST_PERIOD].
 * @property key whose requests one budget counts; [RateLimitKey.Caller] by default.
 * @throws IllegalArgumentException if [requests] or [per] is out of range.
 * @since 1.2.0
 */
public class OperationRateLimit(
    public val requests: Int,
    public val per: Duration,
    public val key: RateLimitKey = RateLimitKey.Caller,
) {
    init {
        require(requests in 1..MOST_REQUESTS) { "An operation allows from 1 to $MOST_REQUESTS requests per period" }
        require(per.isPositive() && per <= MOST_PERIOD) { "The period of a rate limit is positive and at most a day" }
    }

    override fun equals(other: Any?): Boolean =
        other is OperationRateLimit && requests == other.requests && per == other.per && key == other.key

    override fun hashCode(): Int = listOf(requests, per, key).hashCode()

    override fun toString(): String = "OperationRateLimit($requests per $per, $key)"

    /**
     * The ceilings of a rate limit.
     *
     * @since 1.2.0
     */
    public companion object {
        /**
         * The most requests per period, 1,000,000.
         *
         * @since 1.2.0
         */
        public const val MOST_REQUESTS: Int = 1_000_000

        /**
         * The longest period, one day.
         *
         * @since 1.2.0
         */
        public val MOST_PERIOD: Duration = 1.days
    }
}
