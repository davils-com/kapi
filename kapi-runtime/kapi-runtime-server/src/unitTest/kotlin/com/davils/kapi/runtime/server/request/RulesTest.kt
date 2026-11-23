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

package com.davils.kapi.runtime.server.request

import com.davils.kapi.runtime.core.codec.multipart.MultipartPart
import com.davils.kapi.runtime.core.negotiation.MediaType
import com.davils.kapi.runtime.core.parameter.ParameterFormat
import com.davils.kapi.runtime.core.parameter.ParameterStyle
import com.davils.kapi.runtime.core.problem.Problem
import com.davils.kapi.runtime.core.problem.ProblemSerializer
import com.davils.kapi.runtime.core.problem.ProblemViolation
import com.davils.kapi.runtime.core.validation.ConstraintViolationCodes
import com.davils.kapi.runtime.server.fixture.Filter
import com.davils.kapi.runtime.server.fixture.Note
import com.davils.kapi.runtime.server.problem.ServerCodes
import com.davils.kapi.runtime.server.reply.Reply
import com.davils.kapi.runtime.server.reply.ReplyBody
import com.davils.kore.core.validation.ValidationScope
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.io.bytestring.decodeToString
import kotlinx.io.bytestring.encodeToByteString
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

private const val LIMIT: String = "limit"

private const val NOTE: String = "note"

private const val TAG: String = "tag"

private const val MOST: Int = 9

private const val TWELVE: Int = 12

private const val TOO_MANY: String = "must be at most 9"

private const val FEW_ITEMS: String = "must have at least 2 items"

private const val SHORT: String = "must have at least 2 characters"

private val number: ParameterType<Int> = ParameterType(Int.serializer(), isQuoted = false)

private val form: ParameterFormat = ParameterFormat(LIMIT, ParameterStyle.Form, isExploded = true)

private val most: ValidationScope.(Int) -> Unit = { value ->
    if (value > MOST) fail(TOO_MANY, ConstraintViolationCodes.MAXIMUM)
}

private val two: ValidationScope.(List<Int>) -> Unit = { items ->
    if (items.size < 2) fail(FEW_ITEMS, ConstraintViolationCodes.MIN_ITEMS)
}

private val long: ValidationScope.(String) -> Unit = { text ->
    if (text.length < 2) fail(SHORT, ConstraintViolationCodes.MIN_LENGTH)
}

private fun request(rawQuery: String): Input =
    Request.of(operation = "probe", method = "POST", path = "/probe") { this.rawQuery = rawQuery }.input()

private fun part(name: String, text: String): MultipartPart {
    val disposition = "Content-Disposition" to "form-data; name=\"$name\""
    return MultipartPart(listOf(disposition), text.encodeToByteString())
}

private fun violationsOf(reply: Reply): List<ProblemViolation> {
    val body = reply.body.shouldBeInstanceOf<ReplyBody.Bytes>()
    return Json.decodeFromString(ProblemSerializer, body.content.decodeToString()).violations
}

private fun maximum(path: String): ProblemViolation =
    ProblemViolation(path = path, code = ConstraintViolationCodes.MAXIMUM.value, message = TOO_MANY)

class RulesTest : FunSpec({
    context("the rules of a schema") {
        test("check a parameter once it is read and report under its path") {
            val input = request("limit=12")

            val read = input.value(place = Place.Query, format = form, type = number, isRequired = true, rule = most)

            read shouldBe TWELVE
            input.isValid shouldBe false
            violationsOf(input.refused()) shouldBe listOf(maximum("query.limit"))
        }

        test("leave a parameter that keeps them, a missing one and a malformed one to the other checks") {
            val kept = request("limit=9")
            val absent = request("")
            val malformed = request("limit=x")

            kept.value(place = Place.Query, format = form, type = number, isRequired = true, rule = most) shouldBe MOST
            absent.value(place = Place.Query, format = form, type = number, isRequired = false, rule = most) shouldBe
                null
            malformed.value(place = Place.Query, format = form, type = number, isRequired = true, rule = most) shouldBe
                null

            kept.isValid shouldBe true
            absent.isValid shouldBe true
            violationsOf(malformed.refused()).map(ProblemViolation::code) shouldBe listOf(ServerCodes.MALFORMED)
        }

        test("check the items of a list as a whole") {
            val input = request("limit=1")

            input.items(place = Place.Query, format = form, type = number, isRequired = true, rule = two) shouldBe
                listOf(1)
            val fewer = ProblemViolation(
                path = "query.limit",
                code = ConstraintViolationCodes.MIN_ITEMS.value,
                message = FEW_ITEMS,
            )

            violationsOf(input.refused()) shouldBe listOf(fewer)
        }

        test("check an object, a query string, a field and parts after their own validation") {
            val deep = ParameterFormat("filter", ParameterStyle.DeepObject, isExploded = true)
            val input = request("filter%5Bcolor%5D=red&filter%5Bsize%5D=12")
            val small: ValidationScope.(Filter) -> Unit = { filter ->
                if (filter.size > MOST) fail(TOO_MANY, ConstraintViolationCodes.MAXIMUM)
            }
            val parts = listOf(
                part(name = NOTE, text = "a"),
                part(name = TAG, text = "12"),
                part(name = TAG, text = "3"),
            )

            input.fields(
                place = Place.Query,
                format = deep,
                deserializer = Filter.serializer(),
                isRequired = true,
                rule = small,
            )
            input.field(name = "count", text = "12", deserializer = Int.serializer(), isRequired = true, rule = most)
            input.part(parts = parts, name = NOTE, deserializer = String.serializer(), isRequired = true, rule = long)
            input.partItems(parts = parts, name = TAG, deserializer = Int.serializer(), rule = two)
            input.partItems(parts = parts, name = "none", deserializer = Int.serializer(), rule = two)

            violationsOf(input.refused()).map(ProblemViolation::path) shouldBe
                listOf("query.filter", "body.count", "body.note", "body.none")
        }

        test("report a violation of a validated value at the root under the path itself") {
            val input = request("")
            val note = part(name = NOTE, text = """{"text":" "}""")
            val blank: ValidationScope.(Note) -> Unit = { _ ->
                fail("is blank as a whole", ConstraintViolationCodes.CONDITION)
            }

            input.part(
                parts = listOf(note),
                name = NOTE,
                deserializer = Note.serializer(),
                isRequired = true,
                mediaType = MediaType.Json,
                rule = blank,
            )

            violationsOf(input.refused()).map(ProblemViolation::path) shouldBe listOf("body.note.text", "body.note")
        }
    }

    context("a problem of the rules") {
        test("answers with the status of an invalid request") {
            val input = request("limit=12")

            input.value(place = Place.Query, format = form, type = number, isRequired = true, rule = most)

            input.refused().status shouldBe Problem.INVALID_STATUS
        }
    }
})
