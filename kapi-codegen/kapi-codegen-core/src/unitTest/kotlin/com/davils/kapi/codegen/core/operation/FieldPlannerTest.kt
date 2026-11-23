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

import com.davils.kapi.codegen.core.CodegenCodes
import com.davils.kapi.codegen.core.Generation
import com.davils.kapi.codegen.core.fixture.LIST_PETS
import com.davils.kapi.codegen.core.fixture.clientGenerationOf
import com.davils.kapi.codegen.core.fixture.clientOf
import com.davils.kapi.codegen.core.fixture.codes
import com.davils.kapi.codegen.core.fixture.get
import com.davils.kapi.codegen.core.fixture.schemas
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf

private const val FORM: String = "application/x-www-form-urlencoded"

private const val PARTS: String = "multipart/form-data"

private const val TEXT: String = "{type: string}"

private const val FILE: String = "{type: string, format: binary}"

private const val PHOTO: String = "photo: $FILE"

private const val NUMBER: String = "{type: integer, format: int32}"

private const val NUMBERS: String = "{type: array, items: {type: integer}}"

private const val OPTIONAL: String = "listPets(a: String? = null)"

private const val META: String = "{type: object, properties: {title: {type: string}}}"

private const val META_JSON: String = "client.json(PetstoreListPetsRequestMeta.serializer(), meta)"

private val fields: Map<String, String> = mapOf(
    "user: $TEXT" to """if (user != null) field("user", user)""",
    "age: $NUMBER" to """if (age != null) field("age", age.toString())""",
    "scopes: {type: array, items: $TEXT}" to """if (scopes != null) fields("scopes", scopes)""",
    "ids: $NUMBERS" to """if (ids != null) fields("ids", ids.map { it.toString() })""",
    "meta: $META" to """if (meta != null) field("meta", $META_JSON.decodeToString())""",
    "page-size: {type: string, x-kapi-name: size}" to """if (size != null) field("page-size", size)""",
)

private val parts: Map<String, String> = mapOf(
    "name: $TEXT" to """if (name != null) text("name", name)""",
    "age: $NUMBER" to """if (age != null) text("age", age.toString())""",
    "labels: {type: array, items: $TEXT}" to """if (labels != null) texts("labels", labels)""",
    "ids: $NUMBERS" to """if (ids != null) texts("ids", ids.map { it.toString() })""",
    PHOTO to """if (photo != null) file("photo", photo)""",
    "photos: {type: array, items: $FILE}" to """if (photos != null) files("photos", photos)""",
    "meta: $META" to """if (meta != null) part("meta", MediaType.parse("application/json"), $META_JSON)""",
    "any: true" to
        """if (any != null) part("any", MediaType.parse("application/json"), """ +
        """client.json(JsonElement.serializer(), any))""",
)

private val encodings: Map<String, String> = mapOf(
    PHOTO to """file("photo", photo, MediaType.parse("image/png"))""",
    "photo: {type: array, items: $FILE}" to """files("photo", photo, MediaType.parse("image/png"))""",
    "photo: $META" to """part("photo", MediaType.parse("image/png"), client.json(""",
)

private val refusedEncodings: List<String> = listOf(
    "style: form",
    "explode: true",
    "allowReserved: true",
    "headers: {X-Rate: {schema: {type: string}}}",
)

private fun body(type: String, properties: String, more: String = ""): List<String> {
    val media = "{schema: {type: object, properties: {$properties}$more}}"
    return get(LIST_PETS, "requestBody: {required: true, content: {$type: $media}}")
}

private fun encoded(property: String, encoding: String): List<String> {
    val media = "{schema: {type: object, properties: {$property}}, encoding: {photo: {$encoding}}}"
    return get(LIST_PETS, "requestBody: {required: true, content: {$PARTS: $media}}")
}

