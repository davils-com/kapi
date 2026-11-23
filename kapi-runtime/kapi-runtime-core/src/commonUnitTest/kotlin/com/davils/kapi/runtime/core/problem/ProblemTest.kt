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

package com.davils.kapi.runtime.core.problem

import com.davils.kapi.runtime.core.codec.Codecs
import com.davils.kapi.runtime.core.codec.Decoding
import com.davils.kapi.runtime.core.validation.ConstraintViolationCodes
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope
import com.davils.kore.core.validation.validate
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive

private const val NOT_FOUND: Int = 404

private const val BALANCE: Int = 30

private fun traced(): Problem = Problem(detail = "line\nbreak", extensions = mapOf("trace" to JsonPrimitive("id")))

private const val EMPTY: String = "{}"

private const val RFC_EXAMPLE: String = """{"type":"https://example.com/probs/out-of-credit",""" +
    """"title":"You do not have enough credit.","detail":"Your current balance is 30, but that costs 50.",""" +
    """"instance":"/account/12345/msgs/abc","balance":30,"accounts":["/account/12345","/account/67890"]}"""

private val terse: Json = Json { encodeDefaults = false }

private val eager: Json = Json { encodeDefaults = true }

private val failing: Validatable = object : Validatable {
    val name: String = ""

    override fun ValidationScope.validate() {
        fail(::name, "must be at least 1 character long", ConstraintViolationCodes.MIN_LENGTH)
    }
}

private val full: Problem = Problem(
    type = "https://example.com/problems/not-found",
    title = "Not found",
    status = NOT_FOUND,
    detail = "No pet 7",
    instance = "/pets/7",
    code = "pets.not-found",
    violations = listOf(
        ProblemViolation(path = "id", code = "kapi.runtime.constraint.minimum", message = "must be at least 1"),
    ),
)

class ProblemTest : FunSpec({
    context("a problem") {
        test("is blank unless it says more") {
            val problem = Problem()

            problem.type shouldBe Problem.BLANK_TYPE
            problem.title shouldBe null
            problem.violations shouldBe emptyList()
            problem.extensions shouldBe emptyMap()
            Problem.MEDIA_TYPE shouldBe "application/problem+json"
        }

        test("is written without what it leaves out and read back as itself") {
            terse.encodeToString(ProblemSerializer, Problem()) shouldBe EMPTY
            terse.decodeFromString(ProblemSerializer, EMPTY) shouldBe Problem()
        }

        test("is written and read with every member RFC 9457 and kapi give it") {
            val text = terse.encodeToString(ProblemSerializer, full)

            terse.decodeFromString(ProblemSerializer, text) shouldBe full
            Json.decodeFromString(Problem.serializer(), Json.encodeToString(Problem.serializer(), full)) shouldBe full
        }

        test("keeps the extension members of the example of RFC 9457 and writes them back") {
            val problem = Json.decodeFromString(Problem.serializer(), RFC_EXAMPLE)

            problem.title shouldBe "You do not have enough credit."
            problem.extensions["balance"] shouldBe JsonPrimitive(BALANCE)
            problem.extensions["accounts"] shouldBe JsonArray(
                listOf(JsonPrimitive("/account/12345"), JsonPrimitive("/account/67890")),
            )
            Json.parseToJsonElement(Json.encodeToString(Problem.serializer(), problem)) shouldBe
                Json.parseToJsonElement(RFC_EXAMPLE)
        }

        test("ignores a member of the wrong type and a status out of range") {
            val text = """{"type":7,"title":["x"],"status":"404","detail":null,"code":1,"violations":[{"path":"a"}]}"""
            val problem = Json.decodeFromString(Problem.serializer(), text)

            problem shouldBe Problem()
            Json.decodeFromString(Problem.serializer(), """{"status":1000}""").status shouldBe null
            Json.decodeFromString(Problem.serializer(), """{"status":404.5}""").status shouldBe null
            Json.decodeFromString(Problem.serializer(), """{"violations":{}}""").violations shouldBe emptyList()
        }

        test("is refused when it is no object") {
            shouldThrow<SerializationException> { Json.decodeFromString(Problem.serializer(), "[]") }
        }

        test("is written with its defaults when the format asks for them") {
            val text = eager.encodeToString(ProblemSerializer, Problem())

            text shouldContain "about:blank"
            text shouldContain "\"violations\":[]"
            eager.decodeFromString(ProblemSerializer, text) shouldBe Problem()
        }

        test("is read as application/problem+json by the codecs, also as CBOR") {
            val json = Codecs.Default.codecFor(Problem.MEDIA_TYPE).shouldNotBeNull()
            val content = json.encode(Problem.serializer(), full)
            val cbor = Codecs.Default.codecFor("application/cbor").shouldNotBeNull()

            Codecs.Default.decode(Problem.serializer(), content, Problem.MEDIA_TYPE) shouldBe Decoding.Decoded(full)
            cbor.decode(Problem.serializer(), cbor.encode(Problem.serializer(), full)) shouldBe Decoding.Decoded(full)
        }

        test("refuses a status out of range and an extension that takes the name of a member") {
            shouldThrow<IllegalArgumentException> { Problem(status = 99) }
            shouldThrow<IllegalArgumentException> { Problem(extensions = mapOf("title" to JsonPrimitive("x"))) }
        }

        test("is equal by its members and shows its texts escaped") {
            val problem = traced()

            problem shouldBe traced()
            problem.hashCode() shouldBe traced().hashCode()
            problem shouldNotBe Problem()
            problem.toString() shouldNotContain "\n"
            problem.toString() shouldContain "extensions=[trace]"
            ProblemViolation(path = "a\nb", code = "c", message = "d").toString() shouldNotContain "\n"
        }

        test("needs every member of a violation") {
            shouldThrow<SerializationException> {
                Json.decodeFromString(ProblemViolation.serializer(), """{"path":"id"}""")
            }
            ProblemViolation(
                path = "a",
                code = "b",
                message = "c",
            ).hashCode() shouldBe ProblemViolation(path = "a", code = "b", message = "c").hashCode()
            ProblemViolation(path = "a", code = "b", message = "c").equals("a") shouldBe false
        }
    }

    context("invalid") {
        test("answers once with every violation, its path and its code") {
            val problem = Problem.invalid(failing.validate("request"))
            val violation = problem.violations.single()

            problem.status shouldBe Problem.INVALID_STATUS
            problem.code shouldBe Problem.INVALID_CODE
            problem.title shouldBe "The request is invalid"
            violation.path shouldBe "name"
            violation.code shouldBe "kapi.runtime.constraint.min-length"
            violation.message shouldBe "must be at least 1 character long"
        }
    }
})
