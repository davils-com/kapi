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

package com.davils.kapi.runtime.client

import com.davils.kapi.runtime.client.retry.CallProtection
import com.davils.kapi.runtime.client.retry.CallRetry
import com.davils.kontinuity.core.Environment
import com.davils.kontinuity.core.backoff.exponential
import com.davils.kontinuity.core.backoff.jittered
import com.davils.kontinuity.core.backoff.proportional
import com.davils.kontinuity.core.environment
import com.davils.kontinuity.policy.Policy
import com.davils.kontinuity.policy.resilience
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private const val POLICY_NAME: String = "kapi-client"
private const val RETRY_NAME: String = "kapi-client-retry"
private const val PROTECTION_NAME: String = "kapi-client-protection"
private const val BREAKER_NAME: String = "kapi-client-breaker"

internal fun defaultResilienceOf(builder: ClientSettingsBuilder): Policy? {
    if (!builder.isDefaultResilience) return null
    val environment = environmentOf(builder)
    val backoff = exponential(initial = 200.milliseconds, maximum = 10.seconds).jittered(proportional())
    val retry = CallRetry.retry(name = RETRY_NAME, backoff = backoff, environment = environment)
    return resilience(name = POLICY_NAME, environment = environment) { retry(retry) }
}

internal fun defaultProtectionOf(builder: ClientSettingsBuilder): Policy? {
    if (!builder.isDefaultProtection) return null
    val environment = environmentOf(builder)
    val breaker = CallProtection.circuitBreaker(name = BREAKER_NAME, environment = environment)
    return resilience(name = PROTECTION_NAME, environment = environment) { circuitBreaker(breaker) }
}

private fun environmentOf(builder: ClientSettingsBuilder): Environment = environment {
    tracer = builder.tracer
    timeSource = builder.timeSource
    builder.meters?.let { registry -> meters(registry) }
    builder.events?.let { bus -> events(bus) }
}
