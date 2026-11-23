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

import com.davils.kapi.spec.core.value.Value
import com.davils.kapi.spec.openapi.DiagnosticCodes
import com.davils.kapi.spec.openapi.Document
import com.davils.kapi.spec.openapi.OpenApi
import com.davils.kapi.spec.openapi.fixture.codes
import com.davils.kapi.spec.openapi.fixture.document
import com.davils.kapi.spec.openapi.fixture.parts
import com.davils.kapi.spec.openapi.fixture.readable
import com.davils.kapi.spec.openapi.fixture.reading31
import com.davils.kapi.spec.openapi.fixture.reading32
import com.davils.kapi.spec.openapi.fixture.rewritten
import com.davils.kapi.spec.openapi.model.content.Example
import com.davils.kapi.spec.openapi.resolve
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

private const val SERIALIZED: String = "{dataValue: {name: Rex}, serializedValue: '{\"name\":\"Rex\"}'}"

private const val EXTERNAL: String = "{dataValue: {name: Rex}, externalValue: 'https://example.com/rex.json'}"

private fun examples(vararg examples: String): List<String> =
    listOf("components:", "  examples:") + examples.mapIndexed { index, fields -> "    E$index: $fields" }

private fun Document.example(name: String): Example {
    val declared = parts().examples.getValue(name)
    return resolve(declared).shouldNotBeNull()
}

class ExampleReadingTest : FunSpec({
    context("an example of OpenAPI 3.2") {
        test("reads its data value and its serialized value") {
            val reading = reading32(examples(SERIALIZED, EXTERNAL))
            val serialized = reading.document().example("E0")

            reading.diagnostics.shouldBeEmpty()
            serialized.dataValue shouldBe Value.Mapping(mapOf("name" to Value.Text("Rex")))
            serialized.serializedValue shouldBe "{\"name\":\"Rex\"}"
        }

        test("is written with them and reads back") {
            val document = reading32(examples(SERIALIZED, EXTERNAL)).document()
            val written = OpenApi.write(document)

            written shouldContain "dataValue:"
            written shouldContain "serializedValue:"
            document.rewritten() shouldBe document
        }

        test("gives its value in one of the combinations OpenAPI allows") {
            val conflicting = examples(
                "{value: 1, dataValue: 1}",
                "{value: 1, serializedValue: '1'}",
                "{externalValue: 'https://example.com/a', serializedValue: '1'}",
                "{value: 1, externalValue: 'https://example.com/a'}",
            )
            val reading = reading32(conflicting)

            reading.codes() shouldBe List(4) { DiagnosticCodes.EXAMPLE_SHAPE }
        }
    }

    context("an example before OpenAPI 3.2") {
        test("has no data value or serialized value") {
            val reading = reading31(examples(SERIALIZED))
            val example = reading.readable().example("E0")

            reading.codes() shouldBe List(2) { DiagnosticCodes.FIELD_VERSION }
            example.dataValue.shouldBeNull()
            example.serializedValue.shouldBeNull()
        }
    }
})
