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

import com.davils.kapi.runtime.client.call.Timeout
import com.davils.kapi.runtime.client.fixture.BASE
import com.davils.kapi.runtime.client.pipeline.CallPhase
import com.davils.kapi.runtime.client.pipeline.Interceptor
import com.davils.kapi.runtime.client.redirect.RedirectPolicy
import com.davils.kapi.runtime.client.redirect.redirectPolicy
import com.davils.kore.core.validation.report.ValidationException
import com.davils.kore.core.validation.violation.ViolationCode
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val HOPS: Int = 21

private const val DEFAULT_HOPS: Int = 5

private val keep: Interceptor = Interceptor { trip -> trip }

private val invalidTimeouts: List<Timeout> = listOf(
    Timeout(request = Duration.ZERO),
    Timeout(connect = (-1).seconds),
    Timeout(socket = Duration.INFINITE),
)

private fun codesOf(configure: ClientSettingsBuilder.() -> Unit): List<ViolationCode> {
    val builder = ClientSettingsBuilder(isFollowingRedirects = false)
    builder.baseUrl = BASE
    builder.configure()
    return builder.validate().violations.map { violation -> violation.code }
}

class ApiClientSettingsTest : FunSpec({
    context("the client of an HttpClient that follows redirects itself") {
        test("is refused, because a redirect would carry the credentials wherever a server points") {
            val failure = HttpClient(MockEngine { respondOk() }).use { http ->
                shouldThrow<ValidationException> { apiClient(http) { baseUrl = BASE } }
            }

            failure.report.violations.map { violation -> violation.code } shouldBe
                listOf(ClientViolationCodes.REDIRECTS_FOLLOWED)
        }
    }

    context("the name of a client") {
        test("is api unless the application names it, and may be several words") {
            codesOf { }.shouldBeEmpty()
            codesOf { name = "pet-store.v2_x" }.shouldBeEmpty()
            ClientSettingsBuilder.DEFAULT_NAME shouldBe "api"
        }

        listOf("Petstore", "pet store", "", "pets_", "pets..v2", "2pets").forEach { name ->
            test("is refused when it is no lower case words joined by a separator: '$name'") {
                codesOf { this.name = name } shouldBe listOf(ClientViolationCodes.NAME_INVALID)
            }
        }
    }

    context("the base of a client") {
        listOf("ftp://example.com", "https://example.com?x=1", "https://user@example.com", "https://ex ample.com", "x")
            .forEach { base ->
                test("is refused when it is no plain http URL: '$base'") {
                    codesOf { baseUrl = base } shouldBe listOf(ClientViolationCodes.BASE_URL_INVALID)
                }
            }

        test("may be empty") {
            codesOf { baseUrl = "" }.shouldBeEmpty()
        }
    }

    context("the limits of a client") {
        test("must be above zero and below the hard ceiling") {
            codesOf { maxResponseBytes = 0 } shouldBe listOf(ClientViolationCodes.LIMIT_INVALID)
            codesOf { maxResponseBytes = ClientSettings.HARD_RESPONSE_BYTES + 1 } shouldBe
                listOf(ClientViolationCodes.LIMIT_INVALID)
        }

        invalidTimeouts.forEach { timeout ->
            test("refuse a timeout that is not above zero and finite: $timeout") {
                codesOf { this.timeout = timeout } shouldBe listOf(ClientViolationCodes.TIMEOUT_INVALID)
            }
        }
    }

    context("the interceptors of a client") {
        test("each need a name of their own") {
            codesOf {
                intercept(CallPhase.Prepare, "a", keep)
                intercept(CallPhase.Send, "a", keep)
            } shouldBe listOf(ClientViolationCodes.INTERCEPTOR_INVALID)
            codesOf { intercept(CallPhase.Prepare, " ", keep) } shouldBe
                listOf(ClientViolationCodes.INTERCEPTOR_INVALID)
        }
    }

    context("a redirect policy") {
        test("follows five hops by default and at most twenty") {
            redirectPolicy().maxHops shouldBe DEFAULT_HOPS
            redirectPolicy().toString() shouldBe "RedirectPolicy(maxHops=5, isCrossOriginAllowed=true)"
            shouldThrow<ValidationException> { redirectPolicy { maxHops = HOPS } }
            RedirectPolicy.HARD_HOPS shouldBe HOPS - 1
        }
    }
})
