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

package com.davils.kapi.spec.asyncapi.read.reference

import com.davils.kapi.spec.asyncapi.AsyncApi
import com.davils.kapi.spec.asyncapi.DiagnosticCodes
import com.davils.kapi.spec.asyncapi.fixture.apiUri
import com.davils.kapi.spec.asyncapi.fixture.codes
import com.davils.kapi.spec.asyncapi.fixture.document
import com.davils.kapi.spec.asyncapi.fixture.head
import com.davils.kapi.spec.asyncapi.fixture.readingOf
import com.davils.kapi.spec.asyncapi.fixture.readingWith
import com.davils.kapi.spec.asyncapi.fixture.yamlOf
import com.davils.kapi.spec.asyncapi.model.message.Message
import com.davils.kapi.spec.asyncapi.model.schema.Schema
import com.davils.kapi.spec.asyncapi.resolve
import com.davils.kapi.spec.asyncapi.testing.MemoryResolver
import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.core.source.Refusal
import com.davils.kapi.spec.core.source.Resolution
import com.davils.kapi.spec.core.source.Source
import com.davils.kore.core.validation.violation.Severity
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

private val models: DocumentUri = DocumentUri("memory:/api/models.yaml")

private val modelsText: String = yamlOf(
    "Event:",
    "  payload:",
    "    ${'$'}ref: '#/Payload'",
    "Payload:",
    "  type: object",
)

private val TO_MODELS: String =
    """
        |components:
        |  messages:
        |    Event:
        |      ${'$'}ref: 'models.yaml#/Event'
    """.trimMargin()

private fun messagesTo(pointer: String): String =
    """
        |components:
        |  messages:
        |    Event:
        |      ${'$'}ref: '$pointer'
    """.trimMargin()

private const val FINDINGS: Int = 3

