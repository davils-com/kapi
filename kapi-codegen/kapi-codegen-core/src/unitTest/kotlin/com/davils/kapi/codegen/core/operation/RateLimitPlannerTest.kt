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
import com.davils.kapi.codegen.core.fixture.clientOf
import com.davils.kapi.codegen.core.fixture.codes
import com.davils.kapi.codegen.core.fixture.get
import com.davils.kapi.codegen.core.fixture.schema
import com.davils.kapi.codegen.core.fixture.serverGenerationOf
import com.davils.kapi.codegen.core.fixture.serverOf
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

private const val LIMIT: String = "OperationRateLimit"

private fun limited(value: String): List<String> = get(LIST_PETS, "x-kapi-rate-limit: $value")

class RateLimitPlannerTest : FunSpec({
    context("the rate limit of an operation") {
        test("is applied by the generated server, per caller unless the document says whose requests count") {
            val expected = "rateLimit = $LIMIT(requests = 5, per = Duration.parseIsoString(\"PT30S\"), " +
                "key = RateLimitKey.Caller),"

            serverOf(limited("{requests: 5, period: PT30S}")) shouldContain expected
            serverOf(limited("{requests: 5, period: PT30S, per: principal}")) shouldContain
                "key = RateLimitKey.Principal"
            serverOf(limited("{requests: 5, period: PT30S, per: address}")) shouldContain "key = RateLimitKey.Address"
        }

        test("writes its period as the canonical ISO 8601 duration, so the output is the same for equal periods") {
            serverOf(limited("{requests: 5, period: P1D}")) shouldContain "Duration.parseIsoString(\"PT24H\")"
            serverOf(limited("{requests: 5, period: PT90S}")) shouldContain "Duration.parseIsoString(\"PT1M30S\")"
        }

        test("is absent where the document declares none and means nothing to a client") {
            serverOf(get(LIST_PETS)) shouldNotContain LIMIT
            clientOf(limited("{requests: 5, period: PT30S}")) shouldNotContain LIMIT
        }

        test("is refused with a located diagnostic when it is malformed or out of bounds") {
            val generation = serverGenerationOf(limited("{requests: 5000000, period: PT30S}"))
            val diagnostic = generation.diagnostics.single()

            generation.codes() shouldBe listOf(CodegenCodes.EXTENSION_VALUE)
            diagnostic.message shouldContain "1,000,000"
            val location = diagnostic.location.shouldNotBeNull()

            location.pointer.toString() shouldContain "x-kapi-rate-limit"
        }

        test("is refused on a schema") {
            serverGenerationOf(schema("x-kapi-rate-limit: {requests: 5, period: PT30S}")).codes() shouldBe
                listOf(CodegenCodes.EXTENSION_SITE)
        }
    }
})
