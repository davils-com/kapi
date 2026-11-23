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

package com.davils.kapi.codegen.core.asyncapi

import com.davils.kapi.codegen.core.asyncapi.fixture.Compiler
import com.davils.kapi.codegen.core.asyncapi.fixture.filesOf
import com.davils.kapi.codegen.core.asyncapi.fixture.resource
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private suspend fun scenarioOf(name: String): List<Any?> {
    val scenario = resource(name.replace(oldValue = ".yaml", newValue = "-scenario.txt"))
    val compilation = Compiler.compile(filesOf(name), mapOf("scenario/Scenario.kt" to scenario))
    compilation.messages shouldBe ""
    return compilation.loaded("scenario.Scenario") { type ->
        val instance = type.getField("INSTANCE").get(null)
        val result = type.getMethod("run").invoke(instance) as List<*>
        result.toList()
    }
}

class ScenarioTest : FunSpec({
    test("a generated chat client talks to a generated chat server over a real WebSocket") {
        scenarioOf("chat.yaml") shouldBe listOf(
            "joined ann, said ann hi:lobby:chat.v1:7:test / chat.v1 closed 4001 bye",
            "large closed 1009",
            "failed HandshakeRefused(401)",
            "admission 400",
            "admission 400",
            "admission null",
        )
    }

    test("a generated client and server pair replies with the requests both send and receive the rest") {
        scenarioOf("quotes.yaml") shouldBe listOf(
            "reply price r1 4200, timed out, tick ACME, tick SILENT, confirm c-r3, price r3 1 closed 1000",
            "failed HandshakeRefused(401)",
        )
    }
})
