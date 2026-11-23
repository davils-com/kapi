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

package com.davils.kapi.runtime.client.page

import com.davils.kapi.runtime.client.ApiClient
import com.davils.kapi.runtime.client.call.CallFailure
import com.davils.kapi.runtime.client.call.Exchange
import com.davils.kapi.runtime.client.call.apiCall
import com.davils.kapi.runtime.client.fixture.BASE
import com.davils.kapi.runtime.client.fixture.Exchanges
import com.davils.kapi.runtime.client.fixture.pets
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.toList

private const val PAGES: Int = 3

private fun linked(link: String?) = Exchanges { request ->
    val page = request.url.parameters["page"]?.toInt() ?: 1
    val next = link?.replace(oldValue = "{next}", newValue = "${page + 1}")
    val headers = next?.let { found -> headersOf(name = HttpHeaders.Link, value = found) } ?: headersOf()
    respond(content = "[$page]", status = HttpStatusCode.OK, headers = headers)
}

private fun answering(status: HttpStatusCode, link: String) = Exchanges {
    respond(content = "", status = status, headers = headersOf(name = HttpHeaders.Link, value = link))
}

private suspend fun pagesOf(exchanges: Exchanges, maxPages: Int = PAGES): List<Exchange> =
    exchanges.withClient { client: ApiClient -> client.linkPages(pets, maxPages = maxPages).toList() }

class LinkPagesTest : FunSpec({
    context("pages linked by rel=next") {
        test("follow the link of each page, resolved against its URL, with the query of the link") {
            val exchanges = linked("""<?page={next}>; rel="next"""")
            val first = pets.change { query(name = "size", value = "1") }
            val pages = exchanges.withClient { client -> client.linkPages(first, maxPages = PAGES).toList() }

            pages.size shouldBe PAGES
            exchanges.requests.map { request -> request.url.toString() } shouldBe listOf(
                "$BASE/pets?size=1",
                "$BASE/pets?page=2",
                "$BASE/pets?page=3",
            )
        }

        test("end with a page that names no next link, a failed one, or the URL it was asked with") {
            pagesOf(linked(null)).size shouldBe 1
            pagesOf(answering(HttpStatusCode.NotFound, """</x>; rel="next"""")).size shouldBe 1
            pagesOf(answering(HttpStatusCode.OK, """<$BASE/pets>; rel="next"""")).size shouldBe 1
        }

        test("end with a failure when the next page lies on another origin") {
            val pages = pagesOf(linked("""<https://evil.example.org/pets?page={next}>; rel="next""""))

            pages.size shouldBe 2
            pages.last() shouldBe Exchange.Failed(CallFailure.CrossOrigin)
            pages.first().shouldBeInstanceOf<Exchange.Answered>()
        }

        test("hold at least one page, and none for a call without a URL") {
            val unbased = Exchanges(configure = { baseUrl = "" })
            val delivered = apiCall(operation = "x", method = "GET")

            shouldThrow<IllegalArgumentException> { pagesOf(Exchanges(), maxPages = 0) }
            unbased.withClient { client -> client.linkPages(delivered).toList() } shouldBe
                listOf(Exchange.Failed(CallFailure.BaseUrlMissing))
        }
    }
})
