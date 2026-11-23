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

package com.davils.kapi.spec.asyncapi

import com.davils.kapi.spec.asyncapi.fixture.document
import com.davils.kapi.spec.asyncapi.fixture.readingOf
import com.davils.kapi.spec.asyncapi.fixture.readingOfText
import com.davils.kapi.spec.asyncapi.fixture.rewritten
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

class WritingTest : FunSpec({
    context("a written document") {
        test("starts with its version and keeps the order of its fields") {
            val document = readingOf("channels:\n  chat:\n    address: chat").document()
            val text = AsyncApi.write(document)

            text.lines().first() shouldBe "asyncapi: 3.0.0"
            text shouldContain "channels:"
        }

        test("leaves out what the document leaves out") {
            val text = AsyncApi.write(readingOf("channels:\n  chat:\n    address: null").document())

            text shouldNotContain "address"
            text shouldNotContain "components"
        }

        test("quotes text that would read back as something else") {
            val document = readingOf(
                """
                    |channels:
                    |  chat:
                    |    address: 'true'
                    |    title: '- not a list'
                    |    description: "line\nbreak: here"
                """.trimMargin(),
            ).document()

            document.rewritten() shouldBe document
        }

        test("keeps numbers, nulls and extensions") {
            val document = readingOf(
                """
                    |x-count: 3
                    |x-ratio: -1.5e3
                    |x-none: null
                    |x-list: [1, two, false]
                    |components:
                    |  messages:
                    |    Event:
                    |      examples:
                    |        - payload:
                    |            count: 12
                """.trimMargin(),
            ).document()

            document.rewritten() shouldBe document
        }

        test("reads back from JSON to the same document") {
            val head = """{"asyncapi": "3.0.0", "info": {"title": "Chat", "version": "1.0.0"}, """
            val json = head + """"channels": {"chat": {"address": "chat", "messages": {"said": {"payload": {}}}}}}"""
            val document = readingOfText(json).document()

            document.rewritten() shouldBe document
        }
    }
})
