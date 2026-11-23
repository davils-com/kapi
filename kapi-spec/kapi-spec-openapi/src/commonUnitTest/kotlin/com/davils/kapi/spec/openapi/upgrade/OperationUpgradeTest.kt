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
import com.davils.kaml.core.node.string
import com.davils.kapi.spec.openapi.upgrade.fixture.Upgraded
import com.davils.kapi.spec.openapi.upgrade.fixture.upgraded
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

private const val PATHS: String = "paths:"

private const val PETS: String = "  /pets:"

private val Upgraded.post: YamlNode? get() = root["paths"]["/pets"]["post"]

class OperationUpgradeTest : FunSpec({
    context("an operation") {
        test("has no responses when it lists none") {
            val upgrade = upgraded(PATHS, PETS, "    post: {operationId: create}")

            upgrade.post["operationId"].string() shouldBe "create"
            upgrade.post["responses"].shouldBeNull()
        }

        test("takes its own body over the body of its path") {
            val upgrade = upgraded(
                PATHS,
                PETS,
                "    parameters: [{name: shared, in: body, schema: {type: string}}]",
                "    post:",
                "      parameters: [{name: own, in: body, schema: {type: integer}}]",
                "      responses: {}",
            )
            val schema = upgrade.post["requestBody"]["content"]["application/json"]["schema"]

            schema["type"].string() shouldBe "integer"
        }

        test("inherits the body of its path when it has none") {
            val upgrade = upgraded(
                PATHS,
                PETS,
                "    parameters: [{name: shared, in: body, schema: {type: string}}]",
                "    post: {responses: {}}",
            )
            val schema = upgrade.post["requestBody"]["content"]["application/json"]["schema"]

            schema["type"].string() shouldBe "string"
        }

        test("keeps a header without a name as a parameter, which names no content type") {
            val upgrade = upgraded(
                PATHS,
                PETS,
                "    post:",
                "      parameters: [{in: header, type: string}]",
                "      responses: {}",
            )
            val parameters = upgrade.post["parameters"].shouldBeInstanceOf<YamlNode.Sequence>()

            parameters.items.size shouldBe 1
            val header = parameters.items.single()
            header["in"].string() shouldBe "header"
        }
    }
})
