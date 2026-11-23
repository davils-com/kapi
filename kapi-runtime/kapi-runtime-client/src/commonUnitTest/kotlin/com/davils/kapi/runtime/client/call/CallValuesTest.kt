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

package com.davils.kapi.runtime.client.call

import com.davils.kapi.runtime.core.destination.DestinationRefusal
import com.davils.kapi.runtime.core.framing.FramingFailure
import com.davils.kapi.runtime.core.negotiation.MediaType
import com.davils.kapi.runtime.core.response.Headers
import com.davils.kapi.runtime.core.response.headersOf
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.io.bytestring.encodeToByteString
import kotlin.time.Duration.Companion.seconds

private const val LIMIT: Long = 4

private const val STATUS: Int = 418

private const val HOPS: Int = 3

private const val KEY: String = "X-Key"

private const val TAG: String = "\"1\""

private val failures: List<CallFailure> = listOf(
    CallFailure.Unreachable("IOException"),
    CallFailure.TimedOut,
    CallFailure.TooLarge(LIMIT),
    CallFailure.CredentialMissing,
    CallFailure.CredentialMalformed("bearer"),
    CallFailure.Unsent("rewind"),
    CallFailure.PathRefused("id"),
    CallFailure.ParameterRefused("X-Trace"),
    CallFailure.Aborted("broken"),
    CallFailure.Rejected("bulkhead"),
    CallFailure.BaseUrlMissing,
    CallFailure.DestinationRefused(DestinationRefusal.NotPublic),
    CallFailure.TooManyRedirects(HOPS),
    CallFailure.CrossOrigin,
    CallFailure.Framing(FramingFailure.NotUtf8),
    CallFailure.ChannelsNotInstalled,
    CallFailure.HandshakeRefused(STATUS),
)

private val texts: List<String> = listOf(
    "Unreachable(IOException)",
    "TimedOut",
    "TooLarge(4)",
    "CredentialMissing",
    "CredentialMalformed(bearer)",
    "Unsent(rewind)",
    "PathRefused(id)",
    "ParameterRefused(X-Trace)",
    "Aborted(broken)",
    "Rejected(bulkhead)",
    "BaseUrlMissing",
    "DestinationRefused(NotPublic)",
    "TooManyRedirects(3)",
    "CrossOrigin",
    "Framing(NotUtf8)",
    "ChannelsNotInstalled",
    "HandshakeRefused(418)",
)

private fun tagged(name: String): Headers = headersOf(mapOf(name to listOf(TAG)))

private fun answered(name: String): Exchange.Answered =
    Exchange.Answered(status = STATUS, headers = tagged(name), content = "x".encodeToByteString())

class CallValuesTest : FunSpec({
    context("a failure") {
        test("equals one of the same kind and value, hashes like it and shows itself without secrets") {
            failures.forEachIndexed { index, failure ->
                val others = failures.filterIndexed { other, _ -> other != index }

                failure shouldBe failures[index]
                failure.hashCode() shouldBe failures[index].hashCode()
                failure.toString() shouldBe texts[index]
                others.forEach { different -> failure shouldNotBe different }
            }
        }
    }

    context("the values of a call") {
        test("are equal by their parts and show no content") {
            val body = Body(MediaType.Json, "{}".encodeToByteString())
            val use = SecurityUse("key", CredentialPlacement.Header(KEY))
            val placements = listOf(
                CredentialPlacement.Bearer,
                CredentialPlacement.Basic,
                CredentialPlacement.Header("a"),
                CredentialPlacement.Query("a"),
                CredentialPlacement.Cookie("a"),
            )
            val answered = answered("ETag")

            body shouldBe Body(MediaType.Json, "{}".encodeToByteString())
            body.hashCode() shouldBe Body(MediaType.Json, "{}".encodeToByteString()).hashCode()
            body.toString() shouldBe "Body(application/json, 2 bytes)"
            use shouldBe SecurityUse("key", CredentialPlacement.Header(KEY))
            use.hashCode() shouldBe SecurityUse("key", CredentialPlacement.Header(KEY)).hashCode()
            use.toString() shouldBe "SecurityUse(key, Header(X-Key))"
            placements.map(CredentialPlacement::toString) shouldBe
                listOf("Bearer", "Basic", "Header(a)", "Query(a)", "Cookie(a)")
            placements.map(CredentialPlacement::hashCode).toSet().size shouldBe placements.size - 2
            answered shouldBe answered("etag")
            answered.hashCode() shouldBe answered("etag").hashCode()
            answered.toString() shouldBe """Answered(418, Headers([etag=["1"]]), 1 bytes)"""
            answered.contentType shouldBe null
            Exchange.Failed(CallFailure.TimedOut).toString() shouldBe "Failed(TimedOut)"
            Exchange.Failed(CallFailure.TimedOut).hashCode() shouldBe CallFailure.TimedOut.hashCode()
            Timeout(request = 1.seconds) shouldBe Timeout(request = 1.seconds)
            Timeout(request = 1.seconds).hashCode() shouldBe Timeout(request = 1.seconds).hashCode()
            Timeout() shouldNotBe Timeout(connect = 1.seconds)
            RequestAttributes.upgrade.name shouldBe "kapi.client.upgrade"
        }
    }
})
