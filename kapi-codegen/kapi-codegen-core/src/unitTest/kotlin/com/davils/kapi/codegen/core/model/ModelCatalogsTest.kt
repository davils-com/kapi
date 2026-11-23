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

package com.davils.kapi.codegen.core.model

import com.davils.kapi.codegen.core.CatalogModel
import com.davils.kapi.codegen.core.Codegen
import com.davils.kapi.codegen.core.CodegenCodes
import com.davils.kapi.codegen.core.Generation
import com.davils.kapi.codegen.core.ModelCatalog
import com.davils.kapi.codegen.core.ModelKind
import com.davils.kapi.codegen.core.fixture.codes
import com.davils.kapi.codegen.core.fixture.generator
import com.davils.kapi.codegen.core.fixture.pet
import com.davils.kapi.codegen.core.fixture.readAt
import com.davils.kapi.codegen.core.fixture.readWith
import com.davils.kapi.codegen.core.fixture.schemas
import com.davils.kapi.codegen.core.fixture.textOf
import com.davils.kapi.spec.core.diagnostic.DocumentUri
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

private const val TO_SHARED: String = "shared.yaml#/components/schemas/"

private val shared: DocumentUri = DocumentUri("file:/work/specs/shared.yaml")

private val sharedLines: List<String> = schemas(
    listOf(
        "Money: {type: object, required: [amount], properties: {amount: {type: integer, minimum: 0}}}",
        "Currency: {enum: [EUR, USD]}",
        "Account: {type: object, properties: {id: {type: string, readOnly: true}, name: {type: string}}}",
        "Code: {type: string}",
    ),
)

private fun model(type: String, kind: ModelKind, hasWriteShape: Boolean): CatalogModel =
    CatalogModel(type, kind, hasWriteShape)

private suspend fun catalog(): ModelCatalog {
    val sharedGenerator = Codegen.openApi {
        packageName = "com.example.shared"
        apiName = "Shared"
    }
    val generation = sharedGenerator.generate(readAt(shared, sharedLines).document)
    return generation.shouldBeInstanceOf<Generation.Generated>().catalog
}

private suspend fun generationOf(property: String, catalogs: List<ModelCatalog>): Generation {
    val lines = schemas(pet(property))
    val reading = readWith(mapOf(shared to textOf(sharedLines)), lines)
    return generator().generate(reading.document, catalogs = catalogs)
}

private suspend fun kotlinOf(property: String, name: String = "PetstorePet"): String {
    val generation = generationOf(property, listOf(catalog()))
    val files = generation.shouldBeInstanceOf<Generation.Generated>().files
    return files.first { file -> file.path.endsWith("/$name.kt") }.content
}

class ModelCatalogsTest : FunSpec({
    context("the catalogue of a generation") {
        test("names the document, the package and every model with its kind and its shapes") {
            val catalog = catalog()

            catalog.document shouldBe shared.value
            catalog.packageName shouldBe "com.example.shared.model"
            catalog.models shouldBe mapOf(
                "Account" to model("SharedAccount", ModelKind.Object, hasWriteShape = true),
                "Currency" to model("SharedCurrency", ModelKind.Enum, hasWriteShape = false),
                "Money" to model("SharedMoney", ModelKind.Object, hasWriteShape = false),
            )
        }

        test("is written and read as JSON and not read from a record that lacks a field") {
            val catalog = catalog()
            val json = Json.encodeToString(ModelCatalog.serializer(), catalog)

            Json.decodeFromString(ModelCatalog.serializer(), json) shouldBe catalog
            shouldThrow<SerializationException> { Json.decodeFromString(ModelCatalog.serializer(), "{}") }
            shouldThrow<SerializationException> { Json.decodeFromString(CatalogModel.serializer(), "{}") }
        }
    }

    context("a reference into a document that has a catalogue") {
        test("uses the model that document already generated and validates it") {
            val kotlin = kotlinOf("a: {\$ref: '${TO_SHARED}Money'}")

            kotlin shouldContain "import com.example.shared.model.SharedMoney"
            kotlin shouldContain "public val a: SharedMoney? = null"
            kotlin shouldContain "within(::a) { with(a) { validate() } }"
        }

        test("checks an enumeration of that document as its own") {
            kotlinOf("a: {\$ref: '${TO_SHARED}Currency'}") shouldContain "if (!a.isKnown) fail("
        }

        test("gets a shape to write when the model it uses has one") {
            val write = kotlinOf(property = "a: {\$ref: '${TO_SHARED}Account'}", name = "PetstorePetWrite")

            write shouldContain "import com.example.shared.model.SharedAccountWrite"
            write shouldContain "public val a: SharedAccountWrite? = null"
        }

        test("is refused when it points at what is no model there or into a model") {
            val refused = listOf(CodegenCodes.REFERENCE_UNSUPPORTED)
            val catalogs = listOf(catalog())

            generationOf("a: {\$ref: '${TO_SHARED}Code'}", catalogs).codes() shouldBe refused
            generationOf("a: {\$ref: '${TO_SHARED}Money/properties/amount'}", catalogs).codes() shouldBe refused
        }
    }

    context("a reference into a document without a catalogue") {
        test("is refused") {
            generationOf("a: {\$ref: '${TO_SHARED}Money'}", emptyList()).codes() shouldBe
                listOf(CodegenCodes.REFERENCE_UNSUPPORTED)
        }
    }
})
