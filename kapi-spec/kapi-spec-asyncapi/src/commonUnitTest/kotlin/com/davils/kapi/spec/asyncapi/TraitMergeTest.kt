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
import com.davils.kapi.spec.asyncapi.fixture.parts
import com.davils.kapi.spec.asyncapi.fixture.readingOf
import com.davils.kapi.spec.asyncapi.model.message.Message
import com.davils.kapi.spec.asyncapi.model.operation.Operation
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

private const val TRAIT: String = "From the trait"

private const val OWN: String = "Its own"

private const val FIELDS: Int = 3

private val document: String =
    """
        |channels:
        |  chat:
        |    address: chat
        |operations:
        |  bare:
        |    action: send
        |    channel:
        |      ${'$'}ref: '#/channels/chat'
        |    traits:
        |      - ${'$'}ref: '#/components/operationTraits/full'
        |  rich:
        |    action: send
        |    channel:
        |      ${'$'}ref: '#/channels/chat'
        |    title: Its own
        |    summary: Its own
        |    description: Its own
        |    security:
        |      - type: plain
        |    tags:
        |      - name: own
        |    externalDocs:
        |      url: https://example.com/own
        |    bindings:
        |      ws: {}
        |    traits:
        |      - ${'$'}ref: '#/components/operationTraits/full'
        |  empty:
        |    action: receive
        |    channel:
        |      ${'$'}ref: '#/channels/chat'
        |components:
        |  operationTraits:
        |    full:
        |      title: From the trait
        |      summary: From the trait
        |      description: From the trait
        |      security:
        |        - type: gssapi
        |      tags:
        |        - name: trait
        |      externalDocs:
        |        url: https://example.com/trait
        |      bindings:
        |        kafka: {}
        |  messageTraits:
        |    full:
        |      headers:
        |        type: object
        |      correlationId:
        |        location: ${'$'}message.header#/trait
        |      contentType: text/plain
        |      name: trait
        |      title: From the trait
        |      summary: From the trait
        |      description: From the trait
        |      tags:
        |        - name: trait
        |      externalDocs:
        |        url: https://example.com/trait
        |      bindings:
        |        kafka: {}
        |      examples:
        |        - payload: trait
        |  messages:
        |    Empty: {}
        |    Bare:
        |      traits:
        |        - ${'$'}ref: '#/components/messageTraits/full'
        |    Rich:
        |      headers:
        |        type: object
        |        properties: {}
        |      correlationId:
        |        location: ${'$'}message.header#/own
        |      contentType: application/json
        |      name: own
        |      title: Its own
        |      summary: Its own
        |      description: Its own
        |      tags:
        |        - name: own
        |      externalDocs:
        |        url: https://example.com/own
        |      bindings:
        |        ws: {}
        |      examples:
        |        - payload: own
        |      traits:
        |        - ${'$'}ref: '#/components/messageTraits/full'
    """.trimMargin()

private fun Document.operation(name: String): Operation = effective(
    resolve(operations.getValue(name)).shouldNotBeNull(),
)

private fun Document.message(name: String): Message = effective(
    resolve(parts().messages.getValue(name)).shouldNotBeNull(),
)

class TraitMergeTest : FunSpec({
    context("an operation") {
        test("without fields of its own takes every field of its trait") {
            val read = readingOf(document).document()
            val bare = read.operation("bare")

            listOf(bare.title, bare.summary, bare.description) shouldBe List(FIELDS) { TRAIT }
            bare.security.size shouldBe 1
            bare.tags.size shouldBe 1
            bare.externalDocs.shouldNotBeNull()
            bare.bindings.shouldNotBeNull()
        }

        test("with fields of its own keeps them") {
            val read = readingOf(document).document()
            val rich = read.operation("rich")
            val declared = read.resolve(read.operations.getValue("rich")).shouldNotBeNull()

            listOf(rich.title, rich.summary, rich.description) shouldBe List(FIELDS) { OWN }
            rich.security shouldBe declared.security
            rich.tags shouldBe declared.tags
            rich.externalDocs shouldBe declared.externalDocs
            rich.bindings shouldBe declared.bindings
        }
    }

    context("an object without traits") {
        test("stays empty where it declares nothing") {
            val read = readingOf(document).document()
            val operation = read.operation("empty")
            val message = read.message("Empty")

            listOf(operation.title, operation.summary, operation.description) shouldBe List(FIELDS) { null }
            listOf(message.name, message.contentType, message.title) shouldBe List(FIELDS) { null }
            message.headers shouldBe null
        }
    }

    context("a message") {
        test("without fields of its own takes every field of its trait") {
            val read = readingOf(document).document()
            val bare = read.message("Bare")

            listOf(bare.title, bare.summary, bare.description) shouldBe List(FIELDS) { TRAIT }
            bare.name shouldBe "trait"
            bare.contentType shouldBe "text/plain"
            bare.headers.shouldNotBeNull()
            bare.correlationId.shouldNotBeNull()
            bare.tags.size shouldBe 1
            bare.externalDocs.shouldNotBeNull()
            bare.bindings.shouldNotBeNull()
            bare.examples.size shouldBe 1
        }

        test("with fields of its own keeps them") {
            val read = readingOf(document).document()
            val rich = read.message("Rich")
            val declared = read.resolve(read.parts().messages.getValue("Rich")).shouldNotBeNull()

            listOf(rich.title, rich.summary, rich.description) shouldBe List(FIELDS) { OWN }
            rich.name shouldBe "own"
            rich.contentType shouldBe "application/json"
            rich.headers shouldBe declared.headers
            rich.correlationId shouldBe declared.correlationId
            rich.tags shouldBe declared.tags
            rich.externalDocs shouldBe declared.externalDocs
            rich.bindings shouldBe declared.bindings
            rich.examples shouldBe declared.examples
        }
    }
})
