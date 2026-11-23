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
import com.davils.kapi.spec.core.diagnostic.Diagnostic
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val REF: String = "\$ref"

private const val SESSION: String = "com/example/t/client/TRoomClientSession.kt"

private const val SEND: String = "public suspend fun send(message: "

private const val SENT: String = "): Boolean = session.send(message)"

private const val INT: String = "Int"

private const val TEXT: String = "String"

private const val LONG: String = "Long"

private const val ANYTHING: String = "JsonElement"

private val DEEP: String = "{type: array, items: ".repeat(DEPTH) + "{type: string}" + "}".repeat(DEPTH)

private const val DEPTH: Int = 17

private const val REFUSED: String = "refused [codegen.schema.unsupported]"

private const val UNSUPPORTED: String = "refused [codegen.asyncapi.payload.unsupported]"

private const val SCHEMAS: String = "{Id: {type: string}, Alias: {$REF: '#/components/schemas/Count'}, " +
    "Count: {type: integer, format: int32}, M: {type: object, properties: {a: {type: string}}}}"

private fun document(payload: String): String = listOf(
    "asyncapi: 3.0.0",
    "info: {title: T, version: '1'}",
    "defaultContentType: application/json",
    "servers: {local: {host: localhost, protocol: ws}}",
    "channels: {room: {address: /room, messages: {a: {payload: $payload}}}}",
    "operations: {send: {action: receive, channel: {$REF: '#/channels/room'}}}",
    "components: {schemas: $SCHEMAS}",
).joinToString(separator = "\n")

private suspend fun sentOf(payload: String): String {
    val generation = generationOf(name = "t.yaml", text = document(payload))
    val codes = generation.diagnostics.map(Diagnostic::code)
    val generated = generation as? Generation.Generated ?: return "refused " + codes.map { code -> code.value }
    val session = generated.files.first { file -> file.path == SESSION }.content
    val line = session.lines().first { text -> SEND in text }
    val trimmed = line.trim()
    return trimmed.removePrefix(SEND).removeSuffix(SENT)
}

private val payloads: List<Pair<String, String>> = listOf(
    "{type: string}" to TEXT,
    "{type: integer}" to LONG,
    "{type: integer, format: int32}" to INT,
    "{type: integer, format: int64}" to LONG,
    "{type: number}" to "Double",
    "{type: boolean}" to "Boolean",
    "{}" to ANYTHING,
    "true" to ANYTHING,
    "false" to REFUSED,
    "{type: array, items: {type: integer, format: int32}}" to "List<Int>",
    "{type: array, items: {type: array, items: {type: boolean}}}" to "List<List<Boolean>>",
    "{type: array}" to UNSUPPORTED,
    "{type: [string, integer]}" to REFUSED,
    "{$REF: '#/components/schemas/Id'}" to TEXT,
    "{$REF: '#/components/schemas/Alias'}" to INT,
    "{type: array, items: {$REF: '#/components/schemas/M'}}" to "List<TM>",
    "{type: array, items: {$REF: '#/components/schemas/Id'}}" to "List<String>",
    "{type: array, items: true}" to "List<$ANYTHING>",
    "{type: array, items: false}" to REFUSED,
    "{type: object}" to UNSUPPORTED,
    "{type: 'null'}" to UNSUPPORTED,
    "{type: string, format: uuid}" to TEXT,
    DEEP to UNSUPPORTED,
)

class PayloadTypesTest : FunSpec({
    context("the payload of a message") {
        payloads.forEach { (payload, expected) ->
            test("$payload is sent as $expected") {
                sentOf(payload) shouldBe expected
            }
        }
    }
})
