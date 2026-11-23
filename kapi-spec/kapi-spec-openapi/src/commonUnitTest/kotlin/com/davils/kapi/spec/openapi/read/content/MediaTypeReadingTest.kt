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

package com.davils.kapi.spec.openapi.read.content

import com.davils.kapi.spec.core.model.Referable
import com.davils.kapi.spec.openapi.DiagnosticCodes
import com.davils.kapi.spec.openapi.Document
import com.davils.kapi.spec.openapi.OpenApi
import com.davils.kapi.spec.openapi.fixture.codes
import com.davils.kapi.spec.openapi.fixture.defined
import com.davils.kapi.spec.openapi.fixture.document
import com.davils.kapi.spec.openapi.fixture.operation
import com.davils.kapi.spec.openapi.fixture.parts
import com.davils.kapi.spec.openapi.fixture.readable
import com.davils.kapi.spec.openapi.fixture.reading31
import com.davils.kapi.spec.openapi.fixture.reading32
import com.davils.kapi.spec.openapi.fixture.rewritten
import com.davils.kapi.spec.openapi.model.content.MediaType
import com.davils.kapi.spec.openapi.model.path.Method
import com.davils.kapi.spec.openapi.resolve
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

private const val EVENTS: String = "text/event-stream"

private val stream: List<String> = listOf(
    "paths:",
    "  /events:",
    "    get:",
    "      responses:",
    "        '200':",
    "          description: Events",
    "          content:",
    "            $EVENTS:",
    "              itemSchema: {type: object, properties: {data: {type: string}}}",
    "            multipart/mixed:",
    "              prefixEncoding: [{contentType: application/json}]",
    "              itemEncoding:",
    "                contentType: multipart/form-data",
    "                encoding: {file: {contentType: image/png}}",
)

private val shared: List<String> = listOf(
    "paths:",
    "  /pets:",
    "    post:",
    "      requestBody: {content: {application/json: {\$ref: '#/components/mediaTypes/Pet'}}}",
    "      responses: {'204': {description: Stored}}",
    "components:",
    "  mediaTypes:",
    "    Pet: {schema: {type: object}}",
    "    Same: {\$ref: '#/components/mediaTypes/Pet'}",
)

private fun Document.media(type: String): MediaType {
    val responses = operation("/events", Method.Get).responses
    val ok = resolve(responses.entries.values.single()).shouldNotBeNull()
    return resolve(ok.content.getValue(type)).shouldNotBeNull()
}

class MediaTypeReadingTest : FunSpec({
    context("a media type of OpenAPI 3.2") {
        test("reads the schema of its items and the encodings of its items") {
            val reading = reading32(stream)
            val document = reading.document()
            val events = document.media(EVENTS)
            val mixed = document.media("multipart/mixed")
            val item = mixed.itemEncoding.shouldNotBeNull()

            reading.diagnostics.shouldBeEmpty()
            events.itemSchema.defined().properties.keys shouldBe setOf("data")
            mixed.prefixEncoding.single().contentType shouldBe "application/json"
            item.contentType shouldBe "multipart/form-data"
            item.encoding.getValue("file").contentType shouldBe "image/png"
        }

        test("is written with them and reads back") {
            val document = reading32(stream).document()
            val written = OpenApi.write(document)

            written shouldContain "itemSchema:"
            written shouldContain "prefixEncoding:"
            written shouldContain "itemEncoding:"
            document.rewritten() shouldBe document
        }

        test("is shared through the components and referred to from content") {
            val reading = reading32(shared)
            val document = reading.document()
            val body = document.operation("/pets", Method.Post).requestBody.shouldNotBeNull()
            val content = document.resolve(body)?.content?.get("application/json").shouldNotBeNull()
            val pet = document.resolve(content).shouldNotBeNull()
            val same = document.parts().mediaTypes.getValue("Same")

            reading.diagnostics.shouldBeEmpty()
            content.shouldBeInstanceOf<Referable.Referenced>()
            pet.schema.defined().types.size shouldBe 1
            document.resolve(same) shouldBe pet
            document.rewritten() shouldBe document
        }

        test("reports a reference to a media type that does not exist") {
            val broken = shared.map { line -> line.replace("mediaTypes/Pet'}}}", "mediaTypes/Dog'}}}") }

            reading32(broken).codes() shouldBe listOf(DiagnosticCodes.REFERENCE_UNRESOLVED)
        }
    }

    context("a media type before OpenAPI 3.2") {
        test("has no items, encodings of items or references") {
            val reading = reading31(stream)
            val events = reading.readable().media(EVENTS)

            reading.codes() shouldBe List(3) { DiagnosticCodes.FIELD_VERSION }
            events.itemSchema.shouldBeNull()
        }

        test("is not shared through the components") {
            val reading = reading31(shared)

            reading.codes().toSet() shouldBe setOf(DiagnosticCodes.FIELD_UNKNOWN, DiagnosticCodes.FIELD_VERSION)
        }
    }
})
