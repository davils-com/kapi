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
import com.davils.kapi.runtime.server.operation.Operation
import com.davils.kapi.runtime.server.operation.RateLimitKey
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
import com.davils.kontinuity.ratelimiter.rateLimiterConfig
import com.davils.kontinuity.ratelimiter.tokenBucket
import kotlin.time.Duration

private const val BULKHEAD: String = "kapi-server-operations"
private const val LIMITER: String = "kapi-server-callers"
private const val OPERATION_LIMITER: String = "kapi-server-operation-callers"
private const val RETRY_AFTER: String = "Retry-After"

internal class OperationGuard(
    settings: ServerSettings,
    operation: Operation,
    private val problems: ProblemReplies,
) {
    private val environment: Environment = environmentOf(settings)

    private val bulkhead: Bulkhead = bulkhead(name = BULKHEAD, environment = environment) {
        maxConcurrentCalls = settings.limits.maxConcurrentRequests
        maxWaitDuration = Duration.ZERO
    }

    private val server: CallerLimit? = settings.rateLimit?.let { config ->
        CallerLimit(limiterOf(LIMITER, config), RateLimitKey.Caller, config.limitRefreshPeriod)
    }

    private val own: CallerLimit? = operation.rateLimit?.let { limit ->
        val config = rateLimiterConfig {
            limitForPeriod = limit.requests
            limitRefreshPeriod = limit.per
            timeout = Duration.ZERO
            algorithm = tokenBucket(burst = limit.requests)
        }
        CallerLimit(limiterOf(OPERATION_LIMITER, config), limit.key, limit.per)
    }

    suspend fun guarded(request: Request, block: suspend () -> Reply): Reply {
        val refusing = listOfNotNull(server, own).firstOrNull { limit -> !limit.admits(request) }
        if (refusing != null) return limited(refusing.retryAfter)
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

    private fun limited(retryAfter: String): Reply = problems.problem(
        status = HttpStatus.TOO_MANY_REQUESTS,
        code = ServerCodes.RATE_LIMITED,
        headers = listOf(RETRY_AFTER to retryAfter),
    )

    private fun overloaded(): Reply =
        problems.problem(status = HttpStatus.SERVICE_UNAVAILABLE, code = ServerCodes.OVERLOADED)

    private fun limiterOf(name: String, config: RateLimiterConfig): KeyedRateLimiter<String> = keyedRateLimiter(
        name = name,
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
    }
}
