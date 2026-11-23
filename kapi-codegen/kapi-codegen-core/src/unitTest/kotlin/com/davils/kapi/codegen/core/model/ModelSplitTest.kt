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
import com.davils.kapi.codegen.core.fixture.codes
import com.davils.kapi.codegen.core.fixture.generatedOf
import com.davils.kapi.codegen.core.fixture.kotlinOf
import com.davils.kapi.codegen.core.fixture.refusedOf
import com.davils.kapi.codegen.core.fixture.schemas
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

private const val ID: String = "id: {type: string, readOnly: true}"

private const val PASSWORD: String = "password: {type: string, writeOnly: true}"

private const val FIELDS: String = "{$ID, email: {type: string}, $PASSWORD}"

private const val ACCOUNT: String = "Account: {type: object, required: [id, email], properties: $FIELDS}"

private const val READ: String = "PetstoreAccount"

private const val WRITE: String = "PetstoreAccountWrite"

private const val TO_ACCOUNT: String = "{\$ref: '#/components/schemas/Account'}"

private suspend fun paths(lines: List<String>): List<String> {
    val files = generatedOf(schemas(lines)).files
    return files.map { file -> file.path }
}

class ModelSplitTest : FunSpec({
    context("an object with a property that goes one way only") {
        test("has a shape to read without what is only written") {
            val kotlin = kotlinOf(listOf(ACCOUNT), READ)

            kotlin shouldContain "public val id: String,"
            kotlin shouldContain "public val email: String,"
            kotlin shouldNotContain "password"
        }

        test("has a shape to write without what is only read, even when that is required") {
            val kotlin = kotlinOf(listOf(ACCOUNT), WRITE)

            kotlin shouldContain "public data class PetstoreAccountWrite("
            kotlin shouldContain "public val email: String,"
            kotlin shouldContain "public val password: String? = null"
            kotlin shouldNotContain "val id"
        }

        test("checks a dependency only in the shape that has both properties") {
            val fields = "{$ID, note: {type: string}}"
            val account = "Account: {type: object, properties: $fields, dependentRequired: {id: [note]}}"

            kotlinOf(listOf(account), READ) shouldContain "DEPENDENT_REQUIRED"
            kotlinOf(listOf(account), WRITE) shouldNotContain "DEPENDENT_REQUIRED"
        }
    }

    context("an object without such a property") {
        test("has one shape") {
            paths(listOf("Note: {type: object, properties: {text: {type: string}}}")) shouldNotContain
                "com/example/petstore/model/PetstoreNoteWrite.kt"
        }
    }

    context("what holds an object with two shapes") {
        test("has two shapes itself and holds the one that fits, in a list and a map as well") {
            val members = "members: {type: array, items: $TO_ACCOUNT}"
            val byName = "byName: {type: object, additionalProperties: $TO_ACCOUNT}"
            val team = "Team: {type: object, properties: {owner: $TO_ACCOUNT, $members, $byName, level: {enum: [a]}}}"
            val write = kotlinOf(listOf(team, ACCOUNT), "PetstoreTeamWrite")
            val read = kotlinOf(listOf(team, ACCOUNT), "PetstoreTeam")

            write shouldContain "public val owner: PetstoreAccountWrite? = null"
            write shouldContain "public val members: List<PetstoreAccountWrite>? = null"
            write shouldContain "public val byName: Map<String, PetstoreAccountWrite>? = null"
            write shouldContain "public val level: PetstoreTeamLevel? = null"
            read shouldContain "public val owner: PetstoreAccount? = null"
        }

        test("is a union with two shapes when it is a union") {
            val person = "Person: {type: object, required: [kind], properties: {kind: {type: string}, $ID}}"
            val robot = "Robot: {type: object, required: [kind], properties: {kind: {type: string}}}"
            val refs = "[{\$ref: '#/components/schemas/Person'}, {\$ref: '#/components/schemas/Robot'}]"
            val lines = listOf("Actor: {oneOf: $refs, discriminator: {propertyName: kind}}", person, robot)

            kotlinOf(lines, "PetstoreActorWrite") shouldContain "public sealed interface PetstoreActorWrite"
            kotlinOf(lines, "PetstoreActorWriteSerializer") shouldContain
                """"Person" to PetstorePersonWrite.serializer(),"""
            kotlinOf(lines, "PetstoreActorWriteSerializer") shouldContain
                """"Robot" to PetstoreRobot.serializer(),"""
            kotlinOf(lines, "PetstorePersonWrite") shouldContain "    PetstoreActorWrite {"
            kotlinOf(lines, "PetstorePerson") shouldContain "    PetstoreActor {"
            kotlinOf(lines, "PetstoreRobot") shouldContain "    PetstoreActor,"
            kotlinOf(lines, "PetstoreRobot") shouldContain "    PetstoreActorWrite {"
            paths(lines) shouldContain "com/example/petstore/model/PetstoreActor.kt"
        }
    }

    context("the name of a shape to write") {
        test("collides with a schema that already has it") {
            val taken = "AccountWrite: {type: object, properties: {a: {type: string}}}"

            refusedOf(schemas(listOf(ACCOUNT, taken))).codes() shouldBe listOf(CodegenCodes.NAME_COLLISION)
        }
    }
})
