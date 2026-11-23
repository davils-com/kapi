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

import com.davils.kapi.runtime.client.call.Call
import com.davils.kapi.runtime.client.call.CallFailure
import com.davils.kapi.runtime.client.call.Exchange
import com.davils.kapi.runtime.client.call.apiCall
import com.davils.kapi.runtime.client.fixture.pets
import com.davils.kapi.runtime.core.conditional.HttpDate
import com.davils.kapi.runtime.core.response.headersOf
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.io.bytestring.ByteString
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

private const val NOW: Long = 1_700_000_000

private const val UNAVAILABLE: Int = 503

private const val TOO_MANY: Int = 429

private const val ATTEMPT: Int = 2

private const val OK: Int = 200

private const val BAD_REQUEST: Int = 400

private const val UNAUTHORIZED: Int = 401

private const val NOT_FOUND: Int = 404

private const val NOT_IMPLEMENTED: Int = 501

private const val RETRY_AFTER: String = "Retry-After"

private const val RATE_LIMIT: String = "RateLimit"

private const val SECONDS: Int = 7

private const val LATER: Long = 30

private val post: Call = apiCall(operation = "addPet", method = "POST")

private val clock: Clock = object : Clock {
    override fun now(): Instant = Instant.fromEpochSeconds(NOW)
}

private fun answered(status: Int, vararg headers: Pair<String, String>): Exchange = Exchange.Answered(
    status = status,
    headers = headersOf(headers.associate { (name, value) -> name to listOf(value) }),
    content = ByteString(),
)

private fun delayOf(exchange: Exchange, call: Call = pets) =
    CallRetry.interval(maximum = 1.minutes, clock = clock).delayBefore(ATTEMPT, null, Attempt(call, exchange))

private val transientAnswers: List<Exchange> = CallRetry.transientStatuses.map { status -> answered(status) }

private val transientFailures: List<Exchange> =
    listOf(Exchange.Failed(CallFailure.Unreachable("IOException")), Exchange.Failed(CallFailure.TimedOut))

private val transient: List<Exchange> = transientAnswers + transientFailures

private val lastingStatuses: List<Int> = listOf(OK, BAD_REQUEST, UNAUTHORIZED, NOT_FOUND, NOT_IMPLEMENTED)

private val lastingFailures: List<Exchange> =
    listOf(Exchange.Failed(CallFailure.CredentialMissing), Exchange.Failed(CallFailure.Aborted("broken")))

private val lasting: List<Exchange> = lastingStatuses.map { status -> answered(status) } + lastingFailures

class CallRetryTest : FunSpec({
    context("an attempt of a retryable call") {
        transient.forEach { exchange ->
            test("is tried again after $exchange") {
                CallRetry.isTransient(Attempt(pets, exchange)) shouldBe true
                CallRetry.results.isFailure(Attempt(pets, exchange)) shouldBe true
            }
        }

        lasting.forEach { exchange ->
            test("is not tried again after $exchange") {
                CallRetry.isTransient(Attempt(pets, exchange)) shouldBe false
            }
        }
    }

    context("an attempt of a call that is neither idempotent nor keyed") {
        test("is never tried again, whatever came back") {
            transient.forEach { exchange -> CallRetry.isTransient(Attempt(post, exchange)) shouldBe false }
            CallRetry.isTransient(Attempt(post.change { idempotencyKey() }, answered(UNAVAILABLE))) shouldBe true
            CallRetry.results.isFailure("text") shouldBe false
        }
    }

    context("the wait before the next attempt") {
        test("is the delay or the date of Retry-After, at most the maximum") {
            delayOf(answered(UNAVAILABLE, RETRY_AFTER to "$SECONDS")) shouldBe SECONDS.seconds
            delayOf(answered(UNAVAILABLE, RETRY_AFTER to HttpDate(NOW + LATER).toString())) shouldBe LATER.seconds
            delayOf(answered(UNAVAILABLE, RETRY_AFTER to "3600")) shouldBe 1.minutes
        }

        test("is the reset of a spent quota of RateLimit") {
            delayOf(answered(TOO_MANY, RATE_LIMIT to "\"default\";r=0;t=12")) shouldBe 12.seconds
            delayOf(answered(TOO_MANY, RATE_LIMIT to "\"default\";r=5;t=12")) shouldBe null
            delayOf(answered(TOO_MANY, RATE_LIMIT to "\"default\";r=0;t=0")) shouldBe null
        }

        test("is left to the backoff for a past date, an unreadable field or a failure") {
            delayOf(answered(UNAVAILABLE, RETRY_AFTER to HttpDate(NOW - LATER).toString())) shouldBe null
            delayOf(answered(UNAVAILABLE, RETRY_AFTER to "soon")) shouldBe null
            delayOf(Exchange.Failed(CallFailure.TimedOut)) shouldBe null
            CallRetry.interval().delayBefore(ATTEMPT, IllegalStateException("no"), null) shouldBe null
        }

        test("may be built into a retry of kontinuity") {
            val retry = CallRetry.retry(name = "pets", maxAttempts = 2, maximumServerDelay = 5.milliseconds)

            retry.config.maxAttempts shouldBe 2
            retry.config.results shouldBe CallRetry.results
        }
    }
})
