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

package com.davils.kapi.spec.asyncapi.read

import com.davils.kapi.spec.asyncapi.DiagnosticCodes
import com.davils.kapi.spec.asyncapi.fixture.codes
import com.davils.kapi.spec.asyncapi.fixture.parts
import com.davils.kapi.spec.asyncapi.fixture.readingOf
import com.davils.kapi.spec.asyncapi.model.security.OAuth2Scheme
import com.davils.kapi.spec.core.model.Referable
import com.davils.kore.core.validation.violation.ViolationCode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

private suspend fun codesOf(scheme: String): List<ViolationCode> {
    val indented = scheme.lines().map { line -> "      $line" }
    return readingOf(listOf("components:", "  securitySchemes:", "    scheme:") + indented).codes()
}

class SecurityReadingTest : FunSpec({
    context("a security scheme") {
        test("needs a type") {
            codesOf("description: No type") shouldBe listOf(DiagnosticCodes.SECURITY_TYPE)
        }

        test("needs a type AsyncAPI defines") {
            val reading =
                readingOf(
                    """
                        |components:
                        |  securitySchemes:
                        |    scheme:
                        |      type: mutualTLS
                    """.trimMargin(),
                )

            reading.codes() shouldBe listOf(DiagnosticCodes.SECURITY_TYPE)
            reading.diagnostics.single().message shouldContain "userPassword, apiKey, X509"
            reading.parts().securitySchemes.isEmpty() shouldBe true
        }

        test("must be an object") {
            codesOf("[apiKey]") shouldBe listOf(DiagnosticCodes.FIELD_TYPE)
        }

        test("of an api key is sent as the user or the password") {
            codesOf("type: apiKey\nin: header") shouldBe listOf(DiagnosticCodes.SECURITY_LOCATION)
            codesOf("type: apiKey") shouldBe listOf(DiagnosticCodes.FIELD_MISSING)
        }

        test("of an HTTP api key names its parameter and is sent in a query, a header or a cookie") {
            codesOf("type: httpApiKey\nname: key\nin: user") shouldBe listOf(DiagnosticCodes.SECURITY_LOCATION)
            codesOf("type: httpApiKey\nin: cookie") shouldBe listOf(DiagnosticCodes.FIELD_MISSING)
        }

        test("of HTTP names its scheme") {
            codesOf("type: http") shouldBe listOf(DiagnosticCodes.FIELD_MISSING)
        }

        test("of OpenID Connect names its configuration") {
            codesOf("type: openIdConnect") shouldBe listOf(DiagnosticCodes.FIELD_MISSING)
        }

        test("of OAuth 2.0 needs its flows, each with the fields of its kind") {
            codesOf("type: oauth2") shouldBe listOf(DiagnosticCodes.FIELD_MISSING)
            val flows =
                """
                    |type: oauth2
                    |flows:
                    |  implicit:
                    |    availableScopes: {}
                    |  authorizationCode:
                    |    authorizationUrl: https://example.com/authorize
                    |    tokenUrl: https://example.com/token
                    |    scopes: {}
                    |    availableScopes: {}
                """.trimMargin()

            codesOf(flows) shouldBe listOf(
                DiagnosticCodes.STRUCTURE_SCHEMA,
                DiagnosticCodes.FIELD_MISSING,
                DiagnosticCodes.FIELD_VERSION,
            )
        }

        test("without fields of its own takes no fields of other types") {
            codesOf("type: plain\nname: user") shouldBe listOf(DiagnosticCodes.FIELD_UNKNOWN)
        }

        test("of OAuth 2.0 keeps its flows empty when they are no object") {
            val reading =
                readingOf(
                    """
                        |components:
                        |  securitySchemes:
                        |    oauth:
                        |      type: oauth2
                        |      flows: []
                    """.trimMargin(),
                )
            val scheme = reading.parts().securitySchemes.getValue("oauth")

            reading.codes() shouldBe listOf(DiagnosticCodes.FIELD_MISSING, DiagnosticCodes.FIELD_TYPE)
            scheme.shouldBeInstanceOf<Referable.Inline<OAuth2Scheme>>()
        }
    }
})
