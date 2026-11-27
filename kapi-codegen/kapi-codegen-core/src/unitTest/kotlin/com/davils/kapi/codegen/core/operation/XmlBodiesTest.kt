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

package com.davils.kapi.codegen.core.operation

import com.davils.kapi.codegen.core.Codegen
import com.davils.kapi.codegen.core.CodegenCodes
import com.davils.kapi.codegen.core.Generation
import com.davils.kapi.codegen.core.fixture.LIST_PETS
import com.davils.kapi.codegen.core.fixture.clientGenerationOf
import com.davils.kapi.codegen.core.fixture.clientOf
import com.davils.kapi.codegen.core.fixture.codes
import com.davils.kapi.codegen.core.fixture.generator
import com.davils.kapi.codegen.core.fixture.get
import com.davils.kapi.codegen.core.fixture.paths
import com.davils.kapi.codegen.core.fixture.readAt
import com.davils.kapi.codegen.core.fixture.readWith
import com.davils.kapi.codegen.core.fixture.schemas
import com.davils.kapi.codegen.core.fixture.serverOf
import com.davils.kapi.codegen.core.fixture.textOf
import com.davils.kapi.codegen.core.options.Artifact
import com.davils.kapi.spec.core.diagnostic.DocumentUri
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf

private const val SHARED: String = "shared.yaml"

private const val SHARED_URI: String = "file:/work/specs/shared.yaml"

private const val PET: String = "{\$ref: '#/components/schemas/Pet'}"

private const val FLAT: String = "Pet: {type: object, properties: {name: {type: string}}}"

private const val XML: String = "application/xml"

private const val MODEL: String = "/PetstorePet.kt"

private const val ROOT: String = "@SerialName(\"Pet\")"

private const val UNION: String = "Pet: {type: object, properties: {kind: {oneOf: " +
    "[{\$ref: '#/components/schemas/A'}, {\$ref: '#/components/schemas/B'}]}}}"

private val unsupported: List<Any> = listOf(CodegenCodes.XML_UNSUPPORTED)

private fun sent(type: String, schema: String = PET): List<String> =
    get(LIST_PETS, "requestBody: {required: true, content: {'$type': {schema: $schema}}}")

private fun answered(type: String, schema: String = PET): List<String> {
    val content = "content: {'$type': {schema: $schema}}"
    return paths("/pets: {get: {$LIST_PETS, responses: {'200': {description: ok, $content}}}}")
}

private fun property(name: String, schema: String = "{type: string}"): List<String> =
    listOf("Pet: {type: object, properties: {$name: $schema}}")

private val refused: Map<String, List<String>> = mapOf(
    "an attribute" to property(name = "id", schema = "{type: string, xml: {attribute: true}}"),
    "a wrapped array" to
        property(name = "tags", schema = "{type: array, xml: {wrapped: true}, items: {type: string}}"),
    "a namespace" to listOf("Pet: {type: object, xml: {namespace: 'urn:pets'}, properties: {a: {type: string}}}"),
    "a prefix" to listOf("Pet: {type: object, xml: {prefix: p}, properties: {a: {type: string}}}"),
    "a name of its own" to listOf("Pet: {type: object, xml: {name: animal}, properties: {a: {type: string}}}"),
    "an xml object deep below" to property(name = "owner", schema = "{\$ref: '#/components/schemas/Owner'}") +
        listOf("Owner: {type: object, properties: {id: {type: string, xml: {attribute: true}}}}"),
    "a map" to property(name = "labels", schema = "{type: object, additionalProperties: {type: string}}"),
    "a free-form value" to property(name = "extra", schema = "{}"),
    "an array of arrays" to
        property(name = "grid", schema = "{type: array, items: {type: array, items: {type: integer}}}"),
    "an array of nulls" to property(name = "tags", schema = "{type: array, items: {type: [string, 'null']}}"),
    "a union" to listOf(
        UNION,
        "A: {type: object, properties: {a: {type: string}}}",
        "B: {type: object, properties: {b: {type: string}}}",
    ),
    "a type of the application" to property(name = "id", schema = "{type: string, x-kapi-type: kotlin.uuid.Uuid}"),
    "a property that is no XML name" to property(name = "'two words'"),
    "a property whose name XML reserves" to property(name = "xmlns"),
    "a component whose name is no XML name" to listOf("'1st': {type: object, properties: {a: {type: string}}}"),
    "a component whose name has a dot" to listOf("'a.b': {type: object, properties: {a: {type: string}}}"),
    "a property that is empty" to property(name = "''"),
    "a property that accepts anything" to property(name = "extra", schema = "true"),
    "a property of another document" to
        property(name = "price", schema = "{\$ref: '$SHARED#/components/schemas/Money'}"),
    "an enumeration" to listOf("Pet: {enum: [a, b]}"),
)

private val sharedLines: List<String> =
    schemas(listOf("Money: {type: object, properties: {amount: {type: integer}}}"))

private val accepted: List<String> = schemas(
    listOf(
        "Pet: {type: object, xml: {}, properties: {_id: {type: string}, Sku-2.x: {type: string}, " +
            "kind: {\$ref: '#/components/schemas/Kind'}, child: {\$ref: '#/components/schemas/Pet'}, " +
            "twin: {\$ref: '#/components/schemas/Pet'}, tags: {type: array, items: {type: string}}}}",
        "Kind: {enum: [cat, dog]}",
    ),
)

