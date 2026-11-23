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
import com.davils.kapi.spec.asyncapi.fixture.parts
import com.davils.kapi.spec.asyncapi.fixture.readable
import com.davils.kapi.spec.asyncapi.fixture.readingOf
import com.davils.kapi.spec.core.value.ExtensionName
import com.davils.kapi.spec.core.value.Value
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

private const val TRAITS: Int = 3

private val document: String =
    """
        |channels:
        |  chat:
        |    address: chat
        |    messages:
        |      said:
        |        ${'$'}ref: '#/components/messages/Said'
        |operations:
        |  send:
        |    action: send
        |    channel:
        |      ${'$'}ref: '#/channels/chat'
        |    title: Send
        |    x-level: operation
        |    traits:
        |      - ${'$'}ref: '#/components/operationTraits/first'
        |      - summary: From the second trait
        |        tags:
        |          - name: second
        |        x-level: second
        |        x-second: true
        |      - ${'$'}ref: '#/components/operationTraits/missing'
        |  listen:
        |    action: receive
        |    channel:
        |      ${'$'}ref: '#/channels/chat'
        |    title: Listen
        |components:
        |  operationTraits:
        |    first:
        |      title: From the first trait
        |      summary: From the first trait
        |      description: From the first trait
        |      x-first: true
        |  messages:
        |    Said:
        |      name: said
        |      payload:
        |        type: string
        |      traits:
        |        - contentType: text/plain
        |          name: trait
        |          examples:
        |            - payload: hello
        |        - ${'$'}ref: '#/components/messageTraits/later'
        |  messageTraits:
        |    later:
        |      contentType: application/json
        |      headers:
        |        type: object
    """.trimMargin()

class TraitsTest : FunSpec({
    context("the traits of an operation") {
        test("are merged in their order, and what the operation declares wins") {
            val reading = readingOf(document)
            val read = reading.readable()
            val operation = read.resolve(read.operations.getValue("send")).shouldNotBeNull()
            val effective = read.effective(operation)

            reading.codes() shouldBe listOf(DiagnosticCodes.REFERENCE_UNRESOLVED)
            operation.traits.size shouldBe TRAITS
            effective.title shouldBe "Send"
            effective.summary shouldBe "From the second trait"
            effective.description shouldBe "From the first trait"
            effective.tags.size shouldBe 1
            effective.traits.shouldBeEmpty()
            effective.channel shouldBe operation.channel
            effective.location shouldBe operation.location
            effective.extensions[ExtensionName("x-level")] shouldBe Value.Text("operation")
            effective.extensions[ExtensionName("x-first")] shouldBe Value.Flag(true)
            effective.extensions[ExtensionName("x-second")] shouldBe Value.Flag(true)
        }

        test("leave an operation without traits as it is") {
            val read = readingOf(document).readable()
            val listen = read.resolve(read.operations.getValue("listen")).shouldNotBeNull()

            read.effective(listen) shouldBe listen
        }
    }

    context("the traits of a message") {
        test("are merged in their order, a later trait over an earlier one") {
            val read = readingOf(document).readable()
            val said = read.parts().messages.getValue("Said")
            val message = read.resolve(said).shouldNotBeNull()
            val effective = read.effective(message)

            effective.name shouldBe "said"
            effective.contentType shouldBe "application/json"
            effective.headers.shouldNotBeNull()
            effective.payload shouldBe message.payload
            effective.examples.size shouldBe 1
            effective.traits.shouldBeEmpty()
        }
    }
})
