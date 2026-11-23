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


package com.davils.kapi.codegen.core

import com.davils.kapi.codegen.core.fixture.API
import com.davils.kapi.codegen.core.fixture.PACKAGE
import com.davils.kapi.codegen.core.fixture.UNMATCHED
import com.davils.kapi.codegen.core.fixture.clientGenerationOf
import com.davils.kapi.codegen.core.fixture.codes
import com.davils.kapi.codegen.core.fixture.generationOf
import com.davils.kapi.codegen.core.fixture.generator
import com.davils.kapi.codegen.core.fixture.get
import com.davils.kapi.codegen.core.fixture.mockGenerationOf
import com.davils.kapi.codegen.core.fixture.operation
import com.davils.kapi.codegen.core.fixture.parameters
import com.davils.kapi.codegen.core.fixture.paths
import com.davils.kapi.codegen.core.fixture.pet
import com.davils.kapi.codegen.core.fixture.read
import com.davils.kapi.codegen.core.fixture.responses
import com.davils.kapi.codegen.core.fixture.schema
import com.davils.kapi.codegen.core.fixture.schemas
import com.davils.kapi.codegen.core.options.Visibility
import kotlinx.io.bytestring.toHexString
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

private const val DIGEST: String = "sha256:"

private const val DIGEST_LENGTH: Int = 71

private const val PAGED: String = "x-kapi-pagination: {style: page, parameter: position}"

private const val POSITION: String = "parameters: [{name: position, in: query, schema: {type: integer}}]"

private const val TEXT_ANSWER: String =
    "responses: {'200': {description: ok, content: {application/json: {schema: {type: string}}}}}"

private val examples: Map<Any, List<String>> = mapOf(
    CodegenCodes.EXTENSION_UNKNOWN to operation("x-kapi-retries: 3"),
    CodegenCodes.EXTENSION_SITE to schema("x-kapi-idempotent: true"),
    CodegenCodes.EXTENSION_VALUE to operation("x-kapi-timeout: soon"),
    CodegenCodes.NAME_COLLISION to schemas(listOf("pet:", "  type: string", "Pet:", "  type: string")),
    CodegenCodes.SCHEMA_UNSUPPORTED to schema("allOf:", "  - type: string"),
    CodegenCodes.SCHEMA_UNCHECKED to schema("not: {maxLength: 3}"),
    CodegenCodes.REFERENCE_UNSUPPORTED to schemas(
        listOf("Pet:", "  type: object", "  properties:", "    name:", "      type: string") +
            listOf("Tag:", "  \$ref: '#/components/schemas/Pet/properties/name'"),
    ),
    CodegenCodes.ENUM_VALUES to schema("enum: [a, 1]"),
    CodegenCodes.PROPERTY_IGNORED to schemas(
        listOf("Pet:", "  type: object", "  required: [name]", "  properties:", "    name:", "      type: string") +
            listOf("      x-kapi-ignore: true"),
    ),
    CodegenCodes.OPERATION_UNSUPPORTED to parameters("name: filter, in: query, schema: {type: object}"),
    CodegenCodes.EXTENSION_PENDING to get("x-kapi-pagination: {style: link}"),
    CodegenCodes.PARAMETER_RESERVED_HEADER to
        parameters("name: Accept, in: header, schema: {type: string}"),
    CodegenCodes.PAGINATION_INVALID to paths("/pets: {get: {$PAGED, $POSITION, $TEXT_ANSWER}}"),
    CodegenCodes.SAMPLE_UNAVAILABLE to schemas(pet("code: $UNMATCHED", required = "code")),
    CodegenCodes.MOCK_UNANSWERED to
        responses("'200': {description: ok, content: {application/json: {schema: $UNMATCHED}}}"),
)

private val mocked: Set<Any> = setOf(
    CodegenCodes.SAMPLE_UNAVAILABLE,
    CodegenCodes.MOCK_UNANSWERED,
)

private suspend fun provoked(code: Any, lines: List<String>): Generation {
    if (code in mocked) return mockGenerationOf(lines)
    return clientGenerationOf(lines)
}

class GeneratorTest : FunSpec({
    context("every code of the generator") {
        test("has a document that provokes it") {
            examples.keys.toList() shouldBe CodegenCodes.all
        }

        examples.forEach { (code, lines) ->
            test("$code is the one finding of its document") {
                provoked(code, lines).codes() shouldBe listOf(code)
            }
        }

        test("lives below kide.openapi.generator") {
            CodegenCodes.all.forEach { code -> code.value shouldStartWith "codegen." }
        }
    }

    context("a refused generation") {
        test("lists its findings in the order of the document") {
            val generation = generationOf(
                operation("x-kapi-retries: 3", "x-kapi-timeout: soon") + schema("x-kapi-idempotent: true"),
            )

            generation.codes() shouldBe listOf(
                CodegenCodes.EXTENSION_UNKNOWN,
                CodegenCodes.EXTENSION_VALUE,
                CodegenCodes.EXTENSION_SITE,
            )
        }
    }

    context("the provenance") {
        test("is a resource that names the api, the sources and the options without a path or a time") {
            val reading = read(schema())
            val generation = generator().generate(reading.document, reading.sources)
            val generated = generation.shouldBeInstanceOf<Generation.Generated>()
            val file = generated.files.single()
            val provenance = generated.provenance

            file.path shouldBe "META-INF/kapi/openapi/petstore.provenance.json"
            file.kind shouldBe GeneratedFileKind.Resource
            file.content shouldEndWith "}\n"
            file.content shouldNotContain "/work/"
            Json.decodeFromString(Provenance.serializer(), file.content) shouldBe provenance
            provenance.protocol shouldBe Provenance.PROTOCOL
            provenance.api shouldBe API
            provenance.packageName shouldBe PACKAGE
            val source = reading.sources.single()

            provenance.sources shouldBe listOf(Fingerprint("petstore.yaml", DIGEST + source.digest.toHexString()))
            provenance.options shouldStartWith DIGEST
            provenance.options.length shouldBe DIGEST_LENGTH
            provenance.files shouldBe emptyList()
        }

        test("is not read from a record that lacks a field") {
            shouldThrow<SerializationException> {
                Json.decodeFromString(Provenance.serializer(), "{}")
            }
            shouldThrow<SerializationException> {
                Json.decodeFromString(Fingerprint.serializer(), "{}")
            }
        }

        test("is the same for the same document and options") {
            val reading = read(schema())

            generator().generate(reading.document, reading.sources) shouldBe
                generator().generate(reading.document, reading.sources)
        }

        test("changes its options digest with the options and has no sources without any") {
            val document = read(schema()).document
            val public = generator().generate(document)
            val internal = generator { visibility = Visibility.Internal }.generate(document)
            val first = public.shouldBeInstanceOf<Generation.Generated>().provenance
            val second = internal.shouldBeInstanceOf<Generation.Generated>().provenance

            first.sources shouldBe emptyList()
            second.options shouldNotBe first.options
        }
    }
})
