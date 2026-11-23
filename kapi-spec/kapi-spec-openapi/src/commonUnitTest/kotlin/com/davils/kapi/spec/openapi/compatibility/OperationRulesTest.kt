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

import com.davils.kapi.spec.core.compatibility.Impact
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

private val listing: String = """
    |paths:
    |  /pets:
    |    get:
    |      operationId: listPets
    |      responses:
    |        '200': {description: The pets}
""".trimMargin()

class OperationRulesTest : FunSpec({
    context("the servers of a document") {
        test("are matched by their url") {
            val before = "servers:\n  - url: https://a.example.com\n  - url: https://b.example.com"
            val after = "servers:\n  - url: https://b.example.com\n  - url: https://c.example.com"

            (before to after).rules() shouldBe listOf("server-removed", "server-added")
            (before to after).changes().first().client shouldBe Impact.Breaking
        }
    }

    context("the paths of a document") {
        test("that are removed break clients and that are added break providers") {
            val before = "paths:\n  /pets: {}\n  /users: {}"
            val after = "paths:\n  /pets: {}\n  /owners: {}"
            val changes = (before to after).changes()

            changes.map(::ruleOf) shouldBe listOf("path-removed", "path-added")
            changes.map { change -> change.client to change.provider } shouldBe listOf(
                Impact.Breaking to Impact.NonBreaking,
                Impact.NonBreaking to Impact.Breaking,
            )
            changes.first().message shouldContain "\"/users\""
        }

        test("keep their shape when they only rename their parameters") {
            val before = pathWith(template = "/pets/{petId}", parameter = "petId")
            val after = pathWith(template = "/pets/{id}", parameter = "id")

            (before to after).rules() shouldBe listOf("path-parameter-renamed")
        }

        test("may be written as a reference") {
            val before = REFERENCED + PATH_ITEMS
            val after = REFERENCED + PATH_ITEMS + DEPRECATED

            (before to before).rules().shouldBeEmpty()
            (before to after).rules() shouldBe listOf("operation-deprecated")
        }
    }

    context("the operations of a path") {
        test("that are removed or added are reported with their method") {
            val after = listing.replace(oldValue = "    get:", newValue = "    put:")

            (listing to after).rules() shouldBe listOf("operation-removed", "operation-added")
            (listing to after).changes().first().message shouldContain "GET \"/pets\""
        }

        test("report a new operation id and a deprecation") {
            val after = listing.replace(oldValue = "operationId: listPets", newValue = RENAMED)

            (listing to after).rules() shouldBe listOf("operation-deprecated", "operation-id-changed")
            (listing to after).changes().first().client shouldBe Impact.Info
        }

        test("match additional operations by their method") {
            val before = additional("COPY")
            val after = additional("MOVE")

            (before to after).rules32() shouldBe listOf("operation-removed", "operation-added")
        }
    }
})

private val PATH_ITEMS: String = """
    |components:
    |  pathItems:
    |    Pets:
    |      get:
    |        responses: {'200': {description: OK}}
""".trimMargin()

private val REFERENCED: String = """
    |paths:
    |  /pets: {${'$'}ref: '#/components/pathItems/Pets'}
    |
""".trimMargin()

private val DEPRECATED: String = """
    |
    |        deprecated: true
""".trimMargin()

private val RENAMED: String = """
    |operationId: findPets
    |      deprecated: true
""".trimMargin()

private fun additional(method: String): String = """
    |paths:
    |  /pets:
    |    additionalOperations:
    |      $method:
    |        responses: {'200': {description: OK}}
""".trimMargin()

private fun pathWith(template: String, parameter: String): String = """
    |paths:
    |  $template:
    |    get:
    |      parameters:
    |        - {name: $parameter, in: path, required: true, schema: {type: string}}
    |      responses:
    |        '200': {description: The pet}
""".trimMargin()
