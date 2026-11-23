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
import com.davils.kapi.spec.openapi.fixture.document
import com.davils.kapi.spec.openapi.fixture.reading30
import com.davils.kapi.spec.openapi.fixture.reading31
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

private const val COMPONENTS: String = "components:"

private const val PARAMETERS: String = "  parameters:"

private const val SCHEMES: String = "  securitySchemes:"

private const val BEARER: String = "    bearer: {type: http, scheme: bearer}"

private const val FLOWS: String = "{password: {tokenUrl: https://example.com, scopes: {}}}"

private const val OAUTH: String = "    oauth: {type: oauth2, flows: $FLOWS}"

private const val TEXT: String = "{type: string}"

private const val LIMIT: String = "{name: limit, in: query, schema: $TEXT}"

private val shape = DiagnosticCodes.PARAMETER_SHAPE

class ParameterAndSecurityRulesTest : FunSpec({
    context("a parameter") {
        test("has either a schema or a content with one media type") {
            val neither = "    Neither: {name: a, in: query}"
            val both = "    Both: {name: b, in: query, schema: $TEXT, content: {text/plain: {}}}"
            val several = "    Several: {name: c, in: query, content: {text/plain: {}, text/html: {}}}"
            val content = "    Content: {name: d, in: query, content: {text/plain: {}}}"

            val reading = reading31(COMPONENTS, PARAMETERS, neither, both, several, content)

            reading.codes() shouldBe listOf(shape, shape, shape)
        }

        test("uses a style its place allows") {
            val odd = "    Odd: {name: a, in: query, style: matrix, schema: $TEXT}"
            val reading = reading31(COMPONENTS, PARAMETERS, odd)

            reading.codes() shouldBe listOf(DiagnosticCodes.PARAMETER_STYLE)
        }

        test("should say that it is required when it is part of the path") {
            val reading = reading31(COMPONENTS, PARAMETERS, "    Id: {name: id, in: path, schema: $TEXT}")

            reading.codes() shouldBe listOf(DiagnosticCodes.PARAMETER_PATH_OPTIONAL)
            reading.document()
        }

        test("is declared once in a list, by its name and its place") {
            val header = "{name: limit, in: header, schema: $TEXT}"
            val responses = "responses: {'200': {description: Fine}}"
            val get = "get: {parameters: [$LIMIT, $header, $header], $responses}"
            val item = "  /pets: {parameters: [$LIMIT, $LIMIT], $get}"
            val duplicate = DiagnosticCodes.PARAMETER_DUPLICATE

            reading31("paths:", item).codes() shouldBe listOf(duplicate, duplicate)
        }
    }

    context("a security requirement") {
        test("names schemes the components declare") {
            val reading = reading31("security: [{basic: []}]", COMPONENTS, SCHEMES, BEARER)

            reading.codes() shouldBe listOf(DiagnosticCodes.SECURITY_UNKNOWN_SCHEME)
        }

        test("lists scopes only for schemes that have scopes in OpenAPI 3.0") {
            val reading = reading30("security: [{bearer: [read], oauth: [read]}]", COMPONENTS, SCHEMES, BEARER, OAUTH)

            reading.codes() shouldBe listOf(DiagnosticCodes.SECURITY_SCOPES)
        }

        test("may list roles for any scheme in OpenAPI 3.1") {
            val reading = reading31("security: [{bearer: [admin]}, {}]", COMPONENTS, SCHEMES, BEARER)

            reading.diagnostics.shouldBeEmpty()
        }
    }
})
