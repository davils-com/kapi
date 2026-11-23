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

class DocumentRulesTest : FunSpec({
    context("the id of a document") {
        test("is an absolute URI") {
            readingOf("id: 'urn:example:chat'").diagnostics.shouldBeEmpty()
            readingOf("id: 'https://example.com/chat'").diagnostics.shouldBeEmpty()
            readingOf("id: chat").codes() shouldBe listOf(DiagnosticCodes.DOCUMENT_ID)
            readingOf("id: 'a b'").codes() shouldBe listOf(DiagnosticCodes.DOCUMENT_ID)
        }
    }

    context("the keys of a document") {
        test("of components, channels, operations and messages hold letters, digits, dots, hyphens and underscores") {
            val reading = readingOf(
                """
                    |channels:
                    |  'chat room':
                    |    address: chat
                    |    messages:
                    |      'said/it': {}
                    |      said.it_-1: {}
                    |components:
                    |  schemas:
                    |    'Pet Store': {}
                    |    Pet.Store_v-1: {}
                """.trimMargin(),
            )

            reading.codes() shouldBe List(FINDINGS) { DiagnosticCodes.KEY_INVALID }
        }

        test("of servers and parameters hold letters, digits, hyphens and underscores") {
            val reading = readingOf(
                """
                    |servers:
                    |  prod.eu:
                    |    host: example.com
                    |    protocol: ws
                    |channels:
                    |  room:
                    |    address: '{room.id}'
                    |    parameters:
                    |      room.id: {}
                """.trimMargin(),
            )

            reading.codes() shouldBe List(2) { DiagnosticCodes.KEY_INVALID }
        }
    }

    context("the tags of an object") {
        test("name every tag once") {
            val reading = readingOf(
                """
                    |components:
                    |  tags:
                    |    event:
                    |      name: event
                    |  messages:
                    |    Event:
                    |      tags:
                    |        - name: event
                    |        - ${'$'}ref: '#/components/tags/event'
                    |    Other:
                    |      tags:
                    |        - name: event
                    |        - name: other
                """.trimMargin(),
            )

            reading.codes() shouldBe listOf(DiagnosticCodes.TAG_DUPLICATE)
        }
    }

    context("a security scheme of OAuth 2.0") {
        test("requires only scopes its flows offer") {
            val reading = readingOf(
                """
                    |components:
                    |  securitySchemes:
                    |    oauth:
                    |      type: oauth2
                    |      flows:
                    |        clientCredentials:
                    |          tokenUrl: https://example.com/token
                    |          availableScopes:
                    |            read: Read
                    |      scopes: [read, write]
                """.trimMargin(),
            )

            reading.codes() shouldBe listOf(DiagnosticCodes.SECURITY_SCOPES)
        }
    }

    context("the WebSocket binding of a channel") {
        test("describes its query and its headers as objects with properties") {
            val reading = readingOf(
                """
                    |components:
                    |  channelBindings:
                    |    good:
                    |      ws:
                    |        query:
                    |          type: object
                    |          properties: {}
                    |    bad:
                    |      ws:
                    |        query:
                    |          type: object
                    |        headers:
                    |          type: string
                    |          properties: {}
                """.trimMargin(),
            )

            reading.codes() shouldBe List(2) { DiagnosticCodes.BINDING_WEBSOCKET_SCHEMA }
        }
    }
})
