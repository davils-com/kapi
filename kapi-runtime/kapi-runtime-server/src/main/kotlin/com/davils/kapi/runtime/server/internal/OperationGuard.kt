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

import com.davils.kapi.runtime.core.response.HttpStatus
import com.davils.kapi.runtime.server.ServerSettings
import com.davils.kapi.runtime.server.problem.ServerCodes
import com.davils.kapi.runtime.server.reply.Reply
import com.davils.kapi.runtime.server.request.Request
import com.davils.kontinuity.bulkhead.Bulkhead
import com.davils.kontinuity.bulkhead.bulkhead
import com.davils.kontinuity.core.Environment
import com.davils.kontinuity.core.environment
import com.davils.kontinuity.ratelimiter.KeyedRateLimiter
import com.davils.kontinuity.ratelimiter.RateLimiterConfig
import com.davils.kontinuity.ratelimiter.keyedRateLimiter
import kotlin.time.Duration
import kotlin.time.DurationUnit

private const val BULKHEAD: String = "kapi-server-operations"
private const val LIMITER: String = "kapi-server-callers"
private const val RETRY_AFTER: String = "Retry-After"
private const val ANONYMOUS: String = "anonymous"
private const val PRINCIPAL: String = "principal:"
private const val ADDRESS: String = "address:"

internal class OperationGuard(settings: ServerSettings, private val problems: ProblemReplies) {
    private val environment: Environment = environmentOf(settings)

    private val bulkhead: Bulkhead = bulkhead(name = BULKHEAD, environment = environment) {
        maxConcurrentCalls = settings.limits.maxConcurrentRequests
        maxWaitDuration = Duration.ZERO
    }

    private val limiter: KeyedRateLimiter<String>? = settings.rateLimit?.let { config -> limiterOf(config) }

    private val retryAfter: String = retryAfterOf(settings.rateLimit)

    suspend fun guarded(request: Request, block: suspend () -> Reply): Reply {
        val isAllowed = limiter?.tryAcquire(callerOf(request)) != false
        if (!isAllowed) return limited()
        return bulkheaded(block)
    }

    private suspend fun bulkheaded(block: suspend () -> Reply): Reply {
        val permit = bulkhead.tryAcquirePermission() ?: return overloaded()
        try {
            return block()
        } finally {
            permit.release()
        }
    }

    private fun limited(): Reply = problems.problem(
        status = HttpStatus.TOO_MANY_REQUESTS,
        code = ServerCodes.RATE_LIMITED,
        headers = listOf(RETRY_AFTER to retryAfter),
    )

    private fun overloaded(): Reply =
        problems.problem(status = HttpStatus.SERVICE_UNAVAILABLE, code = ServerCodes.OVERLOADED)

    private fun limiterOf(config: RateLimiterConfig): KeyedRateLimiter<String> = keyedRateLimiter(
        name = LIMITER,
        idleTimeout = maxOf(a = config.limitRefreshPeriod, b = KeyedRateLimiter.DEFAULT_IDLE_TIMEOUT),
        environment = environment,
    ) {
        limitForPeriod = config.limitForPeriod
        limitRefreshPeriod = config.limitRefreshPeriod
        timeout = Duration.ZERO
        algorithm = config.algorithm
    }

    private companion object {
        fun environmentOf(settings: ServerSettings): Environment = environment {
            val telemetry = settings.telemetry
            tracer = telemetry.tracer
            telemetry.meters?.let { registry -> meters(registry) }
            telemetry.events?.let { bus -> events(bus) }
        }

        fun callerOf(request: Request): String {
            val principal = request.principal?.id
            if (principal != null) return PRINCIPAL + principal
            return request.peer.address?.let { address -> ADDRESS + address } ?: ANONYMOUS
        }

        fun retryAfterOf(config: RateLimiterConfig?): String {
            val period = config?.limitRefreshPeriod ?: return ""
            val seconds = period.toLong(DurationUnit.SECONDS)
            return seconds.coerceAtLeast(1L).toString()
        }
    }
}
