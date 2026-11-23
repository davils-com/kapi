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

package com.davils.kapi.codegen.core.sample

import com.davils.kapi.codegen.core.Codegen
import com.davils.kapi.codegen.core.CodegenCodes
import com.davils.kapi.codegen.core.Generation
import com.davils.kapi.codegen.core.fixture.UNMATCHED
import com.davils.kapi.codegen.core.fixture.codes
import com.davils.kapi.codegen.core.fixture.generator
import com.davils.kapi.codegen.core.fixture.mockGenerationOf
import com.davils.kapi.codegen.core.fixture.pet
import com.davils.kapi.codegen.core.fixture.read
import com.davils.kapi.codegen.core.fixture.readAt
import com.davils.kapi.codegen.core.fixture.readWith
import com.davils.kapi.codegen.core.fixture.samplesOf
import com.davils.kapi.codegen.core.fixture.schemas
import com.davils.kapi.codegen.core.fixture.textOf
import com.davils.kapi.codegen.core.options.Artifact
import com.davils.kapi.spec.core.diagnostic.DocumentUri
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf

private const val REF: String = "\$ref: '#/components/schemas/"
private const val OBJECT: String = "type: object"
private const val WHOLE: String = "Whole"
private const val BRANCH: String = "Branch"
private const val LEAF: String = "Leaf"
private const val SHAPE: String = "Shape"
private const val PET_MODEL: String = "Pet"
private const val OWNER_MODEL: String = "Owner"
private const val NAME: String = "name: {type: string}"
private const val BROKEN: String = "Broken"
private const val EMPTY_PET: String = "get() = PetstorePet()"
private const val OWNER: String = "owner: {${REF}Owner'}"
private const val PET: String = "pet: {${REF}Pet'}"
private const val SIZE: String = "size: {type: integer}"

private val shared: DocumentUri = DocumentUri("file:/work/specs/shared.yaml")

private val sharedLines: List<String> = schemas(listOf("Money: {type: object, properties: {amount: {type: integer}}}"))

private fun model(name: String, vararg properties: String, required: String = ""): List<String> {
    val body = properties.map { line -> "    $line" }
    return listOf("$name:", "  $OBJECT", "  required: [$required]", "  properties:") + body
}

private fun union(name: String, vararg members: String): List<String> {
    val references = members.joinToString { member -> "{$REF$member'}" }
    return listOf("$name:", "  oneOf: [$references]")
}

private val broken: List<String> = model(BROKEN, "code: $UNMATCHED", required = "code")

private val loop: List<String> = model("Loop", "next: {${REF}Loop'}", required = "next")

private suspend fun foreignSamples(): String {
    val sharedGenerator = Codegen.openApi {
        packageName = "com.example.shared"
        apiName = "Shared"
    }
    val catalog = sharedGenerator.generate(readAt(shared, sharedLines).document)
    val catalogs = listOf(catalog.shouldBeInstanceOf<Generation.Generated>().catalog)
    val lines = schemas(pet("price: {\$ref: 'shared.yaml#/components/schemas/Money'}"))
    val reading = readWith(mapOf(shared to textOf(sharedLines)), lines)
    val mocking = generator { artifacts = Artifact.entries.toSet() }
    val generation = mocking.generate(reading.document, catalogs = catalogs)
    val files = generation.shouldBeInstanceOf<Generation.Generated>().files
    return files.single { file -> file.path.endsWith("Samples.kt") }.content
}

class SamplePlannerTest : FunSpec({
    context("the samples of an api") {
        test("are named after their models without the prefix of the api, one for each shape") {
            val samples = samplesOf(pet("id: {type: string, readOnly: true}", NAME))

            samples shouldContain "public object PetstoreSamples {"
            samples shouldContain "public val pet: PetstorePet\n        get() = PetstorePet("
            samples shouldContain "public val petWrite: PetstorePetWrite\n        get() = PetstorePetWrite("
        }

        test("refer to each other where one model holds another") {
            val samples = samplesOf(model(OWNER_MODEL, NAME) + model(PET_MODEL, OWNER))

            samples shouldContain "owner = owner,"
        }

        test("leave out a model that has no sample itself") {
            val generation = mockGenerationOf(schemas(loop + model(PET_MODEL, "loop: {${REF}Loop'}")))
            val files = generation.shouldBeInstanceOf<Generation.Generated>().files

            files.single { file -> file.path.endsWith("Samples.kt") }.content shouldContain EMPTY_PET
        }

        test("leave out a model of another document") {
            foreignSamples() shouldContain EMPTY_PET
        }
    }

    context("models that refer to each other in a circle") {
        test("are cut where the later one may leave the earlier out") {
            val samples = samplesOf(model(OWNER_MODEL, PET) + model(PET_MODEL, OWNER))

            samples shouldContain "get() = PetstoreOwner()"
            samples shouldContain "owner = owner,"
        }

        test("are cut at a list of the model itself, which stays empty") {
            val samples = samplesOf(model(PET_MODEL, "friends: {type: array, items: {${REF}Pet'}}"))

            samples shouldContain "friends = emptyList(),"
        }

        test("keep what the circle requires and build it first") {
            val owner = model(OWNER_MODEL, PET, required = "pet")
            val samples = samplesOf(owner + model(PET_MODEL, OWNER))

            samples shouldContain EMPTY_PET
            samples shouldContain "pet = pet,"
        }

        test("have no sample when every model of the circle requires the next") {
            val generation = mockGenerationOf(schemas(loop))
            val finding = generation.diagnostics.single()

            generation.codes() shouldBe listOf(CodegenCodes.SAMPLE_UNAVAILABLE)
            finding.message shouldContain "No sample of PetstoreLoop is generated"
            finding.message shouldContain "its property next;"
            val location = checkNotNull(finding.location)

            location.pointer.toString() shouldBe "/components/schemas/Loop"
        }
    }

    context("a union") {
        test("is the first of its members that has a sample") {
            val whole = model(WHOLE, SIZE, required = "size")
            val samples = samplesOf(union(SHAPE, BROKEN, WHOLE) + broken + whole)

            samples shouldContain "public val shape: PetstoreShape\n        get() = whole"
            samples shouldNotContain "val broken"
        }

        test("has no sample when none of its members has one") {
            val other = model("Other", "mark: $UNMATCHED", required = "mark")
            val generation = mockGenerationOf(schemas(union(SHAPE, BROKEN, "Other") + broken + other))
            val messages = generation.diagnostics.map { diagnostic -> diagnostic.message }

            messages.single { message -> "PetstoreShape" in message } shouldContain "any of its members"
        }

        test("that one member holds again is built from the member that ends it") {
            val branch = model(
                BRANCH,
                "children: {type: array, minItems: 1, items: {${REF}Tree'}}",
                required = "children",
            )
            val leaf = model(LEAF, SIZE, required = "size")
            val samples = samplesOf(union("Tree", BRANCH, LEAF) + branch + leaf)

            samples shouldContain "public val tree: PetstoreTree\n        get() = leaf"
            samples shouldContain "children = listOf(tree),"
        }
    }

    context("an api without mocks") {
        test("has no samples") {
            val reading = read(schemas(pet(NAME)))
            val generation = generator().generate(reading.document, reading.sources)
            val files = generation.shouldBeInstanceOf<Generation.Generated>().files

            files.none { file -> "/testing/" in file.path } shouldBe true
        }
    }
})
