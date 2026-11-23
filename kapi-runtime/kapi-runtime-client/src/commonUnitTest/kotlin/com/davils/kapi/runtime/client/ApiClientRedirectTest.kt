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

import com.davils.kapi.runtime.client.call.Call
import com.davils.kapi.runtime.client.call.CallFailure
import com.davils.kapi.runtime.client.call.CredentialPlacement
import com.davils.kapi.runtime.client.call.Exchange
import com.davils.kapi.runtime.client.call.SecurityUse
import com.davils.kapi.runtime.client.call.apiCall
import com.davils.kapi.runtime.client.fixture.BASE
import com.davils.kapi.runtime.client.fixture.Exchanges
import com.davils.kapi.runtime.client.fixture.ok
import com.davils.kapi.runtime.client.fixture.pets
import com.davils.kapi.runtime.client.redirect.redirectPolicy
import com.davils.kapi.runtime.core.destination.DestinationRefusal
import com.davils.kapi.runtime.core.negotiation.MediaType
import com.davils.kapi.runtime.core.security.Secret
import com.davils.kapi.runtime.core.security.credentials
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.flowOf
import kotlinx.io.bytestring.encodeToByteString

private const val OTHER: String = "https://cdn.example.org/files/1"

private const val FOUND: Int = 302

private const val SEE_OTHER: Int = 303

private const val TEMPORARY: Int = 307

private const val HOPS: Int = 2

private const val MOVED: String = "/v1/moved"

private const val EMPTY: String = "{}"

private const val START: String = "$BASE/pets"

private val secured: Call = apiCall(operation = "addPet", method = "POST") {
    path("/pets")
    body(MediaType.Json, EMPTY.encodeToByteString())
    security(listOf(SecurityUse("bearer", CredentialPlacement.Bearer)))
}

private fun MockRequestHandleScope.redirect(status: Int, location: String): HttpResponseData = respond(
    content = "",
    status = HttpStatusCode.fromValue(status),
    headers = headersOf(name = HttpHeaders.Location, value = location),
)

private fun following(answer: suspend MockRequestHandleScope.(String) -> HttpResponseData): Exchanges = Exchanges(
    configure = {
        redirects = redirectPolicy { maxHops = HOPS }
        credentials = credentials { scheme("bearer") { Secret("t1") } }
    },
) { request -> answer(request.url.toString()) }

class ApiClientRedirectTest : FunSpec({
    context("a redirect without a redirect policy") {
        test("is the answer of the call") {
            val exchanges = Exchanges { redirect(FOUND, "/elsewhere") }

            exchanges.execute(pets).shouldBeInstanceOf<Exchange.Answered>().status shouldBe FOUND
            exchanges.requests.size shouldBe 1
        }
    }

    context("a redirect the client follows") {
        test("keeps the method, the content and the credentials on its own origin with 307") {
            val exchanges = following { url -> redirect(TEMPORARY, MOVED).takeIf { url == START } ?: ok(EMPTY) }

            exchanges.withClient { client -> client.execute(secured) }.shouldBeInstanceOf<Exchange.Answered>()
            val moved = exchanges.requests.last()

            moved.url.toString() shouldBe "https://api.example.com/v1/moved"
            moved.method.value shouldBe "POST"
            moved.body.toByteArray().decodeToString() shouldBe EMPTY
            moved.headers[HttpHeaders.Authorization] shouldBe "Bearer t1"
        }

        test("turns into GET without content with 303, and with 302 for a POST") {
            listOf(SEE_OTHER, FOUND).forEach { status ->
                val exchanges = following { url -> redirect(status, "/v1/result").takeIf { url == START } ?: ok(EMPTY) }

                exchanges.execute(secured)
                val moved = exchanges.requests.last()

                moved.method.value shouldBe "GET"
                moved.body.toByteArray().size shouldBe 0
            }
        }

        test("goes to another origin without the credentials") {
            val exchanges = following { url -> redirect(FOUND, OTHER).takeIf { url == START } ?: ok(EMPTY) }

            exchanges.withClient { client -> client.execute(pets.change { security(secured.security.first()) }) }
            val moved = exchanges.requests.last()

            moved.url.toString() shouldBe OTHER
            moved.headers[HttpHeaders.Authorization] shouldBe null
        }

        test("hands the redirect back when the policy keeps calls on their origin") {
            val exchanges = Exchanges(configure = { redirects = redirectPolicy { isCrossOriginAllowed = false } }) {
                redirect(FOUND, OTHER)
            }

            exchanges.execute(pets).shouldBeInstanceOf<Exchange.Answered>().status shouldBe FOUND
            exchanges.requests.size shouldBe 1
        }

        test("is refused when a hop leaves the destination policy") {
            val exchanges = following { _ -> redirect(FOUND, "https://169.254.169.254/latest/meta-data") }

            exchanges.execute(pets) shouldBe
                Exchange.Failed(CallFailure.DestinationRefused(DestinationRefusal.NotPublic))
        }

        test("fails after more hops than the policy follows") {
            val exchanges = following { url -> redirect(FOUND, "$url/again") }

            exchanges.execute(pets) shouldBe Exchange.Failed(CallFailure.TooManyRedirects(HOPS))
            exchanges.requests.size shouldBe HOPS + 1
        }

        test("is not followed for a streamed upload that keeps its method, and a location that is no URL is refused") {
            val upload = apiCall(operation = "build", method = "PUT") {
                path("/pets")
                upload(MediaType.OctetStream, flowOf("x".encodeToByteString()))
            }
            val kept = following { _ -> redirect(TEMPORARY, MOVED) }
            val broken = following { _ -> redirect(FOUND, "http://[") }

            kept.execute(upload).shouldBeInstanceOf<Exchange.Answered>().status shouldBe TEMPORARY
            broken.execute(pets) shouldBe Exchange.Failed(CallFailure.DestinationRefused(DestinationRefusal.Url))
        }

        test("hands a redirect without a location back") {
            val exchanges = following { _ -> respond(content = "", status = HttpStatusCode.Found) }

            exchanges.execute(pets).shouldBeInstanceOf<Exchange.Answered>().status shouldBe FOUND
        }
    }
})
