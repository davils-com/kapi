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

import com.davils.kapi.codegen.core.fixture.LIST_PETS
import com.davils.kapi.codegen.core.fixture.OK
import com.davils.kapi.codegen.core.fixture.clientOf
import com.davils.kapi.codegen.core.fixture.get
import com.davils.kapi.codegen.core.fixture.paths
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

private const val IDEMPOTENT: String = "            idempotent()\n"

private val timeouts: Map<String, String> = mapOf(
    "x-kapi-timeout: PT5S" to "            timeout(request = 5_000.milliseconds)\n",
    "x-kapi-timeout: {request: PT5S, connect: PT1S, socket: PT0.5S}" to
        "timeout(request = 5_000.milliseconds, connect = 1_000.milliseconds, socket = 500.milliseconds)\n",
    "x-kapi-timeout: {connect: PT1S}" to "            timeout(connect = 1_000.milliseconds)\n",
    "x-kapi-timeout: {socket: PT1M}" to "            timeout(socket = 60_000.milliseconds)\n",
)

private val repeatable: List<String> = listOf("get", "head", "put", "delete", "options")

private val single: List<String> = listOf("post", "patch")

private fun operation(method: String, vararg fields: String): List<String> {
    val all = fields.toList() + OK
    return paths("/pets: {$method: {${all.joinToString()}}}")
}

class DeliveryPlannerTest : FunSpec({
    context("the timeout of an operation") {
        timeouts.forEach { (keyword, code) ->
            test("is handed to the call in the parts the document sets: $keyword") {
                clientOf(get(LIST_PETS, keyword)) shouldContain code
            }
        }

        test("is that of its path when the operation names none") {
            val lines = paths("/pets: {x-kapi-timeout: PT2S, get: {$LIST_PETS, $OK}}")

            clientOf(lines) shouldContain "timeout(request = 2_000.milliseconds)"
        }

        test("is its own before that of its path") {
            val lines = paths("/pets: {x-kapi-timeout: PT2S, get: {$LIST_PETS, x-kapi-timeout: PT3S, $OK}}")
            val kotlin = clientOf(lines)

            kotlin shouldContain "timeout(request = 3_000.milliseconds)"
            kotlin shouldNotContain "2_000"
        }

        test("is left to the client when the document names none") {
            clientOf(get(LIST_PETS)) shouldNotContain "timeout("
        }
    }

    context("an operation that may be sent again") {
        repeatable.forEach { method ->
            test("is one whose method is idempotent: $method") {
                clientOf(operation(method)) shouldContain IDEMPOTENT
            }
        }

        single.forEach { method ->
            test("is none whose method changes something with every call: $method") {
                clientOf(operation(method)) shouldNotContain IDEMPOTENT
            }
        }

        test("is whatever x-kapi-idempotent says, before its method") {
            clientOf(operation("post", "x-kapi-idempotent: true")) shouldContain IDEMPOTENT
            clientOf(operation("get", "x-kapi-idempotent: false")) shouldNotContain IDEMPOTENT
        }
    }
})
