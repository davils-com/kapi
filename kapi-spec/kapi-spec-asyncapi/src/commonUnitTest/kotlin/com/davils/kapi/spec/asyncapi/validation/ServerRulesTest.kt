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

package com.davils.kapi.spec.asyncapi.validation

import com.davils.kapi.spec.asyncapi.DiagnosticCodes
import com.davils.kapi.spec.asyncapi.fixture.codes
import com.davils.kapi.spec.asyncapi.fixture.readingOf
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

class ServerRulesTest : FunSpec({
    context("a server") {
        test("declares every variable its host and its path name") {
            val reading = readingOf(
                """
                    |servers:
                    |  production:
                    |    host: '{region}.example.com'
                    |    pathname: /{stage}/{region}
                    |    protocol: ws
                    |    variables:
                    |      region:
                    |        default: eu
                """.trimMargin(),
            )

            reading.codes() shouldBe listOf(DiagnosticCodes.SERVER_VARIABLE_UNDECLARED)
        }

        test("writes its host as a valid template") {
            val reading =
                readingOf(
                    """
                        |servers:
                        |  production:
                        |    host: '{region.example.com'
                        |    protocol: ws
                    """.trimMargin(),
                )

            reading.codes() shouldBe listOf(DiagnosticCodes.SERVER_TEMPLATE)
        }

        test("has variables whose default is one of the values they allow") {
            val reading = readingOf(
                """
                    |servers:
                    |  production:
                    |    host: '{region}.example.com'
                    |    protocol: ws
                    |    variables:
                    |      region:
                    |        enum: [eu, us]
                    |        default: ap
                    |  other:
                    |    host: '{region}.example.com'
                    |    protocol: ws
                    |    variables:
                    |      region:
                    |        enum: [eu, us]
                """.trimMargin(),
            )

            reading.codes() shouldBe listOf(DiagnosticCodes.SERVER_VARIABLE_DEFAULT)
        }

        test("needs its host and its protocol") {
            readingOf("servers:\n  production:\n    pathname: /").codes() shouldBe
                List(2) { DiagnosticCodes.FIELD_MISSING }
        }

        test("of AsyncAPI 2 with a url is reported") {
            val reading =
                readingOf(
                    """
                        |servers:
                        |  production:
                        |    host: a
                        |    protocol: ws
                        |    url: ws://a
                    """.trimMargin(),
                )

            reading.codes() shouldBe listOf(DiagnosticCodes.FIELD_VERSION)
        }

        test("may name a variable from the components") {
            val reading = readingOf(
                """
                    |servers:
                    |  production:
                    |    host: '{region}.example.com'
                    |    protocol: ws
                    |    variables:
                    |      region:
                    |        ${'$'}ref: '#/components/serverVariables/region'
                    |components:
                    |  serverVariables:
                    |    region:
                    |      enum: [eu]
                    |      default: eu
                """.trimMargin(),
            )

            reading.diagnostics.shouldBeEmpty()
        }
    }
})
