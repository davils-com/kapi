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

import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kapi.codegen.core.asyncapi.read.Targets
import com.davils.kapi.spec.asyncapi.AsyncApi
import com.davils.kapi.spec.asyncapi.Reading
import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.core.model.Target
import com.davils.kapi.spec.core.source.Source
import com.davils.kapi.spec.core.value.Value
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

private const val REF: String = "\$ref"

private const val COMPONENTS: String = "components"

private const val SCHEMAS: String = "schemas"

private val uri: DocumentUri = DocumentUri("file:///specs/t.yaml")

private val text: String = listOf(
    "asyncapi: 3.0.0",
    "info: {title: T, version: '1'}",
    "channels: {room: {address: /room}}",
    "components:",
    "  schemas:",
    "    A: {$REF: '#/components/schemas/B'}",
    "    B: {type: string}",
    "    Loop: {$REF: '#/components/schemas/Back'}",
    "    Back: {$REF: '#/components/schemas/Loop'}",
).joinToString(separator = "\n")

private suspend fun targets(): Targets {
    val reading = AsyncApi.reader().read(Source.Text(text, uri))
    return Targets(reading.shouldBeInstanceOf<Reading.Invalid>().partial)
}

private fun reference(text: String): Value = Value.Mapping(mapOf(REF to Value.Text(text)))

private fun at(document: DocumentUri, vararg tokens: String): Target = Target(document, JsonPointer.of(*tokens))

class TargetsTest : FunSpec({
    context("a reference of a document") {
        test("names a place in the same document by its fragment, percent-decoded") {
            val targets = targets()

            targets.targetOf("#/components/schemas/A", uri) shouldBe at(uri, COMPONENTS, SCHEMAS, "A")
            targets.targetOf("#/a%20b/%C3%A4", uri) shouldBe at(uri, "a b", "ä")
        }

        test("names a place in another document, relative to the one it is in or absolute") {
            val targets = targets()
            val other = DocumentUri("file:///specs/other.yaml")
            val remote = DocumentUri("https://example.com/s.yaml")

            targets.targetOf("other.yaml#/a", uri) shouldBe at(other, "a")
            targets.targetOf("https://example.com/s.yaml#/a", uri) shouldBe at(remote, "a")
            targets.targetOf("?v=1#/a", uri).shouldNotBeNull()
        }

        test("names nothing for a broken escape, an escape of no UTF-8, a fragment that is no pointer or no URI") {
            val targets = targets()

            listOf("#/a%zz", "#/a%2", "#/a%C3%28", "#a", "#/a%", "http://[::1#/a").forEach { broken ->
                targets.targetOf(broken, uri).shouldBeNull()
            }
        }
    }

    context("a schema of the components") {
        test("is found by its key only in the components of the document itself") {
            val targets = targets()

            targets.componentKey(at(uri, COMPONENTS, SCHEMAS, "A")) shouldBe "A"
            targets.componentKey(at(uri, "components", "messages", "A")).shouldBeNull()
            targets.componentKey(at(uri, "channels", "schemas", "A")).shouldBeNull()
            targets.componentKey(at(uri, COMPONENTS, SCHEMAS)).shouldBeNull()
            targets.componentKey(at(DocumentUri("file:///specs/o.yaml"), COMPONENTS, SCHEMAS, "A")).shouldBeNull()
        }

        test("is resolved through its references until a schema that is none, and not at all in a circle") {
            val targets = targets()

            targets.resolved(reference("#/components/schemas/A")) shouldBe
                targets.schemaAt(at(uri, COMPONENTS, SCHEMAS, "B"))?.definition
            targets.resolved(reference("#/components/schemas/Loop")).shouldBeNull()
            targets.resolved(reference("#/components/schemas/None")).shouldBeNull()
            targets.resolved(reference("#/a%zz")).shouldBeNull()
            targets.resolved(null).shouldBeNull()
            targets.resolved(Value.Text("plain")) shouldBe Value.Text("plain")
        }

        test("is the target of an inline schema by its location") {
            val targets = targets()

            targets.schemaAt(at(uri, COMPONENTS, SCHEMAS, "None")).shouldBeNull()
            targets.channel("room") shouldBe at(uri, "channels", "room")
            targets.referenceOf(Value.Text("x")).shouldBeNull()
        }
    }
})
