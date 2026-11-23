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

package com.davils.kapi.spec.asyncapi.read

import com.davils.kapi.spec.asyncapi.DiagnosticCodes
import com.davils.kapi.spec.asyncapi.fixture.codes
import com.davils.kapi.spec.asyncapi.fixture.parts
import com.davils.kapi.spec.asyncapi.fixture.readingOf
import com.davils.kapi.spec.asyncapi.model.binding.ChannelBindings
import com.davils.kapi.spec.core.model.Referable
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

class BindingReadingTest : FunSpec({
    context("the WebSocket binding") {
        test("of a channel names GET or POST as its method") {
            val reading =
                readingOf(
                    """
                        |components:
                        |  channelBindings:
                        |    ws:
                        |      ws:
                        |        method: PUT
                    """.trimMargin(),
                )
            val bindings = reading.parts().channelBindings.getValue("ws")
            val read = bindings.shouldBeInstanceOf<Referable.Inline<ChannelBindings>>().value

            reading.codes() shouldBe listOf(DiagnosticCodes.BINDING_WEBSOCKET_METHOD)
            read.webSocket.shouldNotBeNull().method shouldBe null
        }

        test("of a server, an operation and a message has no fields") {
            val reading = readingOf(
                """
                    |components:
                    |  serverBindings:
                    |    ws:
                    |      ws:
                    |        bindingVersion: 0.1.0
                    |  operationBindings:
                    |    ws:
                    |      ws:
                    |        method: GET
                    |  messageBindings:
                    |    ws:
                    |      ws: {}
                """.trimMargin(),
            )

            reading.codes() shouldBe listOf(DiagnosticCodes.FIELD_UNKNOWN, DiagnosticCodes.FIELD_UNKNOWN)
        }
    }

    context("the bindings of other protocols") {
        test("are kept as they were written, by the name of the protocol") {
            val reading = readingOf(
                """
                    |components:
                    |  channelBindings:
                    |    all:
                    |      amqp:
                    |        is: queue
                    |        queue:
                    |          name: rooms
                    |      mqtt:
                    |        qos: 1
                    |      x-vendor: true
                """.trimMargin(),
            )
            val bindings = reading.parts().channelBindings.getValue("all")
            val read = bindings.shouldBeInstanceOf<Referable.Inline<ChannelBindings>>().value

            reading.diagnostics shouldBe emptyList()
            read.protocols.keys shouldContainExactly setOf("amqp", "mqtt")
            read.webSocket shouldBe null
        }

        test("are reported when the protocol is unknown") {
            val reading =
                readingOf(
                    """
                        |components:
                        |  messageBindings:
                        |    all:
                        |      kafak: {}
                    """.trimMargin(),
                )

            reading.codes() shouldBe listOf(DiagnosticCodes.FIELD_UNKNOWN)
            reading.diagnostics.single().message shouldContain "did you mean kafka?"
        }
    }
})
