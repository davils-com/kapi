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

private val channels: String =
    """
        |channels:
        |  chat:
        |    address: chat
        |    messages:
        |      said:
        |        ${'$'}ref: '#/components/messages/Said'
        |  replies:
        |    messages:
        |      ack:
        |        ${'$'}ref: '#/components/messages/Ack'
        |  addressed:
        |    address: replies
        |    messages:
        |      ack:
        |        ${'$'}ref: '#/components/messages/Ack'
        |  aliased:
        |    ${'$'}ref: '#/components/channels/shared'
        |components:
        |  channels:
        |    shared:
        |      address: shared
        |      messages:
        |        said:
        |          ${'$'}ref: '#/components/messages/Said'
        |  messages:
        |    Said:
        |      name: said
        |    Ack:
        |      name: ack
    """.trimMargin()

private suspend fun readingWith(operations: String) = readingOf(operations.trimIndent() + "\n" + channels.trimIndent())

class OperationRulesTest : FunSpec({
    context("an operation") {
        test("of the root operations names a channel of the root channels") {
            val reading = readingWith(
                """
                    |operations:
                    |  send:
                    |    action: send
                    |    channel:
                    |      ${'$'}ref: '#/components/channels/shared'
                """.trimMargin(),
            )

            reading.codes() shouldBe listOf(DiagnosticCodes.OPERATION_CHANNEL_PLACEMENT)
        }

        test("of the components may name any channel") {
            val reading = readingOf(
                """
                    |components:
                    |  operations:
                    |    send:
                    |      action: send
                    |      channel:
                    |        ${'$'}ref: '#/components/channels/shared'
                    |      messages:
                    |        - ${'$'}ref: '#/components/channels/shared/messages/said'
                    |  channels:
                    |    shared:
                    |      messages:
                    |        said: {}
                """.trimMargin(),
            )

            reading.diagnostics.shouldBeEmpty()
        }

        test("names only messages of its channel") {
            val reading = readingWith(
                """
                    |operations:
                    |  send:
                    |    action: send
                    |    channel:
                    |      ${'$'}ref: '#/channels/chat'
                    |    messages:
                    |      - ${'$'}ref: '#/channels/chat/messages/said'
                    |      - ${'$'}ref: '#/channels/replies/messages/ack'
                    |      - ${'$'}ref: '#/components/messages/Ack'
                """.trimMargin(),
            )

            reading.codes() shouldBe List(2) { DiagnosticCodes.OPERATION_MESSAGE_NOT_IN_CHANNEL }
        }

        test("may name the messages of a channel it reaches through a reference") {
            val reading = readingWith(
                """
                    |operations:
                    |  send:
                    |    action: send
                    |    channel:
                    |      ${'$'}ref: '#/channels/aliased'
                    |    messages:
                    |      - ${'$'}ref: '#/components/channels/shared/messages/said'
                """.trimMargin(),
            )

            reading.diagnostics.shouldBeEmpty()
        }

        test("sends or receives") {
            val reading = readingWith(
                """
                    |operations:
                    |  send:
                    |    action: publish
                    |    channel:
                    |      ${'$'}ref: '#/channels/chat'
                """.trimMargin(),
            )

            reading.codes() shouldBe listOf(DiagnosticCodes.OPERATION_ACTION)
        }

        test("needs its action and its channel") {
            val reading = readingWith("operations:\n  send:\n    title: Send")

            reading.codes() shouldBe List(2) { DiagnosticCodes.FIELD_MISSING }
        }
    }

    context("a reply") {
        test("names only messages of its channel") {
            val reading = readingWith(
                """
                    |operations:
                    |  ask:
                    |    action: send
                    |    channel:
                    |      ${'$'}ref: '#/channels/chat'
                    |    reply:
                    |      channel:
                    |        ${'$'}ref: '#/channels/replies'
                    |      messages:
                    |        - ${'$'}ref: '#/channels/replies/messages/ack'
                    |        - ${'$'}ref: '#/channels/chat/messages/said'
                """.trimMargin(),
            )

            reading.codes() shouldBe listOf(DiagnosticCodes.REPLY_MESSAGE_NOT_IN_CHANNEL)
        }

        test("with an address names a channel without one") {
            val reading = readingWith(
                """
                    |operations:
                    |  ask:
                    |    action: send
                    |    channel:
                    |      ${'$'}ref: '#/channels/chat'
                    |    reply:
                    |      address:
                    |        location: ${'$'}message.header#/replyTo
                    |      channel:
                    |        ${'$'}ref: '#/channels/addressed'
                    |  other:
                    |    action: send
                    |    channel:
                    |      ${'$'}ref: '#/channels/chat'
                    |    reply:
                    |      address:
                    |        location: ${'$'}message.header#/replyTo
                    |      channel:
                    |        ${'$'}ref: '#/channels/replies'
                    |  third:
                    |    action: send
                    |    channel:
                    |      ${'$'}ref: '#/channels/chat'
                    |    reply:
                    |      address:
                    |        location: ${'$'}message.header#/replyTo
                """.trimMargin(),
            )

            reading.codes() shouldBe listOf(DiagnosticCodes.REPLY_ADDRESS_CONFLICT)
        }
    }

    context("the keys of operations") {
        test("are unique, because a document cannot hold one key twice") {
            val reading = readingWith(
                """
                    |operations:
                    |  send:
                    |    action: send
                    |    channel:
                    |      ${'$'}ref: '#/channels/chat'
                    |  send:
                    |    action: receive
                    |    channel:
                    |      ${'$'}ref: '#/channels/chat'
                """.trimMargin(),
            )

            reading.codes().map { code -> code.value } shouldBe listOf("kaml.mapping.duplicate-key")
        }
    }
})
