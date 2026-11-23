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
import com.davils.kapi.spec.asyncapi.StructureCheck
import com.davils.kapi.spec.asyncapi.fixture.codes
import com.davils.kapi.spec.asyncapi.fixture.readingOf
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

private const val PLACEMENTS: Int = 5

private val schemes: String = """
    |components:
    |  securitySchemes:
    |    user:
    |      type: userPassword
    |  operationTraits:
    |    secured:
    |      security:
    |        - ${'$'}ref: '#/components/schemes/user'
    |  schemes:
    |    user:
    |      type: userPassword
""".trimMargin()

class SecurityRulesTest : FunSpec({
    context("a security requirement") {
        test("may refer to a security scheme of components/securitySchemes or hold one inline") {
            val reading = readingOf(
                """
                    |servers:
                    |  broker:
                    |    host: broker.example.com
                    |    protocol: mqtt
                    |    security:
                    |      - ${'$'}ref: '#/components/securitySchemes/user'
                    |      - type: userPassword
                    |components:
                    |  securitySchemes:
                    |    user:
                    |      type: userPassword
                """.trimMargin(),
            ) { structure = StructureCheck.Reader }

            reading.diagnostics.shouldBeEmpty()
        }

        test("of a server, an operation or a trait refers to nothing else than a security scheme of the components") {
            val reading = readingOf(
                """
                    |servers:
                    |  broker:
                    |    host: broker.example.com
                    |    protocol: mqtt
                    |    security:
                    |      - ${'$'}ref: '#/components/schemes/user'
                    |      - ${'$'}ref: '#/x-vendor/securitySchemes/user'
                    |      - ${'$'}ref: '#/x-user'
                    |x-vendor:
                    |  securitySchemes:
                    |    user:
                    |      type: userPassword
                    |x-user:
                    |  type: userPassword
                    |channels:
                    |  chat:
                    |    address: chat
                    |operations:
                    |  send:
                    |    action: send
                    |    channel:
                    |      ${'$'}ref: '#/channels/chat'
                    |    security:
                    |      - ${'$'}ref: '#/components/schemes/user'
                """.trimMargin() + "\n" + schemes,
            ) { structure = StructureCheck.Reader }

            val codes = reading.codes().filter { code -> code == DiagnosticCodes.SECURITY_REFERENCE_PLACEMENT }
            codes.size shouldBe PLACEMENTS
            val diagnostic = reading.diagnostics.first()
            val first = diagnostic.location.shouldNotBeNull()
            first.pointer.text shouldBe "/servers/broker/security/0"
        }
    }
})
