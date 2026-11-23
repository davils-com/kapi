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
import com.davils.kapi.codegen.core.asyncapi.fixture.Compiler
import com.davils.kapi.codegen.core.asyncapi.fixture.generationOf
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

private const val REF: String = "\$ref"

private const val NAME: String = "t.yaml"

private const val ROOM: String = "{$REF: '#/channels/room'}"

private fun message(name: String): String = "{$REF: '#/channels/room/messages/$name'}"

private fun tagged(tag: String): String =
    "{payload: {type: object, required: [type, id], properties: {type: {type: string, const: $tag}, " +
        "id: {type: string}}}, correlationId: {location: '\$message.payload#/id'}}"

private fun document(channel: String, operations: String, components: String = "{}"): String = listOf(
    "asyncapi: 3.0.0",
    "info: {title: T, version: '1'}",
    "defaultContentType: application/json",
    "servers: {local: {host: localhost, protocol: ws}}",
    "channels: {room: $channel}",
    "operations: $operations",
    "components: $components",
).joinToString(separator = "\n")

private const val SERVER_REQUEST: String = "server request of two messages"

private const val CLIENT_REQUEST: String = "client request of two messages"

private const val DISCRIMINATED: String = "declared discriminator"

private const val HANDSHAKE: String = "handshake from components"

private val shapes: Map<String, String> = mapOf(
    SERVER_REQUEST to document(
        channel = "{address: /room, messages: {c1: ${tagged("c1")}, c2: ${tagged("c2")}, ack: ${tagged("ack")}}}",
        operations = "{ask: {action: send, channel: $ROOM, messages: [${message("c1")}, ${message("c2")}], " +
            "reply: {messages: [${message("ack")}]}}}",
    ),
    CLIENT_REQUEST to document(
        channel = "{address: /room, messages: {q1: ${tagged("q1")}, q2: ${tagged("q2")}, r: ${tagged("r")}}}",
        operations = "{ask: {action: receive, channel: $ROOM, messages: [${message("q1")}, ${message("q2")}], " +
            "reply: {messages: [${message("r")}]}}}",
    ),
    DISCRIMINATED to document(
        channel = "{address: /room, messages: {x: {payload: {$REF: '#/components/schemas/X'}}, " +
            "y: {payload: {$REF: '#/components/schemas/Y'}}}}",
        operations = "{send: {action: send, channel: $ROOM}}",
        components = "{schemas: {" +
            "X: {type: object, discriminator: kind, required: [type, kind], properties: " +
            "{type: {type: string, enum: [t]}, kind: {type: string, enum: [x]}}}, " +
            "Y: {type: object, discriminator: kind, required: [type, kind], properties: " +
            "{type: {type: string, enum: [t]}, kind: {$REF: '#/components/schemas/KindY'}}}, " +
            "KindY: {type: string, const: y}}}",
    ),
    HANDSHAKE to document(
        channel = "{address: /room, messages: {a: {payload: {type: string}}}, bindings: {ws: {" +
            "query: {$REF: '#/components/schemas/Q'}, headers: {$REF: '#/components/schemas/H'}}}}",
        operations = "{send: {action: send, channel: $ROOM}}",
        components = "{schemas: {Q: {type: object, required: [limit], properties: {limit: " +
            "{$REF: '#/components/schemas/Limit'}}}, Limit: {type: integer}, " +
            "H: {type: object, properties: {'Bad Name': {type: string}, 'X-Ok': {type: string}}}}}",
    ),
)

private suspend fun kotlinOf(name: String): String {
    val generation = generationOf(name = NAME, text = shapes.getValue(name))
    val generated = generation.shouldBeInstanceOf<Generation.Generated>()
    Compiler.compile(generated.files).messages shouldBe ""
    return generated.files.joinToString("\n") { file -> file.content }
}

private suspend fun codesOf(name: String): List<String> =
    generationOf(name = NAME, text = shapes.getValue(name)).diagnostics.map { diagnostic -> diagnostic.code.value }

class ChannelShapesTest : FunSpec({
    context("a request of several messages") {
        test("is sent by the server as any of its messages and paired by the id of the one reply") {
            val kotlin = kotlinOf(SERVER_REQUEST)

            kotlin shouldContain "public suspend fun requestAsk(message: TRoomServerMessage, timeout: Duration): " +
                "ChannelReply<TRoomAckPayload> = session.request(message, timeout)"
            kotlin shouldContain """public fun clientId(element: JsonElement): String? = idAt(element, listOf("id"))"""
        }

        test("is sent by the client as any of its messages and paired by the id of the one reply") {
            val kotlin = kotlinOf(CLIENT_REQUEST)

            kotlin shouldContain "public suspend fun requestAsk(message: TRoomClientMessage, timeout: Duration): " +
                "ChannelReply<TRoomRPayload> = session.request(message, timeout)"
            kotlin shouldContain """public fun serverId(element: JsonElement): String? = idAt(element, listOf("id"))"""
        }
    }

    context("messages told apart by the discriminator their schemas declare") {
        test("are refused as the models of a discriminator that nothing extends") {
            codesOf(DISCRIMINATED) shouldBe List(2) { "codegen.schema.unsupported" }
        }
    }

    context("a handshake whose fields are schemas of the components") {
        test("reads them through their references and refuses a header whose name is no field name") {
            val generation = generationOf(name = NAME, text = shapes.getValue(HANDSHAKE))

            generation.diagnostics.single().message shouldContain "The header \"Bad Name\""
        }
    }
})
