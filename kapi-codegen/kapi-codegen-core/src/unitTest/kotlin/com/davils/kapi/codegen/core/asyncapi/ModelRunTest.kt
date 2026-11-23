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

import com.davils.kapi.codegen.core.asyncapi.fixture.generationOf
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val REF: String = "\$ref"

private const val PROPERTY: String = "/channels/room/messages/a/payload/properties/a"

private const val UNSUPPORTED: String = "codegen.schema.unsupported Error "

private fun document(payload: String, schemas: String): String = listOf(
    "asyncapi: 3.0.0",
    "info: {title: T, version: '1'}",
    "defaultContentType: application/json",
    "servers: {local: {host: localhost, protocol: ws}}",
    "channels: {room: {address: /room, messages: {a: {payload: $payload}}}}",
    "operations: {send: {action: receive, channel: {$REF: '#/channels/room'}}}",
    "components: {schemas: $schemas}",
).joinToString(separator = "\n")

private suspend fun findingsOf(payload: String, schemas: String = "{}"): List<String> {
    val generation = generationOf(name = "t.yaml", text = document(payload, schemas))
    return generation.diagnostics.map { diagnostic ->
        val pointer = diagnostic.location?.run { pointer.text }
        "${diagnostic.code.value} ${diagnostic.severity} " + pointer.orEmpty()
    }
}

class ModelRunTest : FunSpec({
    context("a finding of the models of a payload") {
        test("is located at the property of the payload in the AsyncAPI document") {
            findingsOf("{type: object, properties: {a: {type: object, patternProperties: {x: {}}}}}") shouldBe
                listOf(UNSUPPORTED + PROPERTY)
        }

        test("is located in the components where the payload refers to a schema") {
            val schemas = "{M: {type: object, properties: {a: {prefixItems: [{}]}}}}"

            findingsOf("{$REF: '#/components/schemas/M'}", schemas) shouldBe
                listOf(UNSUPPORTED + "/components/schemas/M/properties/a")
        }

        test("keeps the warnings of the reader and of the generator for the schemas of payloads") {
            findingsOf("{type: object, properties: {a: {type: string, pattern: '(?=x)'}}}") shouldBe listOf(
                UNSUPPORTED + PROPERTY,
                "openapi.schema.pattern-unsupported Warning $PROPERTY",
            )
            findingsOf("{type: object, properties: {a: {not: {}}}}") shouldBe
                listOf("codegen.schema.unchecked Warning $PROPERTY")
        }

        test("is located inside a member of a composition by its index") {
            findingsOf("{allOf: [{type: object, properties: {a: {not: {}}}}, {type: object}]}") shouldBe
                listOf("codegen.schema.unchecked Warning /channels/room/messages/a/payload/allOf/0/properties/a")
        }

        test("of the translation is located where the draft-07 keyword is") {
            findingsOf("{type: object, properties: {a: {type: array, items: [{}]}}}") shouldBe
                listOf("codegen.asyncapi.schema.unsupported Error $PROPERTY/items")
        }
    }
})