private suspend fun foreign(lines: List<String>): Generation {
    val sharedGenerator = Codegen.openApi {
        packageName = "com.example.shared"
        apiName = "Shared"
    }
    val shared = sharedGenerator.generate(readAt(DocumentUri(SHARED_URI), sharedLines).document)
    val catalog = shared.shouldBeInstanceOf<Generation.Generated>().catalog
    val reading = readWith(mapOf(DocumentUri(SHARED_URI) to textOf(sharedLines)), lines)
    return generator { artifacts = setOf(Artifact.Models, Artifact.Client) }.generate(
        reading.document,
        catalogs = listOf(catalog),
    )
}

private suspend fun refusal(lines: List<String>): Generation.Refused =
    foreign(lines).shouldBeInstanceOf<Generation.Refused>()

private suspend fun modelOf(lines: List<String>): String {
    val generated = clientGenerationOf(lines).shouldBeInstanceOf<Generation.Generated>()
    val model = generated.files.first { file -> file.path.endsWith(MODEL) }
    return model.content
}

class XmlBodiesTest : FunSpec({
    context("a body of XML") {
        test("is written and read by the codec of its media type, its model named after its component") {
            val lines = sent(XML) + schemas(listOf(FLAT))

            clientGenerationOf(lines).diagnostics.shouldBeEmpty()
            clientOf(lines) shouldContain
                "encoded(client.encode(PetstorePet.serializer(), body, MediaType.parse(\"application/xml\")))"
            modelOf(lines) shouldContain ROOT
            serverOf(lines) shouldContain
                "input.body(PetstorePet.serializer(), true, listOf(MediaRange.of(\"application/xml\")))"
        }

        test("is sent as text/xml, every +xml type and application/xml for a range of them") {
            clientOf(sent("text/xml") + schemas(listOf(FLAT))) shouldContain "MediaType.parse(\"text/xml\")"
            clientOf(sent("application/atom+xml") + schemas(listOf(FLAT))) shouldContain
                "MediaType.parse(\"application/atom+xml\")"
            clientOf(sent("application/*+xml") + schemas(listOf(FLAT))) shouldContain
                "MediaType.parse(\"application/xml\")"
        }

        test("comes after JSON, parts and forms when a request declares several") {
            val content = "requestBody: {required: true, content: {application/xml: {schema: $PET}, " +
                "application/json: {schema: $PET}}}"

            clientOf(get(LIST_PETS, content) + schemas(listOf(FLAT))) shouldContain "client.json("
        }

        test("accepts recursion, enumerations, lists, every XML name and an xml object that maps nothing") {
            foreign(sent(XML) + accepted).diagnostics.shouldBeEmpty()
        }

        test("is refused as the root when its model belongs to another document") {
            val generation = refusal(sent(type = XML, schema = "{\$ref: '$SHARED#/components/schemas/Money'}"))

            generation.diagnostics.single().message shouldContain "root element"
        }

        test("stays bytes in an answer of a range of XML types") {
            val lines = answered(type = "application/*+xml") + schemas(listOf(FLAT))

            clientOf(lines, name = "PetstoreDefaultClient") shouldContain "accept(MediaRange.of(\"application/*+xml\"))"
            modelOf(lines) shouldNotContain ROOT
        }

        test("stays bytes without a schema") {
            clientOf(get(LIST_PETS, "requestBody: {required: true, content: {application/xml: {}}}")) shouldContain
                "listPets(body: ByteString)"
        }

        test("is read from an answer when the answer declares no JSON") {
            val lines = answered(XML) + schemas(listOf(FLAT))

            modelOf(lines) shouldContain ROOT
            serverOf(lines) shouldContain
                "body(MediaType.parse(\"application/xml\"), PetstorePet.serializer(), response.body)"
        }

        test("leaves an answer of JSON and a model that is never XML as they are") {
            val content = "application/json: {schema: $PET}, application/xml: {schema: $PET}"
            val lines = paths("/pets: {get: {$LIST_PETS, responses: {'200': {description: ok, content: {$content}}}}}")

            modelOf(lines + schemas(listOf(FLAT))) shouldNotContain ROOT
        }

        test("is refused without a component that names its root element") {
            val inline = "{type: object, properties: {name: {type: string}}}"
            val generation = refusal(sent(type = XML, schema = inline))
            val listed = refusal(answered(type = XML, schema = "{type: array, items: $PET}") + schemas(listOf(FLAT)))

            generation.codes() shouldBe unsupported
            generation.diagnostics.single().message shouldContain "root element"
            listed.codes() shouldBe unsupported
        }

        refused.forEach { (case, components) ->
            test("is refused with a located diagnostic for $case") {
                val declared = components.first().substringBefore(':')
                val root = declared.trim('\'')
                val schema = "{\$ref: '#/components/schemas/$root'}"
                val generation = refusal(sent(type = XML, schema = schema) + schemas(components))
                val diagnostic = generation.diagnostics.first { found -> found.code == CodegenCodes.XML_UNSUPPORTED }

                diagnostic.message shouldContain "no faithful XML"
                diagnostic.location.shouldNotBeNull()
            }
        }
    }
})
