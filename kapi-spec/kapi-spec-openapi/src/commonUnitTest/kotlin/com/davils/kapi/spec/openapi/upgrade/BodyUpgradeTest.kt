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

package com.davils.kapi.spec.openapi.upgrade

import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.get
import com.davils.kapi.spec.openapi.DiagnosticCodes
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

private const val PATHS_FIELD: String = "paths"

private const val PETS_FIELD: String = "/pets"

private const val REQUEST_BODY: String = "requestBody"

private const val CONTENT: String = "content:"

private const val PARAMETERS: String = "parameters"

private const val PET_CONTENT: String = "content: {application/json: {schema: {${'$'}ref: '#/components/schemas/Pet'}}}"

private const val FORM: String = "  application/x-www-form-urlencoded:"

private const val PET: String = "{name: pet, in: body, schema: {${'$'}ref: '#/definitions/Pet'}}"

private const val PATHS: String = "paths:"

private const val PETS: String = "  /pets:"

private const val POST: String = "    post:"

private suspend fun postWith(vararg lines: String, parameters: List<String>): Upgraded {
    val operation = listOf(PATHS, PETS, POST, "      parameters: [${parameters.joinToString()}]", "      responses: {}")
    return upgradedOf(lines.asList() + operation)
}

private val Upgraded.post: YamlNode? get() = root[PATHS_FIELD][PETS_FIELD]["post"]

private val Upgraded.body: YamlNode? get() = post[REQUEST_BODY]

