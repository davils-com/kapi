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
import io.kotest.matchers.string.shouldContain

class ChannelRulesTest : FunSpec({
    context("the address of a channel") {
        test("names exactly the parameters the channel declares") {
            val reading = readingOf(
                """
                    |channels:
                    |  room:
                    |    address: rooms.{roomId}.{userId}
                    |    parameters:
                    |      roomId: {}
                    |      unused: {}
                """.trimMargin(),
            )

            reading.codes() shouldBe listOf(
                DiagnosticCodes.CHANNEL_PARAMETER_UNDECLARED,
                DiagnosticCodes.CHANNEL_PARAMETER_UNUSED,
            )
            reading.diagnostics.first().message shouldContain "userId"
        }

        test("is a valid template") {
            listOf("rooms.{roomId", "rooms.{}", "rooms.{a{b}}", "rooms}", "a}{b").forEach { address ->
                val reading = readingOf("channels:\n  room:\n    address: '$address'")

                reading.codes() shouldBe listOf(DiagnosticCodes.CHANNEL_ADDRESS_TEMPLATE)
            }
        }

        test("may be null, and then names no parameters") {
            val reading =
                readingOf(
                    """
                        |channels:
                        |  room:
                        |    address: null
                        |    parameters:
                        |      id: {}
                    """.trimMargin(),
                )

            reading.codes() shouldBe listOf(DiagnosticCodes.CHANNEL_PARAMETER_UNUSED)
        }
    }

    context("the servers of a channel") {
        test("of the root channels are servers of the root servers") {
            val reading = readingOf(
                """
                    |servers:
                    |  production:
                    |    host: example.com
                    |    protocol: ws
                    |channels:
                    |  good:
                    |    servers:
                    |      - ${'$'}ref: '#/servers/production'
                    |  bad:
                    |    servers:
                    |      - ${'$'}ref: '#/components/servers/shared'
                    |components:
                    |  servers:
                    |    shared:
                    |      host: shared.example.com
                    |      protocol: ws
                    |  channels:
                    |    free:
                    |      servers:
                    |        - ${'$'}ref: '#/components/servers/shared'
                """.trimMargin(),
            )

            reading.codes() shouldBe listOf(DiagnosticCodes.CHANNEL_SERVER_PLACEMENT)
        }

        test("may be left out") {
            readingOf("channels:\n  room:\n    address: room").diagnostics.shouldBeEmpty()
        }
    }
})
