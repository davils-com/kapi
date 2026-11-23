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

package com.davils.kapi.spec.openapi.upgrade

import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.get
import com.davils.kapi.spec.openapi.upgrade.fixture.nodeOf
import com.davils.kapi.spec.openapi.upgrade.fixture.upgraded
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private suspend fun schemeOf(scheme: String): YamlNode? =
    upgraded("securityDefinitions: {subject: $scheme}").root["components"]["securitySchemes"]["subject"]

class SecurityUpgradeTest : FunSpec({
    context("a security definition") {
        test("becomes the basic scheme of HTTP when it is basic") {
            schemeOf("{type: basic, description: Login, x-realm: kide}") shouldBe
                nodeOf("{type: http, scheme: basic, description: Login, x-realm: kide}")
        }

        test("stays as written when it is an API key or of a type Swagger does not know") {
            schemeOf(
                "{type: apiKey, name: X-Key, in: header}",
            ) shouldBe nodeOf("{type: apiKey, name: X-Key, in: header}")
            schemeOf("{type: unknown}") shouldBe nodeOf("{type: unknown}")
        }

        test("moves the one flow of OAuth2 into the flows under its OpenAPI name") {
            val flows = mapOf(
                "implicit" to "implicit: {authorizationUrl: 'https://a', scopes: {}}",
                "password" to "password: {tokenUrl: 'https://t', scopes: {}}",
                "application" to "clientCredentials: {tokenUrl: 'https://t', scopes: {}}",
                "accessCode" to "authorizationCode: {authorizationUrl: 'https://a', tokenUrl: 'https://t', scopes: {}}",
            )
            flows.forEach { (flow, upgraded) ->
                val settings = upgraded.substringAfter(": ").removeSurrounding(prefix = "{", suffix = "}")

                schemeOf("{type: oauth2, flow: $flow, description: Tokens, x-vendor: kide, $settings}") shouldBe
                    nodeOf("{type: oauth2, description: Tokens, x-vendor: kide, flows: {$upgraded}}")
            }
        }

        test("stays as written when its flow is unknown") {
            schemeOf("{type: oauth2, flow: device}") shouldBe nodeOf("{type: oauth2, flow: device}")
        }

        test("stays as written when it names no flow or no type, or not as text") {
            schemeOf("{type: oauth2, description: Tokens}") shouldBe nodeOf("{type: oauth2, description: Tokens}")
            schemeOf("{type: oauth2, flow: [implicit]}") shouldBe nodeOf("{type: oauth2, flow: [implicit]}")
            schemeOf("{description: Nothing}") shouldBe nodeOf("{description: Nothing}")
            schemeOf("{type: [basic]}") shouldBe nodeOf("{type: [basic]}")
        }
    }
})
