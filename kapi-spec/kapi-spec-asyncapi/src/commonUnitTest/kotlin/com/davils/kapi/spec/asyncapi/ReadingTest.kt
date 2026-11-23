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

import com.davils.kapi.spec.asyncapi.fixture.codes
import com.davils.kapi.spec.asyncapi.fixture.head
import com.davils.kapi.spec.asyncapi.fixture.reading
import com.davils.kapi.spec.asyncapi.fixture.readingOfText
import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.core.source.Source
import com.davils.kapi.spec.core.value.ExtensionName
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

class ReadingTest : FunSpec({
    context("a document that cannot be read") {
        test("is malformed when it is no object") {
            val reading = readingOfText("- a\n- b\n")

            reading.shouldBeInstanceOf<Reading.Malformed>()
            reading.codes() shouldBe listOf(DiagnosticCodes.DOCUMENT_NOT_MAPPING)
            reading.toString() shouldBe "Malformed(1 diagnostics)"
        }

        test("is malformed when it is empty") {
            readingOfText("").codes() shouldBe listOf(DiagnosticCodes.DOCUMENT_NOT_MAPPING)
        }

        test("is malformed when it names no version") {
            val reading = readingOfText("info:\n  title: Chat\n")

            reading.shouldBeInstanceOf<Reading.Malformed>()
            reading.codes() shouldBe listOf(DiagnosticCodes.VERSION_MISSING)
        }

        test("is malformed when it is a document of AsyncAPI 2") {
            val reading =
                readingOfText(
                    """
                        |asyncapi: 2.6.0
                        |info:
                        |  title: Chat
                        |  version: 1.0.0
                        |
                    """.trimMargin(),
                )

            reading.codes() shouldBe listOf(DiagnosticCodes.VERSION_UNSUPPORTED)
            reading.diagnostics.single().message shouldContain "AsyncAPI \"2.6.0\" is not read"
        }

        test("is malformed when it is no YAML") {
            readingOfText("asyncapi: [3.0.0\n").shouldBeInstanceOf<Reading.Malformed>()
        }
    }

    context("a document that can be read") {
        test("is read with its sources and without findings") {
            val reading = reading(head)
            val read = reading.shouldBeInstanceOf<Reading.Read>()

            read.diagnostics.shouldBeEmpty()
            read.sources.single().uri shouldBe DocumentUri("memory:/api/asyncapi.yaml")
            read.document.info.title shouldBe "Chat"
            read.toString() shouldBe "Read(0 diagnostics)"
        }

        test("is invalid when it breaks a rule, and keeps what it could read") {
            val reading = reading(head + "x-team: chat" + "unknown: 1")
            val invalid = reading.shouldBeInstanceOf<Reading.Invalid>()

            invalid.codes() shouldBe listOf(DiagnosticCodes.FIELD_UNKNOWN)
            invalid.partial.extensions[ExtensionName("x-team")].shouldBeInstanceOf<Any>()
        }

        test("reports a version without a patch") {
            val reading = reading(listOf("asyncapi: '3.0'") + head.drop(1))

            reading.codes() shouldBe listOf(DiagnosticCodes.VERSION_PATCH_MISSING)
            reading.diagnostics.single().message shouldContain "3.0.0"
        }

        test("reads JSON as well as YAML") {
            val text = """{"asyncapi": "3.0.0", "info": {"title": "Chat", "version": "1.0.0"}, "channels": {}}"""

            readingOfText(text).shouldBeInstanceOf<Reading.Read>()
        }

        test("is equal to a reading of the same text, finding for finding") {
            val reader = AsyncApi.reader()
            val text =
                """
                    |asyncapi: 3.0.0
                    |info: {title: Chat, version: 1.0.0}
                    |unknown: 1
                    |
                """.trimMargin()
            val first = reader.read(Source.Text(text))
            val second = reader.read(Source.Text(text))
            val other = reader.read(Source.Text("asyncapi: 3.0.0\ninfo: {title: Chat, version: 1.0.0}\n"))

            first shouldBe second
            first.hashCode() shouldBe second.hashCode()
            first shouldNotBe other
            readingOfText("[]") shouldBe readingOfText("[]")
            first.equals(text) shouldBe false
        }
    }
})
