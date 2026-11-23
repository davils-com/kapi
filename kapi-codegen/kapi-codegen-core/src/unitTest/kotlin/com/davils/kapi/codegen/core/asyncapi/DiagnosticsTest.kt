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

package com.davils.kapi.codegen.core.asyncapi

import com.davils.kapi.codegen.core.Generation
import com.davils.kapi.codegen.core.asyncapi.fixture.generationOf
import com.davils.kapi.codegen.core.asyncapi.fixture.resource
import com.davils.kore.core.validation.violation.Severity
import com.davils.kore.core.validation.violation.ViolationCode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

private const val NAME: String = "t.yaml"

private const val REF: String = "\$ref"

private const val ROOM: String = "{$REF: '#/channels/room'}"

private const val SEND: String = "{send: {action: send, channel: $ROOM}}"

private const val TEXT: String = "a: {payload: {type: string}}"

private const val OBJECT: String = "{type: object, required: [type], properties: {type: {type: string, const: a}}}"

private const val SCHEMA_A: String = "a: {payload: {$REF: '#/components/schemas/A'}}"

private const val COMPONENT_LINE: Int = 6

private const val REPLY: String = "reply: {channel: $ROOM}"

private fun document(
    channel: String,
    operations: String = SEND,
    components: String = "{}",
    servers: String = "{local: {host: localhost, protocol: ws}}",
): String = listOf(
    "asyncapi: 3.0.0",
    "info: {title: T, version: '1'}",
    "defaultContentType: application/json",
    "servers: $servers",
    "channels: {room: $channel}",
    "operations: $operations",
    "components: $components",
).joinToString(separator = "\n")

private fun messages(vararg entries: String): String = "{address: /room, messages: {${entries.joinToString()}}}"

private fun bound(binding: String): String = "{address: /room, messages: {$TEXT}, bindings: {ws: $binding}}"

private fun operation(action: String, extra: String): String = "{op: {action: $action, channel: $ROOM, $extra}}"

private fun secured(scheme: String): String = "security: [{$REF: '#/components/securitySchemes/$scheme'}]"

private fun only(message: String): String = "messages: [{$REF: '#/channels/room/messages/$message'}]"

private suspend fun codesOf(text: String): List<Pair<ViolationCode, Severity>> {
    val generation = generationOf(NAME, text)
    return generation.diagnostics.map { diagnostic -> diagnostic.code to diagnostic.severity }
}

private val refused: Map<String, Pair<String, ViolationCode>> = mapOf(
    "a payload in Avro" to (
        document(messages("a: {payload: {schemaFormat: 'application/vnd.apache.avro;version=1.9.0', schema: {}}}")) to
            GenerationCodes.SCHEMA_FORMAT
        ),
    "a reply to a dynamic address" to (
        document(
            channel = messages(TEXT),
            operations = operation(action = "receive", extra = "reply: {address: {location: '\$message.payload#/to'}}"),
        ) to GenerationCodes.REPLY_CHANNEL
        ),
    "two messages no property tells apart" to (
        document(messages(TEXT, "b: {payload: {type: integer}}")) to GenerationCodes.MESSAGE_INDISTINCT
        ),
    "a correlation id in the headers" to (
        document(messages("a: {payload: {type: string}, correlationId: {location: '\$message.header#/id'}}")) to
            GenerationCodes.CORRELATION_UNSUPPORTED
        ),
    "a message that is no JSON" to (
        document(messages("a: {contentType: application/xml, payload: {type: string}}")) to
            GenerationCodes.CONTENT_TYPE_UNSUPPORTED
        ),
    "a handshake with POST" to (document(bound("{method: POST}")) to GenerationCodes.BINDING_METHOD),
    "a scheme a handshake cannot carry" to (
        document(
            channel = messages(TEXT),
            operations = operation(action = "send", extra = secured("up")),
            components = "{securitySchemes: {up: {type: userPassword}}}",
        ) to GenerationCodes.SECURITY_UNSUPPORTED
        ),
    "operations that demand different security" to (
        document(
            channel = messages(TEXT, "b: {payload: {type: string}}"),
            operations = "{send: {action: send, channel: $ROOM, ${only("a")}, ${secured("key")}}, " +
                "receive: {action: receive, channel: $ROOM, ${only("b")}}}",
            components = "{securitySchemes: {key: {type: httpApiKey, name: k, in: query}}}",
        ) to GenerationCodes.SECURITY_MIXED
        ),
    "a payload that is a list of inline objects" to (
        document(messages("a: {payload: {type: array, items: $OBJECT}}")) to GenerationCodes.PAYLOAD_UNSUPPORTED
        ),
    "items as a list" to (
        document(messages("a: {payload: {type: array, items: [{type: string}]}}")) to
            GenerationCodes.SCHEMA_UNSUPPORTED
        ),
    "a query parameter that is an object" to (
        document(bound("{query: {type: object, properties: {q: {type: object}}}}")) to
            GenerationCodes.HANDSHAKE_UNSUPPORTED
        ),
    "an address with a query" to (
        document("{address: '/room?x=1', messages: {$TEXT}}") to GenerationCodes.CHANNEL_ADDRESS
        ),
    "two messages with one Kotlin name" to (
        document(
            messages(
                "say-it: {payload: {type: object, required: [t], properties: {t: {const: a}}}}",
                "say_it: {payload: {type: object, required: [t], properties: {t: {const: b}}}}",
            ),
        ) to GenerationCodes.NAME_COLLISION
        ),
)

