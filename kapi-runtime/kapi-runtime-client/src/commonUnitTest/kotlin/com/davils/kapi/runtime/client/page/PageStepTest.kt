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

import com.davils.kapi.runtime.client.call.CallFailure
import com.davils.kapi.runtime.client.call.Exchange
import com.davils.kapi.runtime.client.call.apiCall
import com.davils.kapi.runtime.client.fixture.Exchanges
import com.davils.kapi.runtime.client.fixture.pets
import com.davils.kapi.runtime.client.internal.PageStep
import com.davils.kapi.runtime.core.response.headersOf
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.io.bytestring.ByteString

private const val OK: Int = 200

private const val PETS: String = "https://pets.example.com/v2/pets"

private fun linked(link: String): Exchange =
    Exchange.Answered(status = OK, headers = headersOf(mapOf("Link" to listOf(link))), content = ByteString())

class PageStepTest : FunSpec({
    context("the next step of a page") {
        test("is none without a next link, without a URL, or for a link that cannot be resolved") {
            val unbased = Exchanges(configure = { baseUrl = "" })
            val next = linked("""</v1/pets?page=2>; rel="next"""")

            Exchanges().withClient { client ->
                listOf(
                    client.nextOf(pets, linked("""</prev>; rel="prev"""")),
                    client.nextOf(pets, linked("""<https://api.example.com:99999999999/x>; rel="next"""")),
                )
            } shouldBe listOf(PageStep(), PageStep())
            unbased.withClient { client -> client.nextOf(pets, next) } shouldBe PageStep()
        }

        test("follows a call of its own destination, and refuses another origin") {
            val own = apiCall(operation = "listPets", method = "GET") { destination(PETS) }
            val unbased = Exchanges(configure = { baseUrl = "" })

            val steps = unbased.withClient { client ->
                listOf(
                    client.nextOf(own, linked("""<?page=2>; rel="next"""")),
                    client.nextOf(own, linked("""<https://evil.example.com/x>; rel="next"""")),
                )
            }

            steps.first().call?.destination shouldBe "$PETS?page=2"
            steps.last() shouldBe PageStep(failure = Exchange.Failed(CallFailure.CrossOrigin))
        }

        test("has no URL for a destination that cannot be read") {
            val broken = apiCall(
                operation = "listPets",
                method = "GET",
            ) { destination("https://a.example.com:99999999999/") }

            Exchanges().withClient { client -> client.urlOf(broken) } shouldBe null
        }
    }
})
