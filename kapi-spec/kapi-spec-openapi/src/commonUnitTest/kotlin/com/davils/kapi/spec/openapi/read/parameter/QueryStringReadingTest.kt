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

package com.davils.kapi.spec.openapi.read.parameter

import com.davils.kapi.spec.openapi.DiagnosticCodes
import com.davils.kapi.spec.openapi.OpenApi
import com.davils.kapi.spec.openapi.fixture.codes
import com.davils.kapi.spec.openapi.fixture.document
import com.davils.kapi.spec.openapi.fixture.operation
import com.davils.kapi.spec.openapi.fixture.reading31
import com.davils.kapi.spec.openapi.fixture.reading32
import com.davils.kapi.spec.openapi.fixture.rewritten
import com.davils.kapi.spec.openapi.Document
import com.davils.kapi.spec.openapi.model.parameter.Parameter
import com.davils.kapi.spec.openapi.model.parameter.ParameterPlacement
import com.davils.kapi.spec.openapi.model.parameter.ParameterStyle
import com.davils.kapi.spec.openapi.model.path.Method
import com.davils.kapi.spec.openapi.resolve
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

private const val PATHS: String = "paths:"

private const val FINE: String = "responses: {'200': {description: Fine}}"

private const val WHOLE: String = "{name: filter, in: querystring, content: {application/x-www-form-urlencoded: {}}}"

private const val COOKIE: String = "{name: session, in: cookie, style: cookie, schema: {type: object}}"

private fun operationWith(parameter: String): String = "  /pets: {get: {parameters: [$parameter], $FINE}}"

private fun Document.parameter(): Parameter {
    val declared = operation("/pets", Method.Get).parameters.single()
    return resolve(declared).shouldNotBeNull()
}

class QueryStringReadingTest : FunSpec({
    context("a parameter in querystring") {
        test("is read from OpenAPI 3.2 on") {
            val reading = reading32(PATHS, operationWith(WHOLE))
            val parameter = reading.document().parameter()

            reading.diagnostics.shouldBeEmpty()
            parameter.placement shouldBe ParameterPlacement.QueryString
            parameter.content.keys shouldBe setOf("application/x-www-form-urlencoded")
        }

        test("is no place of a parameter before OpenAPI 3.2") {
            val reading = reading31(PATHS, operationWith(WHOLE))

            reading.codes() shouldBe listOf(DiagnosticCodes.PARAMETER_LOCATION)
            reading.diagnostics.single().message shouldContain "path, query, header or cookie"
        }

        test("is written and reads back") {
            val document = reading32(PATHS, operationWith(WHOLE)).document()

            OpenApi.write(document) shouldContain "in: querystring"
            document.rewritten() shouldBe document
        }
    }

    context("the cookie style") {
        test("is read for a cookie parameter from OpenAPI 3.2 on") {
            val reading = reading32(PATHS, operationWith(COOKIE))
            val parameter = reading.document().parameter()

            reading.diagnostics.shouldBeEmpty()
            parameter.style shouldBe ParameterStyle.Cookie
        }

        test("is no style before OpenAPI 3.2") {
            val reading = reading31(PATHS, operationWith(COOKIE))

            reading.codes() shouldBe listOf(DiagnosticCodes.PARAMETER_STYLE)
        }

        test("is refused outside a cookie") {
            val query = "{name: session, in: query, style: cookie, schema: {type: object}}"
            val reading = reading32(PATHS, operationWith(query))

            reading.codes() shouldBe listOf(DiagnosticCodes.PARAMETER_STYLE)
            reading.diagnostics.single().message shouldContain "in query"
        }
    }
})