private val warned: Map<String, Pair<String, ViolationCode>> = mapOf(
    "a channel served over Kafka" to (
        document(messages(TEXT), servers = "{kafka: {host: k, protocol: kafka}}") to
            GenerationCodes.PROTOCOL_UNSUPPORTED
        ),
    "a message with headers" to (
        document(messages("a: {headers: {type: object}, payload: {type: string}}")) to
            GenerationCodes.MESSAGE_HEADERS
        ),
    "a reply without correlation ids" to (
        document(channel = messages(TEXT), operations = operation(action = "receive", extra = REPLY)) to
            GenerationCodes.REPLY_UNPAIRED
        ),
    "a request the server sends" to (
        document(channel = messages(TEXT), operations = operation(action = "send", extra = REPLY)) to
            GenerationCodes.REPLY_UNPAIRED
        ),
    "a channel no operation names" to (document(messages(TEXT), operations = "{}") to GenerationCodes.CHANNEL_SKIPPED),
    "keywords beside a reference" to (
        document(
            channel = messages(SCHEMA_A),
            components = "{schemas: {A: {type: object, properties: {b: {$REF: '#/components/schemas/B', " +
                "type: string}}}, B: {type: string}}}",
        ) to GenerationCodes.SCHEMA_REFERENCE_SIBLING
        ),
)

class DiagnosticsTest : FunSpec({
    context("a document the generator cannot express faithfully") {
        refused.forEach { (subject, case) ->
            val (text, code) = case
            test("is refused: $subject") {
                generationOf(NAME, text).shouldBeInstanceOf<Generation.Refused>()
                codesOf(text) shouldContain (code to Severity.Error)
            }
        }
    }

    context("a document the generator expresses with a warning") {
        warned.forEach { (subject, case) ->
            val (text, code) = case
            test("is generated with a warning: $subject") {
                generationOf(NAME, text).shouldBeInstanceOf<Generation.Generated>()
                codesOf(text) shouldContain (code to Severity.Warning)
            }
        }
    }

    test("a schema the model generator refuses is located in the AsyncAPI document, its line counted from zero") {
        val schemas = "{schemas: {A: {type: object, properties: {b: {type: string, const: {x: 1}}}}}}"
        val text = document(messages(SCHEMA_A), components = schemas)
        val generation = generationOf(NAME, text).shouldBeInstanceOf<Generation.Refused>()
        val located = generation.diagnostics.first { diagnostic -> diagnostic.isError }.location

        located?.run { pointer.text } shouldBe "/components/schemas/A/properties/b"
        located?.run { position?.line } shouldBe COMPONENT_LINE
    }

    test("the chat names the Kafka channel it skips") {
        codesOf(resource("chat.yaml")) shouldContain (GenerationCodes.PROTOCOL_UNSUPPORTED to Severity.Warning)
    }
})
