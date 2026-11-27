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

package com.davils.kapi.runtime.server.internal

import com.davils.kapi.runtime.server.operation.RateLimitKey
import com.davils.kapi.runtime.server.request.Request
import com.davils.kontinuity.ratelimiter.KeyedRateLimiter
import kotlin.time.Duration
import kotlin.time.DurationUnit

private const val ANONYMOUS: String = "anonymous"

private const val PRINCIPAL: String = "principal:"

private const val ADDRESS: String = "address:"

private fun secondsOf(period: Duration): Long {
    val seconds = period.toLong(DurationUnit.SECONDS)
    return seconds.coerceAtLeast(1L)
}

internal class CallerLimit(
    private val limiter: KeyedRateLimiter<String>,
    private val key: RateLimitKey,
    period: Duration,
) {
    val retryAfter: String = secondsOf(period).toString()

    fun admits(request: Request): Boolean = limiter.tryAcquire(keyOf(request))

    private fun keyOf(request: Request): String = when (key) {
        RateLimitKey.Caller -> principalOf(request) ?: addressOf(request)
        RateLimitKey.Principal -> principalOf(request) ?: ANONYMOUS
        RateLimitKey.Address -> addressOf(request)
    }

    private fun principalOf(request: Request): String? {
        val principal = request.principal ?: return null
        return PRINCIPAL + principal.id
    }

    private fun addressOf(request: Request): String {
        val address = request.peer.address ?: return ANONYMOUS
        return ADDRESS + address
    }
}