class FieldPlannerTest : FunSpec({
    context("the field of a form") {
        fields.forEach { (property, code) ->
            test("is a parameter that is written as text: $property") {
                clientOf(body(type = FORM, properties = property)) shouldContain code
            }
        }

        test("is refused when it is a file") {
            clientGenerationOf(body(type = FORM, properties = PHOTO)).codes() shouldBe
                listOf(CodegenCodes.OPERATION_UNSUPPORTED)
        }
    }

    context("the part of a multipart body") {
        parts.forEach { (property, code) ->
            test("is a parameter that is written as its kind of part: $property") {
                clientOf(body(type = PARTS, properties = property)) shouldContain code
            }
        }

        test("is a file with its name and its type when the document says binary") {
            val kotlin = clientOf(body(type = PARTS, properties = "photo: $FILE, photos: {type: array, items: $FILE}"))

            kotlin shouldContain "photo: FilePart? = null"
            kotlin shouldContain "photos: List<FilePart>? = null"
        }

        encodings.forEach { (property, code) ->
            test("takes the content type the document gives it: $property") {
                clientOf(encoded(property, "contentType: image/png")) shouldContain code
            }
        }

        refusedEncodings.forEach { encoding ->
            test("is refused with an encoding beyond its content type: $encoding") {
                val generation = clientGenerationOf(encoded(PHOTO, encoding))

                generation.shouldBeInstanceOf<Generation.Refused>()
                generation.codes() shouldBe listOf(CodegenCodes.OPERATION_UNSUPPORTED)
            }
        }
    }

    context("the fields of a body") {
        test("are required as the schema says, with what is required first") {
            val kotlin = clientOf(body(type = FORM, properties = "a: $TEXT, b: $TEXT", more = ", required: [b]"))

            kotlin shouldContain "listPets(b: String, a: String? = null)"
            kotlin shouldContain """                field("b", b)"""
        }

        test("stay optional when they are required and their schema allows null") {
            val kotlin = clientOf(
                body(type = FORM, properties = "a: {type: [string, 'null']}", more = ", required: [a]"),
            )

            kotlin shouldContain OPTIONAL
        }

        test("are refused when the body has no schema to take them from") {
            val lines = get(LIST_PETS, "requestBody: {content: {$FORM: {}}}")

            clientGenerationOf(lines).codes() shouldBe listOf(CodegenCodes.OPERATION_UNSUPPORTED)
        }

        test("are those of the schema the components declare") {
            val content = "$FORM: {schema: {\$ref: '#/components/schemas/Login'}}"
            val lines = get(LIST_PETS, "requestBody: {required: true, content: {$content}}") +
                schemas(listOf("Login: {type: object, required: [user], properties: {user: $TEXT}}"))

            clientOf(lines) shouldContain "listPets(user: String)"
        }

        test("leave out what is only read and what x-kapi-ignore names") {
            val properties = "id: {type: string, readOnly: true}, hidden: {type: string, x-kapi-ignore: true}, a: $TEXT"
            val kotlin = clientOf(body(type = FORM, properties = properties))

            kotlin shouldContain OPTIONAL
            kotlin shouldNotContain "hidden"
        }

        test("keep a property whose x-kapi-ignore is false") {
            clientOf(body(type = FORM, properties = "a: {type: string, x-kapi-ignore: false}")) shouldContain OPTIONAL
        }

        test("are written in the shape to write of a model that is read differently") {
            val account = "Account: {type: object, properties: {id: {type: string, readOnly: true}, email: $TEXT}}"
            val lines = body(
                type = PARTS,
                properties = "account: {\$ref: '#/components/schemas/Account'}",
            ) +
                schemas(listOf(account))

            clientOf(lines) shouldContain "client.json(PetstoreAccountWrite.serializer(), account)"
        }

        test("are refused under a name the generated client uses itself") {
            clientGenerationOf(body(type = FORM, properties = "body: $TEXT")).codes() shouldBe
                listOf(CodegenCodes.NAME_COLLISION)
        }

        test("are refused under the name of a parameter") {
            val parameter = "parameters: [{name: user, in: query, schema: $TEXT}]"
            val media = "{schema: {type: object, properties: {user: $TEXT}}}"
            val lines = get(LIST_PETS, parameter, "requestBody: {content: {$FORM: $media}}")

            clientGenerationOf(lines).codes() shouldBe listOf(CodegenCodes.NAME_COLLISION)
        }
    }
})
