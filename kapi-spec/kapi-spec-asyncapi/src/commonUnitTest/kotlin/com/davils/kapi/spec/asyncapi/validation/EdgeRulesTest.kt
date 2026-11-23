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
import com.davils.kapi.spec.asyncapi.fixture.readingWith
import com.davils.kapi.spec.asyncapi.fixture.yamlOf
import com.davils.kapi.spec.core.diagnostic.DocumentUri
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private val events: DocumentUri = DocumentUri("memory:/api/events.yaml")

class EdgeRulesTest : FunSpec({
    context("a key") {
        test("is checked for a reference as well") {
            val reading = readingOf(
                """
                    |components:
                    |  messages:
                    |    'bad key':
                    |      ${'$'}ref: '#/components/messages/Good'
                    |    Good: {}
                """.trimMargin(),
            )

            reading.codes() shouldBe listOf(DiagnosticCodes.KEY_INVALID)
        }
    }

    context("a tag") {
        test("that cannot be resolved counts for no duplicate") {
            val reading = readingOf(
                """
                    |components:
                    |  messages:
                    |    Event:
                    |      tags:
                    |        - ${'$'}ref: '#/components/tags/missing'
                    |        - name: event
                """.trimMargin(),
            )

            reading.codes() shouldBe listOf(DiagnosticCodes.REFERENCE_UNRESOLVED)
        }
    }

    context("an operation") {
        test("whose channel cannot be resolved is checked no further") {
            val reading = readingOf(
                """
                    |operations:
                    |  send:
                    |    action: send
                    |    channel:
                    |      ${'$'}ref: '#/channels/missing'
                    |    messages:
                    |      - ${'$'}ref: '#/channels/missing/messages/said'
                """.trimMargin(),
            )

            reading.codes().toSet() shouldBe setOf(
                DiagnosticCodes.REFERENCE_UNRESOLVED,
                DiagnosticCodes.OPERATION_CHANNEL_PLACEMENT,
            )
        }

        test("names a message that exists in its channel") {
            val reading = readingOf(
                """
                    |channels:
                    |  chat:
                    |    messages:
                    |      said: {}
                    |operations:
                    |  send:
                    |    action: send
                    |    channel:
                    |      ${'$'}ref: '#/channels/chat'
                    |    messages:
                    |      - ${'$'}ref: '#/channels/chat/messages/missing'
                """.trimMargin(),
            )

            reading.codes() shouldBe listOf(
                DiagnosticCodes.OPERATION_MESSAGE_NOT_IN_CHANNEL,
                DiagnosticCodes.REFERENCE_UNRESOLVED,
            )
        }

        test("of the root names no channel of another document") {
            val documents = mapOf(events to yamlOf("channels:", "  chat:", "    messages: {}"))
            val reading = readingWith(
                documents,
                """
                    |channels:
                    |  chat: {}
                    |operations:
                    |  send:
                    |    action: send
                    |    channel:
                    |      ${'$'}ref: 'events.yaml#/channels/chat'
                """.trimMargin(),
            )

            reading.codes() shouldBe listOf(DiagnosticCodes.OPERATION_CHANNEL_PLACEMENT)
        }

        test("replies on a channel that cannot be resolved") {
            val reading = readingOf(
                """
                    |operations:
                    |  send:
                    |    action: send
                    |    channel:
                    |      ${'$'}ref: '#/channels/chat'
                    |    reply:
                    |      address:
                    |        location: ${'$'}message.header#/replyTo
                    |      channel:
                    |        ${'$'}ref: '#/channels/missing'
                    |channels:
                    |  chat: {}
                """.trimMargin(),
            )

            reading.codes() shouldBe listOf(DiagnosticCodes.REFERENCE_UNRESOLVED)
        }
    }

    context("the WebSocket binding of a channel") {
        test("takes a boolean schema and refuses a type that is no string") {
            val reading = readingOf(
                """
                    |components:
                    |  channelBindings:
                    |    ws:
                    |      ws:
                    |        query: true
                    |        headers:
                    |          type: [object]
                    |          properties: {}
                """.trimMargin(),
            )

            reading.codes() shouldBe listOf(DiagnosticCodes.BINDING_WEBSOCKET_SCHEMA)
        }
    }
})
