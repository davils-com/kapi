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

import com.davils.kapi.codegen.core.CodegenCodes
import com.davils.kapi.codegen.core.fixture.LIST_PETS
import com.davils.kapi.codegen.core.fixture.clientFilesOf
import com.davils.kapi.codegen.core.fixture.clientGenerationOf
import com.davils.kapi.codegen.core.fixture.clientOf
import com.davils.kapi.codegen.core.fixture.codes
import com.davils.kapi.codegen.core.fixture.get
import com.davils.kapi.codegen.core.fixture.schemas
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

private const val PATCH: String = "PetstorePetPatch"

private const val TRI_STATE: String = "x-kapi-tri-state: true"

private const val OWNER: String = "Owner: {type: object, properties: {name: {type: string}}}"

private const val PROPERTIES: String =
    "name: {type: string, minLength: 2}, nick: {type: string}, id: {type: string, readOnly: true}, " +
        "tags: {type: array, items: {type: string, maxLength: 9}}, owner: {\$ref: '#/components/schemas/Owner'}"

private const val PET: String = "Pet: {type: object, required: [name], properties: {$PROPERTIES}}"

private fun patching(content: String, vararg keywords: String): List<String> {
    val body = "requestBody: {required: true, content: {$content}}"
    return get(LIST_PETS, body, *keywords) + schemas(listOf(PET, OWNER))
}

private fun pet(vararg keywords: String): List<String> =
    patching("application/merge-patch+json: {schema: {\$ref: '#/components/schemas/Pet'}}", *keywords)

class ModelPatchesTest : FunSpec({
    context("the body of an operation that patches") {
        test("is the patch shape of its model") {
            val kotlin = clientOf(pet(TRI_STATE))

            kotlin shouldContain "listPets(body: $PATCH)"
            kotlin shouldContain "client.json($PATCH.serializer(), body)"
        }

        test("is the model itself without the keyword or when it says false") {
            clientOf(pet()) shouldContain "listPets(body: PetstorePetWrite)"
            clientOf(pet("x-kapi-tri-state: false")) shouldContain "listPets(body: PetstorePetWrite)"
            clientFilesOf(pet()) shouldNotContain "model/$PATCH.kt"
        }
    }

    context("the patch shape of a model") {
        test("has every property that is written, absent unless the caller sets or clears it") {
            val kotlin = clientOf(pet(TRI_STATE), PATCH)

            kotlin shouldContain "public val name: Patch<String> = Patch.Absent,"
            kotlin shouldContain "public val nick: Patch<String> = Patch.Absent,"
            kotlin shouldContain "public val tags: Patch<List<String>> = Patch.Absent,"
            kotlin shouldContain "public val owner: Patch<PetstoreOwner> = Patch.Absent,"
            kotlin shouldNotContain "val id"
        }

        test("never writes a property that is absent, whatever the codec does with defaults") {
            val kotlin = clientOf(pet(TRI_STATE), PATCH)

            kotlin shouldContain "@OptIn(ExperimentalSerializationApi::class)"
            kotlin shouldContain "@EncodeDefault(EncodeDefault.Mode.NEVER)\n    public val name: Patch<String>"
        }

        test("refuses to clear what its model requires, and nothing else") {
            val kotlin = clientOf(pet(TRI_STATE), PATCH)

            kotlin shouldContain
                """if (name is Patch.Null) fail(::name, "must not be null", ViolationCodes.NOT_NULL)"""
            kotlin shouldNotContain "if (nick is Patch.Null)"
        }

        test("checks a value that is set as its model would") {
            val kotlin = clientOf(pet(TRI_STATE), PATCH)

            kotlin shouldContain "if (name is Patch.Value) {"
            kotlin shouldContain "Constraints.length(name.value) < 2"
            kotlin shouldContain "each(::tags, tags.value) { item ->"
            kotlin shouldContain "within(::owner) { with(owner.value) { validate() } }"
            kotlin shouldNotContain "if (nick is Patch.Value)"
        }

        test("is written next to the model and its shape to write") {
            val files = clientFilesOf(pet(TRI_STATE))

            files shouldContain "model/$PATCH.kt"
            files shouldContain "model/PetstorePet.kt"
            files shouldContain "model/PetstorePetWrite.kt"
        }

        test("is refused under the name of another model") {
            val lines = pet(TRI_STATE) + listOf("    PetPatch: {type: object, properties: {a: {type: string}}}")

            clientGenerationOf(lines).codes() shouldBe listOf(CodegenCodes.NAME_COLLISION)
        }
    }

    context("a body that cannot be patched") {
        listOf(
            "application/json: {schema: {type: string}}",
            "application/x-www-form-urlencoded: {schema: {type: object, properties: {a: {type: string}}}}",
            "application/octet-stream: {schema: {type: string, format: binary}}",
        ).forEach { content ->
            test("is refused: $content") {
                clientGenerationOf(patching(content, TRI_STATE)).codes() shouldBe
                    listOf(CodegenCodes.OPERATION_UNSUPPORTED)
            }
        }
    }

    context("an operation without a body") {
        test("ignores that it patches") {
            clientOf(get(LIST_PETS, TRI_STATE)) shouldContain "listPets()"
        }
    }
})
