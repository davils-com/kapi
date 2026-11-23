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

package com.davils.kapi.spec.openapi.validation

import com.davils.kapi.spec.openapi.DiagnosticCodes
import com.davils.kapi.spec.openapi.fixture.codes
import com.davils.kapi.spec.openapi.fixture.reading32
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

private const val PATHS: String = "paths:"

private const val FINE: String = "responses: {'200': {description: Fine}}"

private const val FORM: String = "content: {application/x-www-form-urlencoded: {}}"

private const val WHOLE: String = "{name: filter, in: querystring, $FORM}"

private const val OTHER: String = "{name: search, in: querystring, $FORM}"

private const val QUERY: String = "{name: limit, in: query, schema: {type: integer}}"

private fun operationWith(vararg parameters: String): String =
    "  /pets: {get: {parameters: [${parameters.joinToString()}], $FINE}}"

class QueryStringRulesTest : FunSpec({
    context("a parameter in querystring") {
        test("describes the query string by the one media type of its content") {
            reading32(PATHS, operationWith(WHOLE)).diagnostics.shouldBeEmpty()
        }

        test("is an error without exactly one media type or with a schema or a serialization") {
            val shapes = listOf(
                "{name: filter, in: querystring}",
                "{name: filter, in: querystring, content: {text/plain: {}, application/json: {}}}",
                "{name: filter, in: querystring, schema: {type: string}}",
                "{name: filter, in: querystring, schema: {type: string}, $FORM}",
                "{name: filter, in: querystring, explode: false, $FORM}",
                "{name: filter, in: querystring, allowReserved: true, $FORM}",
            )
            shapes.forEach { parameter ->
                val reading = reading32(PATHS, operationWith(parameter))

                reading.codes() shouldBe listOf(DiagnosticCodes.PARAMETER_QUERYSTRING)
            }
        }

        test("has no style") {
            val reading = reading32(PATHS, operationWith("{name: filter, in: querystring, style: form, $FORM}"))

            reading.codes() shouldBe listOf(DiagnosticCodes.PARAMETER_STYLE)
        }

        test("is one per operation, counting those of the path item") {
            val own = reading32(PATHS, operationWith(WHOLE, OTHER))
            val shared = reading32(PATHS, "  /pets: {parameters: [$OTHER], get: {parameters: [$WHOLE], $FINE}}")
            val location = own.diagnostics.single().location.shouldNotBeNull()

            own.codes() shouldBe listOf(DiagnosticCodes.PARAMETER_QUERYSTRING_REPEATED)
            shared.codes() shouldBe listOf(DiagnosticCodes.PARAMETER_QUERYSTRING_REPEATED)
            location.pointer.toString() shouldBe "/paths/~1pets/get"
        }

        test("replaces the parameter of the path item of its name") {
            val reading = reading32(PATHS, "  /pets: {parameters: [$WHOLE], get: {parameters: [$WHOLE], $FINE}}")

            reading.diagnostics.shouldBeEmpty()
        }

        test("stands beside no parameter in query") {
            val own = reading32(PATHS, operationWith(WHOLE, QUERY))
            val shared = reading32(PATHS, "  /pets: {parameters: [$QUERY], get: {parameters: [$WHOLE], $FINE}}")
            val additional = reading32(
                PATHS,
                "  /pets: {additionalOperations: {SEARCH: {parameters: [$WHOLE, $QUERY], $FINE}}}",
            )

            own.codes() shouldBe listOf(DiagnosticCodes.PARAMETER_QUERYSTRING_WITH_QUERY)
            shared.codes() shouldBe listOf(DiagnosticCodes.PARAMETER_QUERYSTRING_WITH_QUERY)
            additional.codes() shouldBe listOf(DiagnosticCodes.PARAMETER_QUERYSTRING_WITH_QUERY)
        }
    }
})
