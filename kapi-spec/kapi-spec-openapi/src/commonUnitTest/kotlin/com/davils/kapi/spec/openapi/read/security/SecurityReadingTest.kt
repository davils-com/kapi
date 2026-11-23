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

package com.davils.kapi.spec.openapi.read.security

import com.davils.kapi.spec.openapi.DiagnosticCodes
import com.davils.kapi.spec.openapi.Reading
import com.davils.kapi.spec.openapi.fixture.codes
import com.davils.kapi.spec.openapi.fixture.partial
import com.davils.kapi.spec.openapi.fixture.reading30
import com.davils.kapi.spec.openapi.fixture.reading31
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

private val head: List<String> = listOf("components:", "  securitySchemes:", "    subject:")

private val missing = DiagnosticCodes.FIELD_MISSING

private suspend fun scheme(vararg lines: String): Reading = reading31(head + lines.map { line -> "      $line" })

class SecurityReadingTest : FunSpec({
    context("a security scheme") {
        test("needs a type the reader knows") {
            scheme("type: magic").codes() shouldBe listOf(DiagnosticCodes.SECURITY_TYPE)
            scheme("description: None").codes() shouldBe listOf(DiagnosticCodes.SECURITY_TYPE)
            scheme("type: 1").codes() shouldBe listOf(DiagnosticCodes.SECURITY_TYPE)
        }

        test("is left out of the document when its type is unknown") {
            val reading = scheme("type: magic")

            reading.partial().components?.securitySchemes shouldBe emptyMap()
        }

        test("cannot be mutual tls in OpenAPI 3.0") {
            val reading = reading30(head + "      type: mutualTLS")

            reading.codes() shouldBe listOf(DiagnosticCodes.SECURITY_TYPE)
        }

        test("needs a name and a place when it is an api key") {
            scheme("type: apiKey").codes() shouldBe listOf(missing, DiagnosticCodes.SECURITY_LOCATION)
            scheme("type: apiKey", "name: X-Key", "in: body").codes() shouldBe
                listOf(DiagnosticCodes.SECURITY_LOCATION)
        }

        test("needs a scheme when it is http") {
            val reading = scheme("type: http")

            reading.codes() shouldBe listOf(missing)
            reading.diagnostics.single().message shouldContain "needs the field scheme"
        }

        test("needs its flows when it is oauth2") {
            scheme("type: oauth2").codes() shouldBe listOf(missing)
        }

        test("needs its address when it is openid connect") {
            scheme("type: openIdConnect").codes() shouldBe listOf(missing)
        }
    }

    context("a security requirement") {
        test("is an object of schemes and their scopes") {
            reading31("security:", "  - bearer").codes() shouldBe listOf(DiagnosticCodes.FIELD_TYPE)
            reading31("security:", "  - bearer: read").codes() shouldContain DiagnosticCodes.FIELD_TYPE
        }
    }
})
