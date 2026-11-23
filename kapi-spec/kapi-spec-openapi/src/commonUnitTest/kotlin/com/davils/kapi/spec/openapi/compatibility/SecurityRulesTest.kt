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

package com.davils.kapi.spec.openapi.compatibility

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

private val schemes: String = """
    |components:
    |  securitySchemes:
    |    key: {type: apiKey, name: X-Key, in: header}
    |    bearer: {type: http, scheme: bearer}
    |    oauth:
    |      type: oauth2
    |      flows:
    |        clientCredentials:
    |          tokenUrl: https://example.com/token
    |          scopes: {read: Read, write: Write}
    |    oidc: {type: openIdConnect, openIdConnectUrl: https://example.com/.well-known/openid-configuration}
    |    tls: {type: mutualTLS}
""".trimMargin()

private fun secured(security: String, components: String = schemes): String = """
    |paths:
    |  /pets:
    |    get:
    |      security: $security
    |      responses:
    |        '200': {description: OK}
""".trimMargin() + "\n" + components

class SecurityRulesTest : FunSpec({
    context("the security of an operation") {
        test("that is added breaks clients and that is removed breaks providers") {
            (secured("[]") to secured("[{key: []}]")).rules() shouldBe listOf("security-added")
            (secured("[{key: []}]") to secured("[{}]")).rules() shouldBe listOf("security-removed")
        }

        test("reports removed and added alternatives") {
            (secured("[{key: []}, {bearer: []}]") to secured("[{key: []}, {tls: []}]")).rules() shouldBe
                listOf("security-requirement-removed", "security-requirement-added")
            (secured("[{key: [], bearer: []}]") to secured("[{bearer: [], key: []}]")).rules().shouldBeEmpty()
        }

        test("reports needed and dropped scopes") {
            val read = secured("[{oauth: [read]}]")
            (read to secured("[{oauth: [read, write]}]")).rules() shouldBe listOf("security-scope-added")
            (secured("[{oauth: [read, write]}]") to secured("[{oauth: [write]}]")).rules() shouldBe
                listOf("security-scope-removed")
        }
    }

    context("the security schemes of the components") {
        test("break both sides when they carry credentials differently") {
            val renamed = schemes.replace(oldValue = "name: X-Key", newValue = "name: X-Api-Key")
            val basic = renamed.replace(oldValue = "scheme: bearer", newValue = "scheme: basic")
            val moved = basic.replace(oldValue = "com/token", newValue = "com/oauth/token")
            val changed = moved.replace(oldValue = "example.com/.well-known", newValue = "example.org/.well-known")

            val rules = (secured("[]") to secured(security = "[]", components = changed)).rules()

            rules shouldBe List(SCHEMES) { "security-scheme-changed" }
            val described = schemes.replace(oldValue = "Read", newValue = "Read all")
            (secured("[]") to secured(security = "[]", components = described)).rules().shouldBeEmpty()
        }
    }
})

private const val SCHEMES: Int = 4
