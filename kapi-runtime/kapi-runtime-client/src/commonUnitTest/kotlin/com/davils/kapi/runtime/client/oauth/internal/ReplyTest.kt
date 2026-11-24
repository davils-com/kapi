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

package com.davils.kapi.runtime.client.oauth.internal

import com.davils.kapi.runtime.core.response.headersOf
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain

private const val TOKEN: String = "s3cr3t-access-token"

private const val OK: Int = 200

class ReplyTest : FunSpec({
    context("an answer of an authorization server") {
        test("shows its status and masked headers but never its content") {
            val headers = headersOf(mapOf("Set-Cookie" to listOf("session=$TOKEN")))
            val answered = Reply.Answered(status = OK, headers = headers, content = """{"access_token":"$TOKEN"}""")

            answered.toString() shouldNotContain TOKEN
            answered.toString() shouldBe "Answered($OK, $headers, ${answered.content.length} characters)"
        }
    }
})
