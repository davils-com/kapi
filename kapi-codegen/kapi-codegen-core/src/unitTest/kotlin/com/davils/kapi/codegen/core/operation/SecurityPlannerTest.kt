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


package com.davils.kapi.codegen.core.operation

import com.davils.kapi.codegen.core.CodegenCodes
import com.davils.kapi.codegen.core.fixture.LIST_PETS
import com.davils.kapi.codegen.core.fixture.clientGenerationOf
import com.davils.kapi.codegen.core.fixture.clientOf
import com.davils.kapi.codegen.core.fixture.codes
import com.davils.kapi.codegen.core.fixture.get
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

private const val USE: String = "SecurityUse"

private const val PLACEMENT: String = "CredentialPlacement"

private const val BEARER: String = "token: {type: http, scheme: bearer}"

private const val KEY: String = "key: {type: apiKey, name: X-Api-Key, in: header}"

private const val BEARER_PLACEMENT: String = "$PLACEMENT.Bearer"

private const val REQUIRED: String = "security: [{token: []}]"

private const val TOKEN_USE: String = "$USE(\"token\", $BEARER_PLACEMENT)"

private const val KEY_USE: String = """$USE("key", $PLACEMENT.Header("X-Api-Key"))"""

private const val FLOWS: String = "flows: {clientCredentials: {tokenUrl: 'https://example.com/token', scopes: {}}}"

private const val DISCOVERY: String = "openIdConnectUrl: 'https://example.com/.well-known/openid-configuration'"

private val schemes: Map<String, String> = mapOf(
    BEARER to BEARER_PLACEMENT,
    "token: {type: http, scheme: BEARER}" to BEARER_PLACEMENT,
    "token: {type: http, scheme: basic}" to "$PLACEMENT.Basic",
    "token: {type: apiKey, name: X-Api-Key, in: header}" to "$PLACEMENT.Header(\"X-Api-Key\")",
    "token: {type: apiKey, name: api_key, in: query}" to "$PLACEMENT.Query(\"api_key\")",
    "token: {type: apiKey, name: sid, in: cookie}" to "$PLACEMENT.Cookie(\"sid\")",
    "token: {type: oauth2, $FLOWS}" to BEARER_PLACEMENT,
    "token: {type: openIdConnect, $DISCOVERY}" to BEARER_PLACEMENT,
)

private fun secured(schemes: String, security: String, vararg document: String): List<String> =
    document.toList() + get(LIST_PETS, security) + listOf("components:", "  securitySchemes: {$schemes}")

class SecurityPlannerTest : FunSpec({
    context("the scheme an operation requires") {
        schemes.forEach { (scheme, placement) ->
            test("is placed as the document declares it: $scheme") {
                val kotlin = clientOf(secured(scheme, REQUIRED))

                kotlin shouldContain """            security(listOf($USE("token", $placement)))"""
            }
        }

        test("is refused when the generator has no way to place it") {
            val lines = secured("token: {type: http, scheme: digest}", REQUIRED)

            clientGenerationOf(lines).codes() shouldBe listOf(CodegenCodes.OPERATION_UNSUPPORTED)
        }

        test("adds nothing to the call when it is the certificate of the connection") {
            val kotlin = clientOf(secured("token: {type: mutualTLS}", REQUIRED))

            kotlin shouldContain "            security(listOf())\n"
        }

        test("leaves the certificate of the connection out and places the schemes required with it") {
            val kotlin = clientOf(secured("tls: {type: mutualTLS}, $BEARER", "security: [{tls: [], token: []}]"))

            kotlin shouldContain "            security(listOf($TOKEN_USE))\n"
        }
    }

    context("the requirements of an operation") {
        test("are those of the document when it names none") {
            val kotlin = clientOf(secured(BEARER, "tags: [pets]", REQUIRED), "PetstorePetsClient")

            kotlin shouldContain "security(listOf($TOKEN_USE))"
        }

        test("replace those of the document") {
            val kotlin = clientOf(secured("$BEARER, $KEY", "security: [{key: []}]", REQUIRED))

            kotlin shouldContain "security(listOf($KEY_USE))"
            kotlin shouldNotContain TOKEN_USE
        }

        test("are none when the operation says so") {
            val kotlin = clientOf(secured(BEARER, "security: []", REQUIRED))

            kotlin shouldNotContain "security("
        }

        test("are alternatives in the order of the document, each with every scheme it needs together") {
            val kotlin = clientOf(secured("$BEARER, $KEY", "security: [{token: [], key: []}, {key: []}, {}]"))

            kotlin shouldContain listOf(
                "            security(listOf($TOKEN_USE, $KEY_USE))",
                "            security(listOf($KEY_USE))",
                "            security(listOf())",
            ).joinToString("\n")
        }
    }
})
