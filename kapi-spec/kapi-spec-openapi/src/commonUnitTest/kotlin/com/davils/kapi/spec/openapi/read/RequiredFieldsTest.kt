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

package com.davils.kapi.spec.openapi.read

import com.davils.kapi.spec.openapi.DiagnosticCodes
import com.davils.kapi.spec.openapi.fixture.codes
import com.davils.kapi.spec.openapi.fixture.operation
import com.davils.kapi.spec.openapi.fixture.partial
import com.davils.kapi.spec.openapi.fixture.reading
import com.davils.kapi.spec.openapi.fixture.reading31
import com.davils.kapi.spec.openapi.fixture.schema
import com.davils.kapi.spec.openapi.model.path.Method
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

private const val MISSING_FIELDS: Int = 9

private val missing = DiagnosticCodes.FIELD_MISSING

private val incomplete: List<String> = listOf(
    "servers:",
    "  - description: No address",
    "    variables:",
    "      region:",
    "        description: No default",
    "tags:",
    "  - description: No name",
    "    externalDocs:",
    "      description: No address",
    "paths:",
    "  /pets:",
    "    get:",
    "      parameters:",
    "        - description: No name and no place",
    "          schema: {}",
    "      responses:",
    "        '200':",
    "          content: {}",
    "components:",
    "  schemas:",
    "    Pet:",
    "      discriminator:",
    "        mapping: {}",
    "  requestBodies:",
    "    Pet:",
    "      description: No content",
)

class RequiredFieldsTest : FunSpec({
    context("an object that lacks what it needs") {
        test("is reported once for every field and read with what it has") {
            val reading = reading31(incomplete)
            val document = reading.partial()
            val server = document.servers.single()
            val tag = document.tags.single()

            reading.codes() shouldBe List(MISSING_FIELDS) { missing }
            server.url shouldBe ""
            server.variables.getValue("region").default shouldBe ""
            tag.name shouldBe ""
            tag.externalDocs?.url shouldBe ""
            document.schema("Pet").discriminator?.propertyName shouldBe ""
        }

        test("keeps the parameter and the response that lack a field") {
            val reading = reading31(incomplete)
            val get = reading.partial().operation("/pets", Method.Get)

            get.parameters shouldHaveSize 1
            get.responses.entries.size shouldBe 1
        }

        test("is reported for a license without a name") {
            val license = listOf("  license:", "    url: https://example.com")
            val info = listOf("openapi: 3.1.0", "paths: {}", "info:", "  title: a", "  version: b")
            val nameless = reading(info + license)
            val read = nameless.partial().info

            nameless.codes() shouldBe listOf(missing)
            read.license?.name shouldBe ""
        }
    }
})
