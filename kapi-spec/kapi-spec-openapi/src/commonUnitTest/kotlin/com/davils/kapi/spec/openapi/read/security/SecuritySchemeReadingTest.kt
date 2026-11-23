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
import com.davils.kapi.spec.openapi.Document
import com.davils.kapi.spec.openapi.OpenApi
import com.davils.kapi.spec.openapi.fixture.codes
import com.davils.kapi.spec.openapi.fixture.document
import com.davils.kapi.spec.openapi.fixture.parts
import com.davils.kapi.spec.openapi.fixture.readable
import com.davils.kapi.spec.openapi.fixture.reading31
import com.davils.kapi.spec.openapi.fixture.reading32
import com.davils.kapi.spec.openapi.fixture.rewritten
import com.davils.kapi.spec.openapi.model.security.OAuth2Scheme
import com.davils.kapi.spec.openapi.model.security.SecurityScheme
import com.davils.kapi.spec.openapi.resolve
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

private const val DEVICE: String = "{deviceAuthorizationUrl: 'https://id.example.com/device', " +
    "tokenUrl: 'https://id.example.com/token', scopes: {read: Read}}"

private val schemes: List<String> = listOf(
    "components:",
    "  securitySchemes:",
    "    device:",
    "      type: oauth2",
    "      deprecated: true",
    "      oauth2MetadataUrl: 'https://id.example.com/.well-known/oauth-authorization-server'",
    "      flows: {deviceAuthorization: $DEVICE}",
    "    key: {type: apiKey, name: key, in: header, deprecated: true}",
    "    basic: {type: http, scheme: basic, deprecated: false}",
    "    tls: {type: mutualTLS, deprecated: true}",
    "    oidc: {type: openIdConnect, openIdConnectUrl: 'https://id.example.com', deprecated: true}",
)

private fun Document.scheme(name: String): SecurityScheme {
    val declared = parts().securitySchemes.getValue(name)
    return resolve(declared).shouldNotBeNull()
}

class SecuritySchemeReadingTest : FunSpec({
    context("a security scheme of OpenAPI 3.2") {
        test("may be deprecated") {
            val reading = reading32(schemes)
            val document = reading.document()
            val deprecated = listOf("device", "key", "basic", "tls", "oidc").map { name ->
                document.scheme(name).isDeprecated
            }

            reading.diagnostics.shouldBeEmpty()
            deprecated shouldBe listOf(true, true, false, true, true)
        }

        test("reads the metadata address and the device authorization flow of oauth2") {
            val device = reading32(schemes).document().scheme("device").shouldBeInstanceOf<OAuth2Scheme>()
            val flow = device.flows.deviceAuthorization.shouldNotBeNull()

            device.oauth2MetadataUrl shouldBe "https://id.example.com/.well-known/oauth-authorization-server"
            flow.deviceAuthorizationUrl shouldBe "https://id.example.com/device"
            flow.tokenUrl shouldBe "https://id.example.com/token"
            flow.scopes shouldBe mapOf("read" to "Read")
        }

        test("is written with them and reads back") {
            val document = reading32(schemes).document()
            val written = OpenApi.write(document)

            written shouldContain "deviceAuthorizationUrl:"
            written shouldContain "oauth2MetadataUrl:"
            written shouldContain "deprecated: true"
            document.rewritten() shouldBe document
        }

        test("needs both addresses for the device authorization flow") {
            val flow = "      flows: {deviceAuthorization: {scopes: {}}}"
            val reading = reading32(schemes.take(4) + flow)

            reading.codes() shouldBe List(2) { DiagnosticCodes.FIELD_MISSING }
        }

        test("knows the device authorization address only in the device authorization flow") {
            val flow = "      flows: {clientCredentials: {deviceAuthorizationUrl: x, tokenUrl: y, scopes: {}}}"
            val reading = reading32(schemes.take(4) + flow)

            reading.codes() shouldBe listOf(DiagnosticCodes.FIELD_UNKNOWN)
        }
    }

    context("a security scheme before OpenAPI 3.2") {
        test("is never deprecated and has no metadata address or device authorization flow") {
            val reading = reading31(schemes)
            val device = reading.readable().scheme("device").shouldBeInstanceOf<OAuth2Scheme>()

            reading.codes().toSet() shouldBe setOf(DiagnosticCodes.FIELD_VERSION)
            reading.codes().size shouldBe 7
            device.isDeprecated shouldBe false
            device.oauth2MetadataUrl.shouldBeNull()
            device.flows.deviceAuthorization.shouldBeNull()
        }
    }
})