class ReferenceReadingTest : FunSpec({
    context("a reference into another document") {
        test("loads that document and resolves what it points at, and what that refers to in turn") {
            val resolver = MemoryResolver(mapOf(models to modelsText))
            val reading = readingOf(TO_MODELS) { this.resolver = resolver }
            val document = reading.document()
            val event = document.components
                .shouldNotBeNull()
                .messages
                .getValue("Event")
            val message = document.resolve(event).shouldNotBeNull()
            val payload = document.resolve(message.payload.shouldNotBeNull()).shouldNotBeNull()

            reading.diagnostics.shouldBeEmpty()
            reading.sources.map { source -> source.uri } shouldBe listOf(apiUri, models)
            payload.shouldBeInstanceOf<Schema>()
            resolver.requested shouldBe listOf(models)
        }

        test("is an error at the reference when that document does not exist") {
            val reading = readingWith(emptyMap(), TO_MODELS)
            val location = reading.diagnostics
                .single()
                .location
                .shouldNotBeNull()

            reading.codes() shouldBe listOf(DiagnosticCodes.LOAD_MISSING)
            location.pointer.toString() shouldBe "/components/messages/Event/\$ref"
        }

        test("is refused when the resolver refuses it, fails or the document is no UTF-8") {
            val answers = mapOf(
                models to Resolution.Refused(Refusal.Confinement),
            )
            val resolver = MemoryResolver(emptyMap(), answers)

            readingOf(TO_MODELS) { this.resolver = resolver }.codes() shouldBe listOf(DiagnosticCodes.REFERENCE_OUTSIDE)
        }

        test("is refused when it leaves the folder of the root") {
            val reading = readingWith(mapOf(models to modelsText), messagesTo("../models.yaml#/Event"))

            reading.codes() shouldBe listOf(DiagnosticCodes.REFERENCE_OUTSIDE)
        }

        test("is refused when it names a host or the root is remote") {
            readingOf(messagesTo("//example.com/models.yaml#/Event")).codes() shouldBe
                listOf(DiagnosticCodes.REFERENCE_OUTSIDE)
            val text = yamlOf(head + TO_MODELS.trimIndent().lines())
            val remote = AsyncApi.reader().read(Source.Text(text, DocumentUri("https://example.com/api.yaml")))

            remote.codes() shouldBe listOf(DiagnosticCodes.LOAD_REMOTE)
        }

        test("is refused when it is remote") {
            readingOf(messagesTo("https://example.com/models.yaml#/Event")).codes() shouldBe
                listOf(DiagnosticCodes.LOAD_REMOTE)
        }
    }

    context("a reference that leads nowhere") {
        test("is an error when nothing is at the pointer") {
            val reading = readingOf(messagesTo("#/components/messages/Missing"))

            reading.codes() shouldBe listOf(DiagnosticCodes.REFERENCE_UNRESOLVED)
            reading.diagnostics.single().message shouldContain "points at nothing"
        }

        test("is an error when it is no string") {
            readingOf(
                """
                    |components:
                    |  messages:
                    |    Event:
                    |      ${'$'}ref: 1
                """.trimMargin(),
            ).codes() shouldBe
                listOf(DiagnosticCodes.REFERENCE_SYNTAX)
        }

        test("is an error when it is no uri") {
            readingOf(messagesTo("a b#/x")).codes() shouldBe listOf(DiagnosticCodes.REFERENCE_SYNTAX)
        }

        test("is an error when its fragment is no JSON pointer") {
            readingOf(messagesTo("#event")).codes() shouldBe listOf(DiagnosticCodes.REFERENCE_POINTER)
            readingOf(messagesTo("#/a%zz")).codes() shouldBe listOf(DiagnosticCodes.REFERENCE_SYNTAX)
            readingOf(messagesTo("#/a%c3")).codes() shouldBe listOf(DiagnosticCodes.REFERENCE_POINTER)
        }

        test("decodes the escapes of its fragment") {
            val reading = readingOf(
                """
                    |components:
                    |  messages:
                    |    Event:
                    |      ${'$'}ref: '#/components/messages/Plain%20Event'
                    |    Plain Event:
                    |      name: plain
                """.trimMargin(),
            )

            reading.codes() shouldBe listOf(DiagnosticCodes.KEY_INVALID)
        }

        test("is an error when it is relative and the document has no place of its own") {
            val text = yamlOf(head + TO_MODELS.trimIndent().lines())
            val reading = AsyncApi.reader().read(Source.Text(text, DocumentUri("urn:chat")))

            reading.codes() shouldBe listOf(DiagnosticCodes.REFERENCE_NO_BASE)
        }
    }

    context("a chain of references") {
        test("is followed to its end") {
            val document = readingOf(
                """
                    |components:
                    |  messages:
                    |    First:
                    |      ${'$'}ref: '#/components/messages/Second'
                    |    Second:
                    |      ${'$'}ref: '#/components/messages/Third'
                    |    Third:
                    |      name: third
                """.trimMargin(),
            ).document()
            val first = document.components
                .shouldNotBeNull()
                .messages
                .getValue("First")

            document.resolve(first).shouldNotBeNull().name shouldBe "third"
        }

        test("is an error when it leads back to itself") {
            val reading = readingOf(
                """
                    |components:
                    |  messages:
                    |    First:
                    |      ${'$'}ref: '#/components/messages/Second'
                    |    Second:
                    |      ${'$'}ref: '#/components/messages/First'
                """.trimMargin(),
            )

            reading.codes().toSet() shouldBe setOf(DiagnosticCodes.REFERENCE_CYCLE)
        }

        test("is an error when it is longer than a reading follows") {
            val reading = readingOf(
                """
                    |components:
                    |  messages:
                    |    First:
                    |      ${'$'}ref: '#/components/messages/Second'
                    |    Second:
                    |      ${'$'}ref: '#/components/messages/Third'
                    |    Third:
                    |      name: third
                """.trimMargin(),
            ) { limits { maximumReferenceHops = 1 } }

            reading.codes().toSet() shouldBe setOf(DiagnosticCodes.REFERENCE_CHAIN_TOO_LONG)
        }
    }

    context("a reference") {
        test("warns about the fields beside it") {
            val reading = readingOf(
                """
                    |components:
                    |  messages:
                    |    Event:
                    |      ${'$'}ref: '#/components/messages/Plain'
                    |      name: ignored
                    |    Plain:
                    |      name: plain
                """.trimMargin(),
            )

            reading.codes() shouldBe listOf(DiagnosticCodes.REFERENCE_SIBLING)
            reading.diagnostics.single().severity shouldBe Severity.Warning
        }

        test("is an error when one place is read as two different objects") {
            val reading = readingOf(
                """
                    |components:
                    |  messages:
                    |    Event:
                    |      ${'$'}ref: '#/components/schemas/Shared'
                    |  schemas:
                    |    Plain:
                    |      ${'$'}ref: '#/components/schemas/Shared'
                    |    Shared:
                    |      type: object
                """.trimMargin(),
            )

            reading.codes() shouldBe listOf(DiagnosticCodes.REFERENCE_KIND)
        }

        test("is required where AsyncAPI allows only a reference") {
            val reading = readingOf(
                """
                    |channels:
                    |  chat:
                    |    address: chat
                    |    servers:
                    |      - host: inline.example.com
                    |        protocol: ws
                    |operations:
                    |  send:
                    |    action: send
                    |    channel:
                    |      address: inline
                    |    messages:
                    |      - name: inline
                """.trimMargin(),
            )

            reading.codes() shouldBe List(FINDINGS) { DiagnosticCodes.REFERENCE_REQUIRED }
        }

        test("of an operation leads to the channel and the messages it names") {
            val document = readingOf(
                """
                    |channels:
                    |  chat:
                    |    address: chat
                    |    messages:
                    |      said:
                    |        name: said
                    |operations:
                    |  send:
                    |    action: send
                    |    channel:
                    |      ${'$'}ref: '#/channels/chat'
                    |    messages:
                    |      - ${'$'}ref: '#/channels/chat/messages/said'
                """.trimMargin(),
            ).document()
            val operation = document.resolve(document.operations.getValue("send")).shouldNotBeNull()

            document.resolve(operation.messages.single()).shouldBeInstanceOf<Message>().name shouldBe "said"
            document.resolve(operation.channel.shouldNotBeNull()).shouldNotBeNull()
        }
    }
})