class BodyUpgradeTest : FunSpec({
    context("a body parameter") {
        test("becomes a request body with one content per type the operation consumes") {
            val upgrade = upgraded(
                PATHS,
                PETS,
                POST,
                "      consumes: [application/json, application/xml]",
                "      parameters:",
                "        - {name: pet, in: body, description: The pet, required: true, x-go-name: Pet, " +
                    "schema: {${'$'}ref: '#/definitions/Pet'}}",
                "      responses: {}",
            )

            upgrade.body shouldBe nodeOf(
                "description: The pet",
                "required: true",
                "x-go-name: Pet",
                CONTENT,
                "  application/json: {schema: {${'$'}ref: '#/components/schemas/Pet'}}",
                "  application/xml: {schema: {${'$'}ref: '#/components/schemas/Pet'}}",
            )
            upgrade.post[PARAMETERS].shouldBeNull()
            upgrade.post["consumes"].shouldBeNull()
        }

        test("consumes the types of the document and JSON when nobody names one") {
            postWith("consumes: [text/plain]", parameters = listOf(PET)).body shouldBe
                nodeOf("content: {text/plain: {schema: {${'$'}ref: '#/components/schemas/Pet'}}}")
            postWith(parameters = listOf("{name: pet, in: body, required: false}")).body shouldBe
                nodeOf("content: {application/json: {}}")
        }

        test("is inlined when it is a parameter the document shares") {
            val upgrade = postWith(
                "parameters: {pet: $PET, limit: {name: limit, in: query, type: integer}}",
                parameters = listOf("{${'$'}ref: '#/parameters/pet'}"),
            )

            upgrade.body shouldBe nodeOf(
                PET_CONTENT,
            )
            upgrade.root["components"][PARAMETERS] shouldBe
                nodeOf("limit: {name: limit, in: query, schema: {type: integer}}")
        }

        test("is found through a shared name that escapes a slash") {
            postWith("parameters: {a/b: $PET}", parameters = listOf("{${'$'}ref: '#/parameters/a~1b'}")).body shouldBe
                nodeOf(PET_CONTENT)
        }

        test("stays a parameter reference when it names nothing the document shares") {
            val references = mapOf(
                "'#/parameters/missing'" to "'#/components/parameters/missing'",
                "'#/parameters/a/b'" to "'#/components/parameters/a/b'",
                "'other.yaml#/parameters/pet'" to "'other.yaml#/components/parameters/pet'",
            )
            references.forEach { (reference, upgraded) ->
                val upgrade = postWith(parameters = listOf("{${'$'}ref: $reference}"))

                upgrade.body.shouldBeNull()
                upgrade.post[PARAMETERS] shouldBe nodeOf("- {${'$'}ref: $upgraded}")
            }
        }
    }

    context("the content type header") {
        test("names the types of the body and leaves the parameters") {
            val upgrade = postWith(
                parameters = listOf(
                    "{name: Content-type, in: header, type: string, enum: [application/x-tar]}",
                    "{name: X-Registry-Config, in: header, type: string}",
                    "{name: context, in: body, schema: {type: string, format: binary}}",
                ),
            )

            upgrade.body shouldBe nodeOf("content: {application/x-tar: {schema: {type: string, format: binary}}}")
            upgrade.post[PARAMETERS] shouldBe nodeOf(
                "- {name: X-Registry-Config, in: header, schema: {type: string}}",
            )
        }

        test("is dropped and leaves the types to consumes when it names none") {
            postWith(parameters = listOf("{name: Content-Type, in: header, type: string}", PET)).body shouldBe
                nodeOf(PET_CONTENT)
        }
    }

    context("form parameters") {
        test("become an encoded form with the style of every list and the fields that are required") {
            val upgrade = postWith(
                parameters = listOf(
                    "{name: name, in: formData, type: string, required: true, description: The name}",
                    "{name: tags, in: formData, type: array, items: {type: string}, collectionFormat: multi}",
                    "{name: kind, in: formData}",
                ),
            )

            upgrade.body shouldBe nodeOf(
                "required: true",
                CONTENT,
                FORM,
                "    schema:",
                "      type: object",
                "      required: [name]",
                "      properties:",
                "        name: {type: string, description: The name}",
                "        tags: {type: array, items: {type: string}}",
                "        kind: {}",
                "    encoding:",
                "      tags: {style: form, explode: true}",
            )
        }

        test("become parts when a field is a file and the operation names no form type") {
            postWith(parameters = listOf("{name: upload, in: formData, type: file}")).body shouldBe nodeOf(
                CONTENT,
                "  multipart/form-data:",
                "    schema: {type: object, properties: {upload: {type: string, format: binary}}}",
            )
        }

        test("follow the form types the operation consumes and encode no style for parts") {
            postWith(
                "consumes: [multipart/form-data, application/json]",
                parameters = listOf("{name: tags, in: formData, type: array}"),
            ).body shouldBe nodeOf(
                CONTENT,
                "  multipart/form-data:",
                "    schema: {type: object, properties: {tags: {type: array}}}",
            )
        }

        test("skip a field without a name and note a list delimited by tabs") {
            val upgrade = postWith(
                parameters = listOf(
                    "{in: formData, type: string}",
                    "{name: ids, in: formData, type: array, collectionFormat: tsv}",
                ),
            )

            upgrade.body shouldBe nodeOf(
                CONTENT,
                FORM,
                "    schema: {type: object, properties: {ids: {type: array}}}",
            )
            upgrade.notes.single().code shouldBe DiagnosticCodes.UPGRADE_LOSSY
        }
    }

    context("the parameters of a path") {
        test("hand their body to every operation that declares none") {
            val upgrade = upgraded(
                PATHS,
                PETS,
                "    parameters: [$PET, {name: tag, in: formData, type: string}]",
                "    post: {responses: {}}",
                "    put: {parameters: [{name: replacement, in: body, schema: {type: string}}], responses: {}}",
            )

            val item = upgrade.root[PATHS_FIELD][PETS_FIELD]
            item["post"][REQUEST_BODY] shouldBe
                nodeOf(PET_CONTENT)
            item["put"][REQUEST_BODY] shouldBe nodeOf("content: {application/json: {schema: {type: string}}}")
        }

        test("hand their form fields on unless the operation declares a field of the same name") {
            val upgrade = upgraded(
                PATHS,
                PETS,
                "    parameters: [{name: tag, in: formData, type: string}, {name: kind, in: formData, type: string}]",
                "    post: {parameters: [{name: tag, in: formData, type: integer}], responses: {}}",
            )

            upgrade.root[PATHS_FIELD][PETS_FIELD]["post"][REQUEST_BODY] shouldBe nodeOf(
                CONTENT,
                FORM,
                "    schema: {type: object, properties: {tag: {type: integer}, kind: {type: string}}}",
            )
        }
    }
})
