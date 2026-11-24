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

import com.davils.kapi.codegen.core.Codegen
import com.davils.kapi.codegen.core.options.Artifact
import com.davils.kapi.spec.asyncapi.AsyncApi
import com.davils.kapi.spec.asyncapi.Reading
import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.core.source.Source
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val REF: String = "\$ref"

private const val PAYLOAD: String = "/channels/room/messages/a/payload"

private const val FIELD_TYPE: String = "openapi.field.type Error@"

private const val MIN_LENGTH: String = "$FIELD_TYPE$PAYLOAD/minLength"

private fun document(payload: String): String = listOf(
    "asyncapi: 3.0.0",
    "info: {title: T, version: '1'}",
    "defaultContentType: application/json",
    "servers: {local: {host: localhost, protocol: ws}}",
    "channels: {room: {address: /room, messages: {a: {payload: $payload}}}}",
    "operations: {send: {action: receive, channel: {$REF: '#/channels/room'}}}",
).joinToString(separator = "\n")

private suspend fun findingsOf(payload: String): List<String> {
    val reading = AsyncApi.reader().read(Source.Text(document(payload), DocumentUri("file:///specs/t.yaml")))
    val partial = reading as? Reading.Invalid
    val document = partial?.partial ?: (reading as Reading.Read).document
    val generator = Codegen.asyncApi {
        packageName = "com.example.t"
        apiName = "T"
        artifacts = setOf(Artifact.Models, Artifact.Client, Artifact.Server)
    }
    return generator.generate(document).diagnostics.map { diagnostic ->
        "${diagnostic.code.value} ${diagnostic.severity}@" + diagnostic.location?.run { pointer.text }
    }
}

private val payloads: List<Pair<String, List<String>>> = listOf(
    "{type: string, minLength: -1}" to listOf(MIN_LENGTH),
    "{type: string, minLength: x}" to listOf(MIN_LENGTH),
    "{type: string, minLength: true}" to listOf(MIN_LENGTH),
    "{type: string, minLength: null}" to listOf(MIN_LENGTH),
    "{type: integer, multipleOf: 0}" to listOf("openapi.schema.constraint Error@$PAYLOAD"),
    "{type: object, properties: {a: {type: string, format: 7}}}" to
        listOf("$FIELD_TYPE$PAYLOAD/properties/a/format"),
    "{type: object, title: 7}" to listOf("$FIELD_TYPE$PAYLOAD/title"),
    "{enum: []}" to listOf("codegen.enum.values Error@$PAYLOAD"),
)

class InnerModelRunTest : FunSpec({
    context("a payload that only the OpenAPI reading of its models refuses") {
        payloads.forEach { (payload, expected) ->
            test("$payload is refused where the AsyncAPI document wrote it") {
                findingsOf(payload) shouldBe expected
            }
        }
    }
})
