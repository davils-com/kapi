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
import com.davils.kapi.codegen.core.Generation
import com.davils.kapi.codegen.core.options.Artifact
import com.davils.kapi.spec.asyncapi.AsyncApi
import com.davils.kapi.spec.asyncapi.Document
import com.davils.kapi.spec.asyncapi.Reading
import com.davils.kapi.spec.core.diagnostic.Diagnostic
import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.core.source.Source
import com.davils.kore.core.validation.violation.ViolationCode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

private const val REF: String = "\$ref"

private const val ROOM: String = "{$REF: '#/channels/room'}"

private const val TEXT: String = "a: {payload: {type: string}}"

private const val PLAIN: String = "{address: /room, messages: {$TEXT}}"

private const val SERVERS: String = "{local: {host: localhost, protocol: ws}}"

private fun document(channels: String, operations: String = "{send: {action: send, channel: $ROOM}}"): String = listOf(
    "asyncapi: 3.0.0",
    "info: {title: T, version: '1'}",
    "defaultContentType: application/json",
    "servers: $SERVERS",
    "channels: $channels",
    "operations: $operations",
).joinToString(separator = "\n")

private fun room(channel: String): String = "{room: $channel}"

private suspend fun partialOf(text: String): Document {
    val reading = AsyncApi.reader().read(Source.Text(text, DocumentUri("file:///specs/p.yaml")))
    val invalid = reading as? Reading.Invalid
    val read = reading as? Reading.Read
    return invalid?.partial ?: read?.document.shouldNotBeNull()
}

private suspend fun generationOf(text: String): Generation {
    val generator = Codegen.asyncApi {
        packageName = "com.example.p"
        apiName = "P"
        artifacts = setOf(Artifact.Models, Artifact.Client, Artifact.Server)
    }
    return generator.generate(partialOf(text))
}

private val partials: Map<String, Pair<String, List<ViolationCode>>> = mapOf(
    "an operation on a channel that is missing" to (
        document(channels = room(PLAIN), operations = "{send: {action: send, channel: {$REF: '#/channels/none'}}}") to
            listOf(GenerationCodes.CHANNEL_SKIPPED, GenerationCodes.OPERATION_CHANNEL)
        ),
    "an operation that is missing" to (
        document(channels = room(PLAIN), operations = "{send: {$REF: '#/components/operations/none'}}") to
            listOf(GenerationCodes.CHANNEL_SKIPPED)
        ),
    "a channel that is missing" to (document("{room: {$REF: '#/components/channels/none'}}") to emptyList()),
    "a message that is missing" to (
        document(room("{address: /room, messages: {a: {$REF: '#/components/messages/none'}}}")) to emptyList()
        ),
    "a payload that is missing" to (
        document(room("{address: /room, messages: {a: {payload: {$REF: '#/components/schemas/none'}}}}")) to
            listOf(GenerationCodes.REFERENCE_UNSUPPORTED)
        ),
    "a parameter that is missing" to (
        document(room("{address: '/rooms/{id}', messages: {$TEXT}, parameters: {id: {$REF: '#/none'}}}")) to
            emptyList()
        ),
    "a server that is missing" to (
        document(room("{address: /room, messages: {$TEXT}, servers: [{$REF: '#/servers/none'}]}")) to emptyList()
        ),
    "a correlation id that is missing" to (
        document(room("{address: /room, messages: {a: {payload: {type: string}, correlationId: {$REF: '#/n'}}}}")) to
            listOf(GenerationCodes.CORRELATION_UNSUPPORTED)
        ),
    "a reply on a channel that is missing" to (
        document(
            channels = room(PLAIN),
            operations = "{ask: {action: receive, channel: $ROOM, reply: {channel: {$REF: '#/channels/n'}}}}",
        ) to
            listOf(GenerationCodes.REPLY_CHANNEL)
        ),
    "a handshake whose query is missing" to (
        document(room("{address: /room, messages: {$TEXT}, bindings: {ws: {query: {$REF: '#/none'}}}}")) to
            listOf(GenerationCodes.HANDSHAKE_UNSUPPORTED)
        ),
)

class PartialDocumentTest : FunSpec({
    context("the partial document of a reading that failed") {
        partials.forEach { (name, case) ->
            val (text, codes) = case

            test("with $name is generated without a failure and reports the gap where it is") {
                val generation = generationOf(text)

                generation.diagnostics.map(Diagnostic::code) shouldBe codes
                generation.diagnostics.forEach { diagnostic -> diagnostic.location.shouldNotBeNull() }
            }
        }
    }
})
