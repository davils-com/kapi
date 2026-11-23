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

package com.davils.kapi.spec.openapi.read.server

import com.davils.kapi.spec.openapi.DiagnosticCodes
import com.davils.kapi.spec.openapi.OpenApi
import com.davils.kapi.spec.openapi.fixture.codes
import com.davils.kapi.spec.openapi.fixture.document
import com.davils.kapi.spec.openapi.fixture.readable
import com.davils.kapi.spec.openapi.fixture.reading31
import com.davils.kapi.spec.openapi.fixture.reading32
import com.davils.kapi.spec.openapi.fixture.rewritten
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

private const val SERVERS: String = "servers:"

private const val NAMED: String = "  - {url: 'https://api.example.com', name: production, description: Live}"

class ServerReadingTest : FunSpec({
    context("a server of OpenAPI 3.2") {
        test("carries a name") {
            val reading = reading32(SERVERS, NAMED)

            reading.diagnostics.shouldBeEmpty()
            reading
                .document()
                .servers
                .single()
                .name shouldBe "production"
        }

        test("is written with its name and reads back") {
            val document = reading32(SERVERS, NAMED).document()

            OpenApi.write(document) shouldContain "name: production"
            document.rewritten() shouldBe document
        }
    }

    context("a server before OpenAPI 3.2") {
        test("has no name") {
            val reading = reading31(SERVERS, NAMED)

            reading.codes() shouldBe listOf(DiagnosticCodes.FIELD_VERSION)
            val server = reading.readable().servers.single()
            server.name.shouldBeNull()
        }
    }
})
