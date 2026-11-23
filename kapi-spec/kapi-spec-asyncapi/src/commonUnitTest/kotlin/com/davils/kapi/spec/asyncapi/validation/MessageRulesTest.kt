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

package com.davils.kapi.spec.asyncapi.validation

import com.davils.kapi.spec.asyncapi.DiagnosticCodes
import com.davils.kapi.spec.asyncapi.fixture.codes
import com.davils.kapi.spec.asyncapi.fixture.readingOf
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

private const val FINDINGS: Int = 3

class MessageRulesTest : FunSpec({
    context("a runtime expression") {
        test("of a correlation id, a reply address and a parameter is checked") {
            val reading = readingOf(
                """
                    |channels:
                    |  room:
                    |    address: '{id}'
                    |    parameters:
                    |      id:
                    |        location: ${'$'}message.body#/id
                    |components:
                    |  correlationIds:
                    |    good:
                    |      location: ${'$'}message.header#/correlationId
                    |    header:
                    |      location: ${'$'}message.header
                    |    bad:
                    |      location: message.header#/id
                    |  replyAddresses:
                    |    pointer:
                    |      location: ${'$'}message.payload#id
                """.trimMargin(),
            )

            val expressions = List(FINDINGS) { DiagnosticCodes.RUNTIME_EXPRESSION }
            reading.codes() shouldBe expressions.take(1) + DiagnosticCodes.STRUCTURE_SCHEMA + expressions.drop(1)
        }
    }

    context("a message") {
        test("describes its headers as an object") {
            val reading = readingOf(
                """
                    |components:
                    |  messages:
                    |    Bad:
                    |      headers:
                    |        type: string
                    |    Good:
                    |      headers:
                    |        type: object
                    |    Untyped:
                    |      headers:
                    |        properties: {}
                    |    Avro:
                    |      headers:
                    |        schemaFormat: application/vnd.apache.avro;version=1.9.0
                    |        schema:
                    |          type: string
                    |  messageTraits:
                    |    Bad:
                    |      headers:
                    |        ${'$'}ref: '#/components/schemas/Text'
                    |  schemas:
                    |    Text:
                    |      type: string
                """.trimMargin(),
            )

            reading.codes() shouldBe List(2) { DiagnosticCodes.MESSAGE_HEADERS_TYPE }
        }

        test("names a media type as its content type, like the default of the document") {
            val reading = readingOf(
                """
                    |defaultContentType: json
                    |components:
                    |  messages:
                    |    Good:
                    |      contentType: application/json; charset=utf-8
                    |    Quoted:
                    |      contentType: 'text/plain; charset="utf-8"'
                    |    Bad:
                    |      contentType: application/json;charset
                    |  messageTraits:
                    |    Bad:
                    |      contentType: application/json/x
                """.trimMargin(),
            )

            reading.codes() shouldBe List(FINDINGS) { DiagnosticCodes.MESSAGE_CONTENT_TYPE }
        }

        test("has examples with headers, a payload or both") {
            val reading = readingOf(
                """
                    |components:
                    |  messages:
                    |    Event:
                    |      examples:
                    |        - name: empty
                    |        - payload: null
                    |        - headers:
                    |            id: 1
                """.trimMargin(),
            )

            reading.codes() shouldBe listOf(DiagnosticCodes.MESSAGE_EXAMPLE_EMPTY)
        }

        test("of AsyncAPI 2 with a message id is reported") {
            readingOf(
                """
                    |components:
                    |  messages:
                    |    Event:
                    |      messageId: event
                """.trimMargin(),
            ).codes() shouldBe
                listOf(DiagnosticCodes.FIELD_VERSION)
        }

        test("without anything is valid") {
            readingOf("components:\n  messages:\n    Event: {}").diagnostics.shouldBeEmpty()
        }
    }
})
