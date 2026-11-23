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
import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kapi.spec.openapi.DiagnosticCodes
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

private const val PATHS_FIELD: String = "paths"

private const val PET_FIELD: String = "/pets/{id}"

private const val PARAMETERS: String = "parameters"

private const val PATHS: String = "paths:"

private const val PET_PATH: String = "  /pets/{id}:"

private suspend fun operationWith(vararg parameters: String): Upgraded {
    val listed = parameters.map { parameter -> "        - $parameter" }
    return upgradedOf(listOf(PATHS, PET_PATH, "    get:", "      parameters:") + listed + "      responses: {}")
}

private val Upgraded.parameters: YamlNode? get() = root[PATHS_FIELD][PET_FIELD]["get"][PARAMETERS]

class ParameterUpgradeTest : FunSpec({
    context("a parameter") {
        test("moves its type and its constraints into a schema and keeps everything else") {
            val upgrade = operationWith(
                "{name: limit, in: query, description: At most, required: false, allowEmptyValue: true, " +
                    "x-go-name: Limit, " +
                    "type: integer, format: int32, default: 10, maximum: 100, exclusiveMaximum: true, minimum: 1, " +
                    "multipleOf: 1, enum: [10, 50]}",
            )

            upgrade.parameters shouldBe nodeOf(
                "- name: limit",
                "  in: query",
                "  description: At most",
                "  required: false",
                "  allowEmptyValue: true",
                "  x-go-name: Limit",
                "  schema: {type: integer, format: int32, default: 10, exclusiveMaximum: 100, minimum: 1, " +
                    "multipleOf: 1, enum: [10, 50]}",
            )
        }

        test("declares no schema when it names no type") {
            operationWith("{name: id, in: path, required: true}").parameters shouldBe
                nodeOf("- {name: id, in: path, required: true}")
        }

        test("points into the parameters of the components when it is a reference") {
            val upgrade = upgraded(
                "paths: {/pets: {get: {parameters: [{${'$'}ref: '#/parameters/limit'}], responses: {}}}}",
                "parameters: {limit: {name: limit, in: query, type: integer}}",
            )

            upgrade.root[PATHS_FIELD]["/pets"]["get"][PARAMETERS] shouldBe
                nodeOf("- ${'$'}ref: '#/components/parameters/limit'")
            upgrade.root["components"][PARAMETERS] shouldBe
                nodeOf("limit: {name: limit, in: query, schema: {type: integer}}")
        }
    }

    context("a list in the query") {
        test("is written as a form for csv, which is the default, and for multi") {
            operationWith(
                "{name: a, in: query, type: array, items: {type: string}}",
                "{name: b, in: query, type: array, items: {type: string}, collectionFormat: multi}",
            ).parameters shouldBe nodeOf(
                "- {name: a, in: query, schema: {type: array, items: {type: string}}, style: form, explode: false}",
                "- {name: b, in: query, schema: {type: array, items: {type: string}}, style: form, explode: true}",
            )
        }

        test("is delimited by spaces for ssv and by pipes for pipes") {
            operationWith(
                "{name: a, in: query, type: array, collectionFormat: ssv}",
                "{name: b, in: query, type: array, collectionFormat: pipes}",
            ).parameters shouldBe nodeOf(
                "- {name: a, in: query, schema: {type: array}, style: spaceDelimited, explode: false}",
                "- {name: b, in: query, schema: {type: array}, style: pipeDelimited, explode: false}",
            )
        }

        test("keeps its default style and is noted when it is delimited by tabs") {
            val upgrade = operationWith("{name: a, in: query, type: array, collectionFormat: tsv}")

            upgrade.parameters shouldBe nodeOf("- {name: a, in: query, schema: {type: array}}")
            val note = upgrade.notes.single()
            note.code shouldBe DiagnosticCodes.UPGRADE_LOSSY
            note.location?.pointer shouldBe JsonPointer.of(PATHS_FIELD, PET_FIELD, "get", PARAMETERS, "0")
        }
    }

    context("a list in the path or a header") {
        test("is written in the simple style for csv") {
            operationWith(
                "{name: id, in: path, required: true, type: array}",
                "{name: X-Ids, in: header, type: array, collectionFormat: csv}",
            ).parameters shouldBe nodeOf(
                "- {name: id, in: path, required: true, schema: {type: array}, style: simple, explode: false}",
                "- {name: X-Ids, in: header, schema: {type: array}, style: simple, explode: false}",
            )
        }

        test("is noted for every other format") {
            operationWith("{name: id, in: path, required: true, type: array, collectionFormat: multi}")
                .notes
                .single()
                .code shouldBe DiagnosticCodes.UPGRADE_LOSSY
        }
    }

    context("the items") {
        test("become a schema and note a format of their own") {
            val upgrade = operationWith(
                "{name: a, in: query, type: array, collectionFormat: multi, " +
                    "items: {type: array, collectionFormat: pipes, items: {type: file}}}",
            )

            upgrade.parameters shouldBe nodeOf(
                "- name: a",
                "  in: query",
                "  schema: {type: array, items: {type: array, items: {type: string, format: binary}}}",
                "  style: form",
                "  explode: true",
            )
            upgrade.notes
                .single()
                .location
                ?.pointer shouldBe
                JsonPointer.of(PATHS_FIELD, PET_FIELD, "get", PARAMETERS, "0", "items")
        }

        test("stay as written when they name no type") {
            operationWith("{name: a, in: query, type: array, items: {x-kind: any}}").parameters shouldBe
                nodeOf(
                    "- {name: a, in: query, schema: {type: array, items: {x-kind: any}}, style: form, explode: false}",
                )
        }
    }

    context("the parameters of a path") {
        test("stay at the path and are upgraded like those of an operation") {
            val upgrade = upgraded(
                PATHS,
                PET_PATH,
                "    summary: One pet",
                "    x-owner: pets",
                "    parameters: [{name: id, in: path, required: true, type: integer}]",
                "    get: {responses: {}}",
            )

            val item = upgrade.root[PATHS_FIELD][PET_FIELD]
            item[PARAMETERS] shouldBe nodeOf("- {name: id, in: path, required: true, schema: {type: integer}}")
            item["summary"] shouldBe nodeOf("One pet")
            item["x-owner"] shouldBe nodeOf("pets")
            item["get"] shouldBe nodeOf("responses: {}")
            upgrade.notes.shouldBeEmpty()
        }

        test("leave the path without parameters when they all move into the bodies of its operations") {
            val upgrade = upgraded(
                "paths: {/pets: {parameters: [{name: pet, in: body, schema: {type: object}}], post: {responses: {}}}}",
            )

            upgrade.root[PATHS_FIELD]["/pets"][PARAMETERS].shouldBeNull()
        }

        test("keep a path that points elsewhere, an extension of the paths and a path that is no object") {
            val upgrade = upgraded(
                PATHS,
                "  /pets: {${'$'}ref: 'pets.yaml#/definitions/Pets'}",
                "  /none: null",
                "  x-paths: kept",
            )

            upgrade.root[PATHS_FIELD] shouldBe nodeOf(
                "/pets: {${'$'}ref: 'pets.yaml#/components/schemas/Pets'}",
                "/none: null",
                "x-paths: kept",
            )
        }
    }
})
